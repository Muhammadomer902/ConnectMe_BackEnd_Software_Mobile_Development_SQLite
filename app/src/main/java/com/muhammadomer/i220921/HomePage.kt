package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import java.net.URL

class HomePage : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var postAdapter: HomePostAdapter
    private lateinit var storyContainer: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        // Initialize Retrofit
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

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        // Get userId and token from SharedPreferences
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

        // Set up RecyclerView for posts
        val recyclerView = findViewById<RecyclerView>(R.id.postRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = HomePostAdapter()
        recyclerView.adapter = postAdapter

        // Find the story container
        storyContainer = findViewById<LinearLayout>(R.id.story_container)

        // Load current user's profile picture for NewStory
        val newStoryView = findViewById<CircleImageView>(R.id.NewStory)
        loadUserProfilePicture(userId!!, newStoryView)

        // Fetch and display posts
        fetchAndDisplayPosts(userId!!)

        // Note: Stories feature is not implemented in the API, commenting out Firebase-based story logic
        /*
        // Fetch and display stories
        fetchAndDisplayStories(userId)
        */

        // Navigation button listeners
        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        val newStory = findViewById<CircleImageView>(R.id.NewStory)
        newStory.setOnClickListener {
            val intent = Intent(this, NewStoryPage::class.java)
            startActivity(intent)
        }

        val storyMore = findViewById<ImageButton>(R.id.StoryMore)
        storyMore.setOnClickListener {
            val intent = Intent(this, NewStoryPage::class.java)
            startActivity(intent)
        }

        val dm = findViewById<Button>(R.id.DM)
        dm.setOnClickListener {
            val intent = Intent(this, DMPage::class.java)
            startActivity(intent)
        }

        val search = findViewById<Button>(R.id.Search)
        search.setOnClickListener {
            val intent = Intent(this, SearchPage::class.java)
            startActivity(intent)
        }

        val newPost = findViewById<ImageButton>(R.id.NewPost)
        newPost.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)
        }

        val profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        val contact = findViewById<Button>(R.id.Contact)
        contact.setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserProfilePicture(userId: String, imageView: CircleImageView) {
        // Try API first
        apiService.getUser(userId, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        if (!it.profileImage.isNullOrEmpty()) {
                            Thread {
                                try {
                                    val url = URL(it.profileImage)
                                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                                    runOnUiThread {
                                        imageView.setImageBitmap(bitmap)
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        imageView.setImageResource(R.drawable.dummyprofilepic)
                                    }
                                }
                            }.start()
                        } else {
                            imageView.setImageResource(R.drawable.dummyprofilepic)
                        }
                    }
                } else {
                    Log.e("HomePage", "Failed to load user: ${response.message()}")
                    loadUserProfilePictureFromSQLite(userId.toLong(), imageView)
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("HomePage", "Network error: ${t.message}")
                Toast.makeText(this@HomePage, "Network error, trying local storage", Toast.LENGTH_SHORT).show()
                loadUserProfilePictureFromSQLite(userId.toLong(), imageView)
            }
        })
    }

    private fun loadUserProfilePictureFromSQLite(userId: Long, imageView: CircleImageView) {
        val localUser = databaseHelper.getUserById(userId)
        if (localUser != null && !localUser.profileImage.isNullOrEmpty()) {
            Thread {
                try {
                    val url = URL(localUser.profileImage)
                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                    runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        imageView.setImageResource(R.drawable.dummyprofilepic)
                    }
                }
            }.start()
        } else {
            imageView.setImageResource(R.drawable.dummyprofilepic)
        }
    }

    private fun fetchAndDisplayPosts(userId: String) {
        // Try API first
        apiService.getPosts(userId, "Bearer $token").enqueue(object : Callback<PostsResponse> {
            override fun onResponse(call: Call<PostsResponse>, response: Response<PostsResponse>) {
                if (response.isSuccessful) {
                    val postsResponse = response.body()
                    if (postsResponse?.status == "success") {
                        val posts = postsResponse.posts
                        // Get user data for the posts' owner
                        apiService.getUser(userId, "Bearer $token").enqueue(object : Callback<UserResponse> {
                            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                                if (response.isSuccessful) {
                                    val user = response.body()
                                    user?.let {
                                        val localUser = LocalUser(
                                            userId = userId.toLong(),
                                            name = it.name ?: "",
                                            username = it.username ?: "",
                                            phoneNumber = it.phoneNumber ?: "",
                                            email = it.email ?: "",
                                            bio = it.bio,
                                            profileImage = it.profileImage,
                                            postsCount = it.postsCount ?: 0,
                                            followersCount = it.followersCount ?: 0,
                                            followingCount = it.followingCount ?: 0
                                        )
                                        databaseHelper.insertOrUpdateUser(localUser)
                                        val postList = posts.map { post ->
                                            // Cache post in SQLite
                                            val localPost = LocalPost(
                                                postId = post.postId ?: "",
                                                userId = userId.toLong(),
                                                imageUrls = Gson().toJson(post.imageUrls),
                                                caption = post.caption,
                                                timestamp = post.timestamp ?: 0,
                                                likes = Gson().toJson(post.likes)
                                            )
                                            databaseHelper.insertOrUpdatePost(localPost)
                                            Pair(localUser, post)
                                        }
                                        postAdapter.submitPosts(postList)
                                    }
                                } else {
                                    Log.e("HomePage", "Failed to load user: ${response.message()}")
                                    loadPostsFromSQLite(userId.toLong())
                                }
                            }

                            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                                Log.e("HomePage", "Network error: ${t.message}")
                                Toast.makeText(this@HomePage, "Network error, trying local storage", Toast.LENGTH_SHORT).show()
                                loadPostsFromSQLite(userId.toLong())
                            }
                        })
                    } else {
                        Toast.makeText(this@HomePage, postsResponse?.message ?: "Failed to load posts", Toast.LENGTH_SHORT).show()
                        loadPostsFromSQLite(userId.toLong())
                    }
                } else {
                    Log.e("HomePage", "Failed to load posts: ${response.message()}")
                    Toast.makeText(this@HomePage, "Failed to load posts, trying local storage", Toast.LENGTH_SHORT).show()
                    loadPostsFromSQLite(userId.toLong())
                }
            }

            override fun onFailure(call: Call<PostsResponse>, t: Throwable) {
                Log.e("HomePage", "Network error: ${t.message}")
                Toast.makeText(this@HomePage, "Network error, trying local storage", Toast.LENGTH_SHORT).show()
                loadPostsFromSQLite(userId.toLong())
            }
        })
    }

    private fun loadPostsFromSQLite(userId: Long) {
        val localUser = databaseHelper.getUserById(userId)
        if (localUser != null) {
            val localPosts = databaseHelper.getPostsByUserIds(listOf(userId))
            val postList = localPosts.map { localPost ->
                val post = Post(
                    postId = localPost.postId,
                    imageUrls = Gson().fromJson(localPost.imageUrls, Array<String>::class.java).toList(),
                    caption = localPost.caption,
                    timestamp = localPost.timestamp,
                    likes = Gson().fromJson(localPost.likes, Array<String>::class.java).toList(),
                    comments = emptyList() // Comments not cached
                )
                Pair(localUser, post)
            }
            postAdapter.submitPosts(postList)
            if (postList.isEmpty()) {
                Toast.makeText(this, "No posts available offline", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No user data available offline", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}