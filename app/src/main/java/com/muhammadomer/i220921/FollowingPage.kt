package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient

class FollowingPage : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var followingAdapter: FollowingAdapter
    private var allFollowing = mutableListOf<User>()
    private var displayedFollowing = mutableListOf<User>()
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_following_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
            val intent = Intent(this, LogInPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        val followingRecyclerView = findViewById<RecyclerView>(R.id.followingRecyclerView)
        followingRecyclerView.layoutManager = LinearLayoutManager(this)
        followingAdapter = FollowingAdapter(displayedFollowing)
        followingRecyclerView.adapter = followingAdapter

        loadUserData()

        val searchButton = findViewById<Button>(R.id.SearchLogo)
        val searchEditText = findViewById<EditText>(R.id.Search)
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                displayedFollowing.clear()
                displayedFollowing.addAll(allFollowing)
                followingAdapter.notifyDataSetChanged()
            }
        }

        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Already on FollowingPage
        }

        val follower = findViewById<Button>(R.id.Follower)
        follower.setOnClickListener {
            val intent = Intent(this, FollowerPage::class.java)
            startActivity(intent)
            finish()
        }

        val profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        apiService.getUser(userId!!, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val user = response.body()!!.user
                    findViewById<TextView>(R.id.Username).text = user.username
                    findViewById<Button>(R.id.Follower).text = "${user.followersCount} Followers"
                    findViewById<Button>(R.id.myBtn).text = "${user.followingCount} Following"
                    databaseHelper.insertOrUpdateUser(
                        LocalUser(
                            userId = user.userId.toLong(),
                            name = user.name,
                            username = user.username,
                            phoneNumber = user.phoneNumber,
                            email = user.email,
                            bio = user.bio,
                            profileImage = user.profileImage,
                            postsCount = user.postsCount,
                            followersCount = user.followersCount,
                            followingCount = user.followingCount
                        )
                    )
                    loadFollowing()
                } else {
                    loadUserDataFromSQLite()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                loadUserDataFromSQLite()
            }
        })
    }

    private fun loadUserDataFromSQLite() {
        val localUser = databaseHelper.getUserById(userId!!.toLong())
        if (localUser != null) {
            findViewById<TextView>(R.id.Username).text = localUser.username
            findViewById<Button>(R.id.Follower).text = "${localUser.followersCount} Followers"
            findViewById<Button>(R.id.myBtn).text = "${localUser.followingCount} Following"
            loadFollowingFromSQLite()
        } else {
            Toast.makeText(this, "User data not available offline", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFollowing() {
        apiService.getFollowing(userId!!, "Bearer $token").enqueue(object : Callback<FollowingResponse> {
            override fun onResponse(call: Call<FollowingResponse>, response: Response<FollowingResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    allFollowing.clear()
                    allFollowing.addAll(response.body()!!.following)
                    allFollowing.forEach { user ->
                        databaseHelper.insertOrUpdateUser(
                            LocalUser(
                                userId = user.userId.toLong(),
                                name = user.name,
                                username = user.username,
                                phoneNumber = user.phoneNumber,
                                email = user.email,
                                bio = user.bio,
                                profileImage = user.profileImage,
                                postsCount = user.postsCount,
                                followersCount = user.followersCount,
                                followingCount = user.followingCount
                            )
                        )
                    }
                    displayedFollowing.clear()
                    displayedFollowing.addAll(allFollowing.sortedBy { it.username.lowercase() })
                    followingAdapter.notifyDataSetChanged()
                } else {
                    loadFollowingFromSQLite()
                }
            }

            override fun onFailure(call: Call<FollowingResponse>, t: Throwable) {
                loadFollowingFromSQLite()
            }
        })
    }

    private fun loadFollowingFromSQLite() {
        val localUsers = databaseHelper.getUsersByUsername("") // Get all users as a fallback
        allFollowing.clear()
        allFollowing.addAll(localUsers.map { user ->
            User(
                userId = user.userId.toString(),
                name = user.name,
                username = user.username,
                phoneNumber = user.phoneNumber,
                email = user.email,
                bio = user.bio,
                profileImage = user.profileImage,
                postsCount = user.postsCount,
                followersCount = user.followersCount,
                followingCount = user.followingCount
            )
        })
        displayedFollowing.clear()
        displayedFollowing.addAll(allFollowing.sortedBy { it.username.lowercase() })
        followingAdapter.notifyDataSetChanged()
    }

    private fun performSearch(query: String) {
        val filteredFollowing = allFollowing.filter { user ->
            user.username.lowercase().contains(query.lowercase())
        }.sortedBy { it.username.lowercase() }
        displayedFollowing.clear()
        displayedFollowing.addAll(filteredFollowing)
        followingAdapter.notifyDataSetChanged()
    }
}