package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GalleryPage : AppCompatActivity() {
    private val imagePaths = mutableListOf<String>()
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.forEach { uri ->
            val file = File.createTempFile("image_", ".jpg", cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            imagePaths.add(file.absolutePath)
        }
        if (imagePaths.isNotEmpty()) {
            val intent = Intent(this, FinalizePostPage::class.java)
            intent.putStringArrayListExtra("imagePaths", ArrayList(imagePaths))
            startActivity(intent)
            cleanup()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gallery_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
        val userId = sharedPref.getString("userId", null)
        val token = sharedPref.getString("token", null)

        if (userId == null || token == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LogInPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        val selectImages = findViewById<Button>(R.id.SelectImages)
        selectImages.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        val cancel = findViewById<Button>(R.id.Cancel)
        cancel.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            cleanup()
            finish()
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
                Toast.makeText(this, "Please select images first", Toast.LENGTH_SHORT).show()
            }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}