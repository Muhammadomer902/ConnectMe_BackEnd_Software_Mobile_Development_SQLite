package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import android.animation.ValueAnimator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import java.net.URL

class StoryViewPage : AppCompatActivity() {
    private lateinit var storyImageView: ImageView
    private lateinit var storyProgressBar: ProgressBar
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private val handler = Handler(Looper.getMainLooper())
    private var currentStoryIndex = 0
    private lateinit var stories: List<Story>
    private var userId: String? = null
    private var token: String? = null
    private var progressAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_view_page)

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
            redirectToLogin()
            return
        }

        storyImageView = findViewById(R.id.storyImageView)
        storyProgressBar = findViewById(R.id.storyProgressBar)

        loadStories()
    }

    private fun loadStories() {
        apiService.getStories(userId!!, "Bearer $token").enqueue(object : Callback<StoriesResponse> {
            override fun onResponse(call: Call<StoriesResponse>, response: Response<StoriesResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    stories = response.body()!!.stories
                    stories.forEach { story ->
                        databaseHelper.insertOrUpdateStory(
                            LocalStory(
                                storyId = story.storyId,
                                userId = story.userId.toLong(),
                                imageUrl = story.imageUrl,
                                timestamp = story.timestamp
                            )
                        )
                    }
                    if (stories.isNotEmpty()) {
                        displayStory(stories[currentStoryIndex])
                    } else {
                        loadStoriesFromSQLite()
                    }
                } else {
                    loadStoriesFromSQLite()
                }
            }

            override fun onFailure(call: Call<StoriesResponse>, t: Throwable) {
                loadStoriesFromSQLite()
            }
        })
    }

    private fun loadStoriesFromSQLite() {
        val localStories = databaseHelper.getStoriesByUserIds(listOf(userId!!.toLong()))
        stories = localStories.map {
            Story(
                storyId = it.storyId,
                userId = it.userId.toString(),
                imageUrl = it.imageUrl,
                timestamp = it.timestamp
            )
        }
        if (stories.isNotEmpty()) {
            displayStory(stories[currentStoryIndex])
        } else {
            redirectToHomePage()
        }
    }

    private fun displayStory(story: Story) {
        storyProgressBar.progress = 0
        progressAnimator?.cancel()

        Thread {
            try {
                val url = URL(story.imageUrl)
                val bitmap = BitmapFactory.decodeStream(url.openStream())
                runOnUiThread {
                    storyImageView.setImageBitmap(bitmap)
                    startProgressAnimation()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    storyImageView.setImageResource(R.drawable.dummyprofilepic)
                    startProgressAnimation()
                }
            }
        }.start()
    }

    private fun startProgressAnimation() {
        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 4000
            addUpdateListener { animation ->
                storyProgressBar.progress = animation.animatedValue as Int
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    handler.postDelayed({
                        currentStoryIndex++
                        if (currentStoryIndex < stories.size) {
                            displayStory(stories[currentStoryIndex])
                        } else {
                            redirectToHomePage()
                        }
                    }, 1000)
                }
            })
            start()
        }
    }

    private fun redirectToHomePage() {
        val intent = Intent(this, HomePage::class.java)
        startActivity(intent)
        finish()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LogInPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        progressAnimator?.cancel()
    }
}