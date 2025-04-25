package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import java.net.URL

class ProfilePage : AppCompatActivity() {

    private lateinit var profileImage: CircleImageView
    private lateinit var nameTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var postNumTextView: TextView
    private lateinit var followerButton: Button
    private lateinit var followingButton: Button
    private lateinit var editProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var postRecyclerView: RecyclerView
    private lateinit var postAdapter: ProfilePostAdapter

    private lateinit var apiService: ApiService
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_page)

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

        // Get userId and token from SharedPreferences
        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
        userId = sharedPref.getString("userId", null)
        token = sharedPref.getString("token", null)

        if (userId == null || token == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        Log.d("ProfilePage", "UserId: $userId, Token: $token")

        // Initialize UI elements
        profileImage = findViewById(R.id.ProfilePic)
        nameTextView = findViewById(R.id.Name)
        bioTextView = findViewById(R.id.Bio)
        postNumTextView = findViewById(R.id.PostNum)
        followerButton = findViewById(R.id.Follower)
        followingButton = findViewById(R.id.Following)
        editProfileButton = findViewById(R.id.EditProfile)
        logoutButton = findViewById(R.id.LogoutButton)
        postRecyclerView = findViewById(R.id.postRecyclerView)

        // Set up RecyclerView
        postAdapter = ProfilePostAdapter()
        postRecyclerView.layoutManager = GridLayoutManager(this, 3) // 3 columns for a grid layout
        postRecyclerView.adapter = postAdapter

        // Load user data and posts
        loadUserData()
        loadUserPosts()

        // Set click listeners
        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfilePage::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            // Clear SharedPreferences and navigate to login
            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        // Bottom navigation bar click listeners (placeholders)
        findViewById<Button>(R.id.Home).setOnClickListener {
            // Navigate to HomePage
            Toast.makeText(this, "Navigate to Home", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.Search).setOnClickListener {
            // Navigate to SearchPage
            Toast.makeText(this, "Navigate to Search", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.NewPost).setOnClickListener {
            // Navigate to NewPostPage
            Toast.makeText(this, "Navigate to New Post", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.myBtn).setOnClickListener {
            // Already on ProfilePage, refresh or do nothing
            Toast.makeText(this, "Already on Profile", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.Contact).setOnClickListener {
            // Navigate to ContactPage
            Toast.makeText(this, "Navigate to Contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        apiService.getUser(userId!!, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        // Update UI with user data
                        nameTextView.text = it.name ?: "Unknown"
                        bioTextView.text = it.bio.takeIf { b -> b?.isNotEmpty() == true } ?: "" // Display empty string if bio is null or empty
                        postNumTextView.text = (it.postsCount ?: 0).toString()
                        followerButton.text = (it.followersCount ?: 0).toString()
                        followingButton.text = (it.followingCount ?: 0).toString()

                        // Load profile image using Bitmap
                        if (!it.profileImage.isNullOrEmpty()) {
                            Thread {
                                try {
                                    val url = URL(it.profileImage)
                                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                                    runOnUiThread {
                                        profileImage.setImageBitmap(bitmap)
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        Toast.makeText(this@ProfilePage, "Failed to load profile image", Toast.LENGTH_SHORT).show()
                                        profileImage.setImageResource(R.drawable.dummyprofilepic)
                                    }
                                }
                            }.start()
                        } else {
                            profileImage.setImageResource(R.drawable.dummyprofilepic)
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("ProfilePage", "Failed to load user data: ${response.code()} - ${response.message()} - $errorBody")
                    Toast.makeText(this@ProfilePage, "Failed to load user data: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("ProfilePage", "Network error: ${t.message}")
                Toast.makeText(this@ProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserPosts() {
        apiService.getPosts(userId!!, "Bearer $token").enqueue(object : Callback<PostsResponse> {
            override fun onResponse(call: Call<PostsResponse>, response: Response<PostsResponse>) {
                if (response.isSuccessful) {
                    val postsResponse = response.body()
                    if (postsResponse?.status == "success") {
                        val posts = postsResponse.posts
                        postAdapter.submitPosts(posts)
                    } else {
                        Toast.makeText(this@ProfilePage, postsResponse?.message ?: "Failed to load posts", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("ProfilePage", "Failed to load posts: ${response.code()} - ${response.message()} - $errorBody")
                    Toast.makeText(this@ProfilePage, "Failed to load posts: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<PostsResponse>, t: Throwable) {
                Log.e("ProfilePage", "Network error: ${t.message}")
                Toast.makeText(this@ProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LogInPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}