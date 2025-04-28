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

class FollowerPage : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var followerAdapter: FollowerAdapter
    private var allFollowers = mutableListOf<User>()
    private var displayedFollowers = mutableListOf<User>()
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_follower_page)

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

        val followersRecyclerView = findViewById<RecyclerView>(R.id.followersRecyclerView)
        followersRecyclerView.layoutManager = LinearLayoutManager(this)
        followerAdapter = FollowerAdapter(displayedFollowers)
        followersRecyclerView.adapter = followerAdapter

        loadUserData()

        val searchButton = findViewById<Button>(R.id.SearchLogo)
        val searchEditText = findViewById<EditText>(R.id.Search)
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                displayedFollowers.clear()
                displayedFollowers.addAll(allFollowers)
                followerAdapter.notifyDataSetChanged()
            }
        }

        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Already on FollowerPage
        }

        val following = findViewById<Button>(R.id.Following)
        following.setOnClickListener {
            val intent = Intent(this, FollowingPage::class.java)
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
                    findViewById<Button>(R.id.myBtn).text = "${user.followersCount} Followers"
                    findViewById<Button>(R.id.Following).text = "${user.followingCount} Following"
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
                    loadFollowers()
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
            findViewById<Button>(R.id.myBtn).text = "${localUser.followersCount} Followers"
            findViewById<Button>(R.id.Following).text = "${localUser.followingCount} Following"
            loadFollowersFromSQLite()
        } else {
            Toast.makeText(this, "User data not available offline", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFollowers() {
        apiService.getFollowers(userId!!, "Bearer $token").enqueue(object : Callback<FollowersResponse> {
            override fun onResponse(call: Call<FollowersResponse>, response: Response<FollowersResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    allFollowers.clear()
                    allFollowers.addAll(response.body()!!.followers)
                    allFollowers.forEach { user ->
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
                    displayedFollowers.clear()
                    displayedFollowers.addAll(allFollowers.sortedBy { it.username.lowercase() })
                    followerAdapter.notifyDataSetChanged()
                } else {
                    loadFollowersFromSQLite()
                }
            }

            override fun onFailure(call: Call<FollowersResponse>, t: Throwable) {
                loadFollowersFromSQLite()
            }
        })
    }

    private fun loadFollowersFromSQLite() {
        val localUsers = databaseHelper.getUsersByUsername("") // Get all users as a fallback
        allFollowers.clear()
        allFollowers.addAll(localUsers.map { user ->
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
        displayedFollowers.clear()
        displayedFollowers.addAll(allFollowers.sortedBy { it.username.lowercase() })
        followerAdapter.notifyDataSetChanged()
    }

    private fun performSearch(query: String) {
        val filteredFollowers = allFollowers.filter { user ->
            user.username.lowercase().contains(query.lowercase())
        }.sortedBy { it.username.lowercase() }
        displayedFollowers.clear()
        displayedFollowers.addAll(filteredFollowers)
        followerAdapter.notifyDataSetChanged()
    }
}