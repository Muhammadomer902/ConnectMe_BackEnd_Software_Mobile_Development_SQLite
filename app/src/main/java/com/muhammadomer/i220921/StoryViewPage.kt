package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import android.animation.ValueAnimator
import com.google.firebase.database.*

class StoryViewPage : AppCompatActivity() {
    private lateinit var storyImageView: ImageView
    private lateinit var storyProgressBar: ProgressBar
    private lateinit var database: DatabaseReference
    private val handler = Handler(Looper.getMainLooper())
    private var currentStoryIndex = 0
    private lateinit var storyIds: List<String>
    private lateinit var userId: String
    private var progressAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_view_page)

        storyImageView = findViewById(R.id.storyImageView)
        storyProgressBar = findViewById(R.id.storyProgressBar)
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
        // Reset the progress bar and cancel any existing animation
        storyProgressBar.progress = 0
        progressAnimator?.cancel()

        // Fetch and display the story image immediately
        database.child(storyId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val story = snapshot.getValue(StoryInfo::class.java)
                story?.let {
                    // Decode the Base64 string to a Bitmap
                    val imageBytes = Base64.decode(it.bitmapString, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    storyImageView.setImageBitmap(bitmap)

                    // Start the progress bar animation (0 to 100 over 4 seconds)
                    progressAnimator = ValueAnimator.ofInt(0, 100).apply {
                        duration = 4000 // 4 seconds for the progress bar
                        addUpdateListener { animation ->
                            storyProgressBar.progress = animation.animatedValue as Int
                        }
                        // When the progress bar animation completes, add a pause before moving to the next story
                        addListener(object : android.animation.Animator.AnimatorListener {
                            override fun onAnimationStart(animation: android.animation.Animator) {}
                            override fun onAnimationCancel(animation: android.animation.Animator) {}
                            override fun onAnimationRepeat(animation: android.animation.Animator) {}
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                // After the progress bar completes, pause for 1 second
                                handler.postDelayed({
                                    currentStoryIndex++
                                    if (currentStoryIndex < storyIds.size) {
                                        // Display the next story after the pause
                                        displayStory(storyIds[currentStoryIndex])
                                    } else {
                                        // No more stories, redirect to HomePage
                                        redirectToHomePage()
                                    }
                                }, 1000) // 1-second pause after progress bar completes
                            }
                        })
                        start()
                    }
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
        progressAnimator?.cancel() // Cancel the animation to prevent memory leaks
    }
}