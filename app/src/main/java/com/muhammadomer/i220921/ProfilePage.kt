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
import com.google.gson.Gson

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
    private lateinit var databaseHelper: DatabaseHelper
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_page)

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
            navigateToLogin()
            return
        }

        Log.d("ProfilePage", "UserId: $userId, Token: $token")

        profileImage = findViewById(R.id.ProfilePic)
        nameTextView = findViewById(R.id.Name)
        bioTextView = findViewById(R.id.Bio)
        postNumTextView = findViewById(R.id.PostNum)
        followerButton = findViewById(R.id.Follower)
        followingButton = findViewById(R.id.Following)
        editProfileButton = findViewById(R.id.EditProfile)
        logoutButton = findViewById(R.id.LogoutButton)
        postRecyclerView = findViewById(R.id.postRecyclerView)

        postAdapter = ProfilePostAdapter()
        postRecyclerView.layoutManager = GridLayoutManager(this, 3)
        postRecyclerView.adapter = postAdapter

        loadUserData()
        loadUserPosts()

        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfilePage::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            apiService.logout(userId!!, "Bearer $token").enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    databaseHelper.clearUserData()
                    val editor = sharedPref.edit()
                    editor.clear()
                    editor.apply()
                    Toast.makeText(this@ProfilePage, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    databaseHelper.clearUserData()
                    val editor = sharedPref.edit()
                    editor.clear()
                    editor.apply()
                    Toast.makeText(this@ProfilePage, "Logged out successfully (offline)", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            })
        }

        findViewById<Button>(R.id.Home).setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.Search).setOnClickListener {
            val intent = Intent(this, SearchPage::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.NewPost).setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.myBtn).setOnClickListener {
            Toast.makeText(this, "Already on Profile", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.Contact).setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        apiService.getUser(userId!!, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        nameTextView.text = it.name ?: "Unknown"
                        bioTextView.text = it.bio.takeIf { b -> b?.isNotEmpty() == true } ?: ""
                        postNumTextView.text = (it.postsCount ?: 0).toString()
                        followerButton.text = (it.followersCount ?: 0).toString()
                        followingButton.text = (it.followingCount ?: 0).toString()

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

                        val localUser = LocalUser(
                            userId = userId!!.toLong(),
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
                    }
                } else {
                    Log.e("ProfilePage", "Failed to load user data: ${response.message()}")
                    Toast.makeText(this@ProfilePage, "Failed to load user data from API, trying local storage", Toast.LENGTH_LONG).show()
                    loadUserDataFromSQLite()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("ProfilePage", "Network error: ${t.message}")
                Toast.makeText(this@ProfilePage, "Network error, trying local storage", Toast.LENGTH_SHORT).show()
                loadUserDataFromSQLite()
            }
        })
    }

    private fun loadUserDataFromSQLite() {
        val localUser = databaseHelper.getUserById(userId!!.toLong())
        if (localUser != null) {
            nameTextView.text = localUser.name
            bioTextView.text = localUser.bio.takeIf { b -> b?.isNotEmpty() == true } ?: ""
            postNumTextView.text = localUser.postsCount.toString()
            followerButton.text = localUser.followersCount.toString()
            followingButton.text = localUser.followingCount.toString()

            if (!localUser.profileImage.isNullOrEmpty()) {
                Thread {
                    try {
                        val url = URL(localUser.profileImage)
                        val bitmap = BitmapFactory.decodeStream(url.openStream())
                        runOnUiThread {
                            profileImage.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ProfilePage, "Failed to load profile image from local data", Toast.LENGTH_SHORT).show()
                            profileImage.setImageResource(R.drawable.dummyprofilepic)
                        }
                    }
                }.start()
            } else {
                profileImage.setImageResource(R.drawable.dummyprofilepic)
            }
        } else {
            Toast.makeText(this@ProfilePage, "No local user data available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserPosts() {
        apiService.getPosts(userId!!, "Bearer $token").enqueue(object : Callback<PostsResponse> {
            override fun onResponse(call: Call<PostsResponse>, response: Response<PostsResponse>) {
                if (response.isSuccessful) {
                    val postsResponse = response.body()
                    if (postsResponse?.status == "success") {
                        val posts = postsResponse.posts
                        posts.forEach { post ->
                            val localPost = LocalPost(
                                postId = post.postId ?: "",
                                userId = userId!!.toLong(),
                                imageUrls = Gson().toJson(post.imageUrls),
                                caption = post.caption,
                                timestamp = post.timestamp ?: 0,
                                likes = Gson().toJson(post.likes)
                            )
                            databaseHelper.insertOrUpdatePost(localPost)
                        }
                        postAdapter.submitPosts(posts)
                    } else {
                        Toast.makeText(this@ProfilePage, postsResponse?.message ?: "Failed to load posts", Toast.LENGTH_SHORT).show()
                        loadPostsFromSQLite()
                    }
                } else {
                    Log.e("ProfilePage", "Failed to load posts: ${response.message()}")
                    Toast.makeText(this@ProfilePage, "Failed to load posts, trying local storage", Toast.LENGTH_LONG).show()
                    loadPostsFromSQLite()
                }
            }

            override fun onFailure(call: Call<PostsResponse>, t: Throwable) {
                Log.e("ProfilePage", "Network error: ${t.message}")
                Toast.makeText(this@ProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                loadPostsFromSQLite()
            }
        })
    }

    private fun loadPostsFromSQLite() {
        val localUser = databaseHelper.getUserById(userId!!.toLong())
        if (localUser != null) {
            val localPosts = databaseHelper.getPostsByUserIds(listOf(userId!!.toLong()))
            val posts = localPosts.map { localPost ->
                Post(
                    postId = localPost.postId,
                    imageUrls = Gson().fromJson(localPost.imageUrls, Array<String>::class.java).toList(),
                    caption = localPost.caption,
                    timestamp = localPost.timestamp,
                    likes = Gson().fromJson(localPost.likes, Array<String>::class.java).toList(),
                    comments = emptyList()
                )
            }
            postAdapter.submitPosts(posts)
            if (posts.isEmpty()) {
                Toast.makeText(this, "No posts available offline", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No user data available offline", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LogInPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}