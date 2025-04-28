package com.muhammadomer.i220921

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NewStoryPage : AppCompatActivity() {
    private lateinit var cameraPreview: PreviewView
    private lateinit var clickPicture: ImageView
    private lateinit var reverseCamera: ImageView
    private lateinit var fullScreenImage: ImageView
    private var isFrontCamera = false
    private var selectedBitmap: Bitmap? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private var userId: String? = null
    private var token: String? = null

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            clickPicture.visibility = View.VISIBLE
            cameraPreview.visibility = View.GONE
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            selectedBitmap?.recycle()

            val inputStream = contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            inputStream?.close()

            val rotatedBitmap = if (rotationDegrees != 0f) {
                rotateBitmap(bitmap, rotationDegrees)
            } else {
                bitmap
            }

            selectedBitmap = rotatedBitmap
            fullScreenImage.setImageBitmap(rotatedBitmap)
            fullScreenImage.visibility = View.VISIBLE
            clickPicture.visibility = View.VISIBLE
            cameraPreview.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_story_page)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.2.11/CONNECTME-API/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        databaseHelper = DatabaseHelper(this)

        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
        userId = sharedPref.getString("userId", null)
        token = sharedPref.getString("token", null)

        if (userId == null || token == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LogInPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        cameraPreview = findViewById(R.id.cameraPreview)
        clickPicture = findViewById(R.id.ClickPicture)
        reverseCamera = findViewById(R.id.ReverseCamera)
        fullScreenImage = findViewById(R.id.fullScreenImage)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        clickPicture.setOnClickListener {
            if (cameraPreview.visibility == View.VISIBLE) {
                takePhoto()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        reverseCamera.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera()
        }

        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
            cleanup()
            finish()
        }

        val cancel = findViewById<Button>(R.id.Cancel)
        cancel.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            cleanup()
            finish()
        }

        val post = findViewById<Button>(R.id.NewPost)
        post.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)
            cleanup()
            finish()
        }

        val gallery = findViewById<Button>(R.id.Gallery)
        gallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        val finalizePost = findViewById<Button>(R.id.FinalizePost)
        finalizePost.setOnClickListener {
            if (selectedBitmap != null) {
                uploadStory()
            } else {
                Toast.makeText(this, "Please capture or select an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                cameraPreview.visibility = View.VISIBLE
                clickPicture.visibility = View.VISIBLE
                fullScreenImage.visibility = View.GONE
            } catch (exc: Exception) {
                clickPicture.visibility = View.VISIBLE
                cameraPreview.visibility = View.GONE
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    selectedBitmap?.recycle()

                    val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
                    val rotatedBitmap = if (rotationDegrees != 0f) {
                        rotateBitmap(bitmap, rotationDegrees)
                    } else {
                        if (!isFrontCamera) {
                            rotateBitmap(bitmap, 90f)
                        } else {
                            bitmap
                        }
                    }

                    val finalBitmap = if (isFrontCamera) {
                        mirrorBitmap(rotatedBitmap)
                    } else {
                        rotatedBitmap
                    }

                    selectedBitmap = finalBitmap
                    fullScreenImage.setImageBitmap(finalBitmap)
                    fullScreenImage.visibility = View.VISIBLE
                    clickPicture.visibility = View.VISIBLE
                    cameraPreview.visibility = View.GONE
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    clickPicture.visibility = View.VISIBLE
                    cameraPreview.visibility = View.GONE
                }
            }
        )
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        if (format == android.graphics.ImageFormat.YUV_420_888) {
            val yuvImage = android.graphics.YuvImage(
                bytes,
                android.graphics.ImageFormat.NV21,
                width,
                height,
                null
            )
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
            val jpegBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } else {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun mirrorBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun uploadStory() {
        val tempFile = File.createTempFile("story_", ".jpg", cacheDir)
        val outputStream = FileOutputStream(tempFile)
        selectedBitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()

        val requestFile = tempFile.asRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)

        apiService.uploadPostImage(userId!!, "Bearer $token", part).enqueue(object : Callback<ImageUploadResponse> {
            override fun onResponse(call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val imageUrl = response.body()!!.imageUrl
                    val storyId = UUID.randomUUID().toString()
                    val timestamp = System.currentTimeMillis()
                    val request = CreateStoryRequest(imageUrl = imageUrl, timestamp = timestamp)

                    apiService.createStory(userId!!, "Bearer $token", request).enqueue(object : Callback<GenericResponse> {
                        override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                            if (response.isSuccessful && response.body()?.status == "success") {
                                val localStory = LocalStory(
                                    storyId = storyId,
                                    userId = userId!!.toLong(),
                                    imageUrl = imageUrl,
                                    timestamp = timestamp
                                )
                                databaseHelper.insertOrUpdateStory(localStory)
                                Toast.makeText(this@NewStoryPage, "Story posted successfully", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@NewStoryPage, HomePage::class.java)
                                startActivity(intent)
                                cleanup()
                                finish()
                            } else {
                                queueStory(storyId, imageUrl, timestamp)
                            }
                        }

                        override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                            queueStory(storyId, imageUrl, timestamp)
                        }
                    })
                } else {
                    queueStory(null, null, System.currentTimeMillis())
                }
                tempFile.delete()
            }

            override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                queueStory(null, null, System.currentTimeMillis())
                tempFile.delete()
            }
        })
    }

    private fun queueStory(storyId: String?, imageUrl: String?, timestamp: Long) {
        val action = QueuedAction(
            actionId = UUID.randomUUID().toString(),
            actionType = "create_story",
            payload = Gson().toJson(CreateStoryRequest(
                imageUrl = imageUrl ?: "",
                timestamp = timestamp
            )),
            createdAt = System.currentTimeMillis()
        )
        databaseHelper.queueAction(action)
        Toast.makeText(this, "Story queued for later sync", Toast.LENGTH_SHORT).show()

        if (storyId != null && imageUrl != null) {
            val localStory = LocalStory(
                storyId = storyId,
                userId = userId!!.toLong(),
                imageUrl = imageUrl,
                timestamp = timestamp
            )
            databaseHelper.insertOrUpdateStory(localStory)
        }
    }

    private fun cleanup() {
        selectedBitmap?.recycle()
        selectedBitmap = null
        fullScreenImage.setImageBitmap(null)
        fullScreenImage.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cleanup()
    }
}