package com.muhammadomer.i220921

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NewChatImagePage : AppCompatActivity() {
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
    private var recipientId: String? = null
    private var chatId: String = ""

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            clickPicture.visibility = View.VISIBLE
            cameraPreview.visibility = View.GONE
        }
    }

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
        setContentView(R.layout.activity_new_chat_image_page)

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

        recipientId = intent.getStringExtra("recipientId")
        if (recipientId == null) {
            Toast.makeText(this, "Error: No recipient selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatId = if (userId!! < recipientId!!) "$userId-$recipientId" else "$recipientId-$userId"

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

        val cancel = findViewById<Button>(R.id.Cancel)
        cancel.setOnClickListener {
            val intent = Intent(this, DMPage::class.java)
            startActivity(intent)
            finish()
        }

        val gallery = findViewById<Button>(R.id.Gallery)
        gallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        val finalizePost = findViewById<Button>(R.id.FinalizePost)
        finalizePost.setOnClickListener {
            if (selectedBitmap != null) {
                uploadImageAndSendMessage()
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

    private fun uploadImageAndSendMessage() {
        val bitmap = selectedBitmap ?: return
        val file = File.createTempFile("image_", ".jpg", cacheDir)
        val outputStream = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.close()

        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", file.name, requestBody)

        apiService.uploadMessageImage(userId!!, "Bearer $token", part).enqueue(object : Callback<ImageUploadResponse> {
            override fun onResponse(call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val imageUrl = response.body()!!.imageUrl
                    sendMessage(imageUrl)
                    file.delete()
                } else {
                    queueImageMessage(file.path)
                    file.delete()
                }
            }

            override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                queueImageMessage(file.path)
                file.delete()
            }
        })
    }

    private fun sendMessage(imageUrl: String) {
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            messageId = messageId,
            chatId = chatId,
            text = "",
            imageUrl = imageUrl,
            senderId = userId!!,
            timestamp = System.currentTimeMillis(),
            isSeen = false,
            vanish = false
        )
        val request = SendMessageRequest(
            text = "",
            imageUrl = imageUrl,
            senderId = userId!!,
            timestamp = System.currentTimeMillis(),
            isSeen = false,
            vanish = false
        )
        apiService.sendMessage(chatId, "Bearer $token", request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    databaseHelper.insertOrUpdateMessage(
                        LocalMessage(
                            messageId = message.messageId,
                            chatId = message.chatId,
                            text = message.text,
                            imageUrl = message.imageUrl,
                            senderId = message.senderId.toLong(),
                            timestamp = message.timestamp,
                            isSeen = message.isSeen,
                            vanish = message.vanish
                        )
                    )
                    Toast.makeText(this@NewChatImagePage, "Image sent successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@NewChatImagePage, ChatPage::class.java)
                    intent.putExtra("recipientId", recipientId)
                    startActivity(intent)
                    finish()
                } else {
                    queueMessage(message)
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                queueMessage(message)
            }
        })
    }

    private fun queueImageMessage(filePath: String) {
        val action = QueuedAction(
            actionId = UUID.randomUUID().toString(),
            actionType = "send_image_message",
            payload = Gson().toJson(mapOf("filePath" to filePath, "chatId" to chatId, "recipientId" to recipientId)),
            createdAt = System.currentTimeMillis()
        )
        databaseHelper.queueAction(action)
        Toast.makeText(this, "Image message queued for sending", Toast.LENGTH_SHORT).show()
    }

    private fun queueMessage(message: Message) {
        val action = QueuedAction(
            actionId = UUID.randomUUID().toString(),
            actionType = "send_message",
            payload = Gson().toJson(message),
            createdAt = System.currentTimeMillis()
        )
        databaseHelper.queueAction(action)
        Toast.makeText(this, "Message queued for sending", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        selectedBitmap?.recycle()
        selectedBitmap = null
    }
}