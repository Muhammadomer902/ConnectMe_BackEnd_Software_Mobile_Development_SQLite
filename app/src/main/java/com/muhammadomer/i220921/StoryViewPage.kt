package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class StoryViewPage : AppCompatActivity() {
    private lateinit var storyImageView: ImageView
    private lateinit var database: DatabaseReference
    private val handler = Handler(Looper.getMainLooper())
    private var currentStoryIndex = 0
    private lateinit var storyIds: List<String>
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_view_page)

        storyImageView = findViewById(R.id.storyImageView)
        database = FirebaseDatabase.getInstance().getReference("Stories")

        // Get the story IDs and user ID from the intent
        storyIds = intent.getStringArrayListExtra("storyIds") ?: emptyList()
        userId = intent.getStringExtra("userId") ?: ""

        if (storyIds.isNotEmpty()) {
            displayStory(storyIds[currentStoryIndex])
        } else {
            // If no stories, redirect to HomePage
            redirectToHomePage()
        }
    }

    private fun displayStory(storyId: String) {
        database.child(storyId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val story = snapshot.getValue(StoryInfo::class.java)
                story?.let {
                    // Decode the Base64 string to a Bitmap
                    val imageBytes = Base64.decode(it.bitmapString, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    storyImageView.setImageBitmap(bitmap)

                    // Schedule the next story or redirect after 3 seconds
                    handler.postDelayed({
                        currentStoryIndex++
                        if (currentStoryIndex < storyIds.size) {
                            // Display the next story
                            displayStory(storyIds[currentStoryIndex])
                        } else {
                            // No more stories in this user's story list, redirect to HomePage
                            redirectToHomePage()
                        }
                    }, 4000) // 4 seconds as per requirement
                }
            }

            override fun onCancelled(error: DatabaseError) {
                redirectToHomePage()
            }
        })
    }

    private fun redirectToHomePage() {
        val intent = Intent(this, HomePage::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}