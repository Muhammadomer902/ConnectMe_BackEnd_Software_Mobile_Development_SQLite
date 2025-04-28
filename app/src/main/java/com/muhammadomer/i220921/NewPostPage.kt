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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NewPostPage : AppCompatActivity() {
    private lateinit var cameraPreview: PreviewView
    private lateinit var clickPicture: Button
    private lateinit var reverseCamera: ImageView
    private lateinit var fullScreenImage: ImageView
    private var isFrontCamera = false
    private var selectedBitmap: Bitmap? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val imagePaths = mutableListOf<String>()

    private lateinit var databaseHelper: DatabaseHelper

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

            val filePath = saveBitmapToTempFile(rotatedBitmap)
            if (filePath != null) {
                imagePaths.add(filePath)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_post_page)

        databaseHelper = DatabaseHelper(this)

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

        val post = findViewById<Button>(R.id.NewStory)
        post.setOnClickListener {
            val intent = Intent(this, NewStoryPage::class.java)
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
            if (imagePaths.isNotEmpty()) {
                val intent = Intent(this, FinalizePostPage::class.java)
                intent.putStringArrayListExtra("imagePaths", ArrayList(imagePaths))
                startActivity(intent)
                cleanup()
                finish()
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

                    val filePath = saveBitmapToTempFile(finalBitmap)
                    if (filePath != null) {
                        imagePaths.add(filePath)
                    }
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

    private fun saveBitmapToTempFile(bitmap: Bitmap): String? {
        return try {
            val tempFile = File.createTempFile("image_", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanup() {
        imagePaths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        imagePaths.clear()
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