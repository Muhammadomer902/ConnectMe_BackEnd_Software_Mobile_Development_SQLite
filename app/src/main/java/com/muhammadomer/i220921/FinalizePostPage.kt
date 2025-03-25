package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.File

class FinalizePostPage : AppCompatActivity() {
    private var imagePaths: ArrayList<String>? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_finalize_post_page)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")
        val userId = auth.currentUser?.uid ?: return // Exit if user is not logged in

        // Retrieve the image paths from the intent
        imagePaths = intent.getStringArrayListExtra("imagePaths")
        Log.d("FinalizePostPage", "Received image paths: $imagePaths")
        Log.d("FinalizePostPage", "Number of images received: ${imagePaths?.size ?: 0}")

        // Set up the RecyclerView to display the images
        val recyclerView = findViewById<RecyclerView>(R.id.imagesRecyclerView)
        if (!imagePaths.isNullOrEmpty()) {
            val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = FinalizePostAdapter(imagePaths!!)
            recyclerView.setHasFixedSize(true)
            recyclerView.isNestedScrollingEnabled = false
            Log.d("FinalizePostPage", "RecyclerView set up with ${imagePaths!!.size} items")
        } else {
            Log.w("FinalizePostPage", "No images to display")
            recyclerView.visibility = RecyclerView.GONE
        }

        // Get the caption EditText
        val captionEditText = findViewById<EditText>(R.id.captionEditText)

        // Set up the Share button (myBtn)
        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Get the caption (or empty string if none)
            val caption = captionEditText.text.toString().trim()

            // Convert images to base64 strings
            val imageBitmapStrings = mutableListOf<String>()
            imagePaths?.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        val bitmapString = bitmapToBase64(bitmap)
                        imageBitmapStrings.add(bitmapString)
                        bitmap.recycle() // Free memory
                    } else {
                        Log.w("FinalizePostPage", "Failed to decode bitmap from path: $path")
                    }
                } else {
                    Log.w("FinalizePostPage", "Image file does not exist: $path")
                }
            }

            // Create the post
            val postId = FirebaseDatabase.getInstance().reference.push().key ?: ""
            val timestamp = System.currentTimeMillis()
            val post = Post(
                postId = postId,
                imageUrls = imageBitmapStrings,
                caption = if (caption.isEmpty()) "" else caption,
                timestamp = timestamp,
                likes = mutableListOf(), // Empty list for likes
                comments = mutableListOf() // Empty map for comments
            )

            // Save the post to Firebase under "Posts"
            val postsRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId)
            postsRef.setValue(post)
                .addOnSuccessListener {
                    Log.d("FinalizePostPage", "Post saved successfully: $postId")

                    // Update the user's posts list in RegisteredUsers
                    database.child(userId).child("posts").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val posts = snapshot.getValue(object : GenericTypeIndicator<MutableList<String>>() {}) ?: mutableListOf()
                            posts.add(postId)
                            database.child(userId).child("posts").setValue(posts)
                                .addOnSuccessListener {
                                    Log.d("FinalizePostPage", "User's posts list updated with postId: $postId")

                                    // Navigate to HomePage
                                    val intent = Intent(this@FinalizePostPage, HomePage::class.java)
                                    startActivity(intent)

                                    // Clean up temporary files
                                    imagePaths?.forEach { path ->
                                        val file = File(path)
                                        if (file.exists()) {
                                            file.delete()
                                            Log.d("FinalizePostPage", "Deleted file: $path")
                                        } else {
                                            Log.w("FinalizePostPage", "File does not exist: $path")
                                        }
                                    }
                                    imagePaths?.clear()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this@FinalizePostPage, "Failed to update user's posts: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@FinalizePostPage, "Failed to fetch user's posts: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@FinalizePostPage, "Failed to save post: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Set up the Cancel button
        val cancel = findViewById<Button>(R.id.Cancel)
        cancel.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)

            // Clean up temporary files when canceling
            imagePaths?.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                    Log.d("FinalizePostPage", "Deleted file: $path")
                } else {
                    Log.w("FinalizePostPage", "File does not exist: $path")
                }
            }
            imagePaths?.clear()
            finish()
        }
    }

    // Utility to convert Bitmap to Base64 string
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}