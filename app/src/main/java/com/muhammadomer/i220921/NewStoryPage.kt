package com.muhammadomer.i220921

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Update the import for StoryInfo
import com.muhammadomer.i220921.StoryInfo

class NewStoryPage : AppCompatActivity() {
    private lateinit var cameraPreview: PreviewView
    private lateinit var clickPicture: ImageView
    private lateinit var reverseCamera: ImageView
    private lateinit var fullScreenImage: ImageView
    private var isFrontCamera = false
    private var selectedBitmap: Bitmap? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

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
            // Get the Bitmap from the URI
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            selectedBitmap?.recycle() // Recycle the previous Bitmap if it exists

            // Get the correct orientation from EXIF data and rotate the Bitmap
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

            // Rotate the Bitmap if needed
            val rotatedBitmap = if (rotationDegrees != 0f) {
                rotateBitmap(bitmap, rotationDegrees)
            } else {
                bitmap
            }

            selectedBitmap = rotatedBitmap
            // Set the image as the full-screen background
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

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")
        val userId = auth.currentUser?.uid ?: return

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
            val intent = Intent(this, NewStoryPage::class.java)
            startActivity(intent)

            selectedBitmap?.recycle()
            selectedBitmap = null
            fullScreenImage.setImageBitmap(null)
            fullScreenImage.visibility = View.GONE

            finish()
        }

        val cancel = findViewById<Button>(R.id.Cancel)
        cancel.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
        }

        val post = findViewById<Button>(R.id.NewPost)
        post.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
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
                // Convert the bitmap to a Base64 string
                val bitmapString = bitmapToBase64(selectedBitmap!!)

                // Create a new story (updated to use StoryInfo)
                val storyId = FirebaseDatabase.getInstance().getReference("Stories").push().key ?: return@setOnClickListener
                val story = StoryInfo(
                    storyId = storyId,
                    bitmapString = bitmapString,
                    timestamp = System.currentTimeMillis()
                )

                // Save the story to Firebase under Stories node
                FirebaseDatabase.getInstance().getReference("Stories").child(storyId).setValue(story)
                    .addOnSuccessListener {
                        // Update the user's stories list (overwrite the previous story if exists)
                        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val user = snapshot.getValue(userCredential::class.java)
                                user?.let {
                                    val currentStories = user.stories.toMutableList()
                                    // Remove the previous story if it exists (to enforce only 1 image per story)
                                    if (currentStories.isNotEmpty()) {
                                        // Optionally, delete the previous story from Stories node
                                        currentStories.forEach { oldStoryId ->
                                            FirebaseDatabase.getInstance().getReference("Stories").child(oldStoryId).removeValue()
                                        }
                                    }
                                    // Add the new story ID
                                    val updatedStories = mutableListOf<String>().apply {
                                        add(storyId)
                                    }
                                    database.child(userId).child("stories").setValue(updatedStories)
                                        .addOnSuccessListener {
                                            Toast.makeText(this@NewStoryPage, "Story posted successfully", Toast.LENGTH_SHORT).show()
                                            // Navigate to HomePage
                                            val intent = Intent(this@NewStoryPage, HomePage::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { error ->
                                            Toast.makeText(this@NewStoryPage, "Failed to update stories: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@NewStoryPage, "Failed to fetch user data: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this@NewStoryPage, "Failed to post story: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
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
                    selectedBitmap?.recycle() // Recycle the previous Bitmap if it exists

                    // Rotate the Bitmap based on the image's rotation degrees
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

                    // If using the front camera, mirror the image to correct the default mirroring
                    val finalBitmap = if (isFrontCamera) {
                        mirrorBitmap(rotatedBitmap)
                    } else {
                        rotatedBitmap
                    }

                    selectedBitmap = finalBitmap
                    // Set the image as the full-screen background
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

    // Utility to convert ImageProxy to Bitmap
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

    // Utility to rotate a Bitmap
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Utility to mirror a Bitmap (for front camera)
    private fun mirrorBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f) // Mirror horizontally
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Utility to convert Bitmap to Base64 string
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        selectedBitmap?.recycle()
        selectedBitmap = null
    }
}