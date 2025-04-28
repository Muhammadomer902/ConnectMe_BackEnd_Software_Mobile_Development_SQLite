package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
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
import java.util.UUID

data class RecentSearch(
    val id: String,
    val searchedUserId: String,
    val username: String,
    val timestamp: Long
)

class SearchPage : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var recentSearchesAdapter: RecentSearchesAdapter
    private lateinit var searchedUsersAdapter: SearchedUsersAdapter
    private var currentFilter = "All"
    private var allUsers = mutableListOf<SearchUser>()
    private var recentSearches = mutableListOf<RecentSearch>()
    private var searchedUsers = mutableListOf<SearchUser>()
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search_page)

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

        val recentSearchesRecyclerView = findViewById<RecyclerView>(R.id.recentSearchesRecyclerView)
        recentSearchesRecyclerView.layoutManager = LinearLayoutManager(this)
        recentSearchesAdapter = RecentSearchesAdapter(
            recentSearches,
            onRemoveClick = { search ->
                removeRecentSearch(search.id)
            },
            onClick = { search ->
                findViewById<EditText>(R.id.Search).setText(search.username)
                performSearch(search.username)
            }
        )
        recentSearchesRecyclerView.adapter = recentSearchesAdapter

        val searchedUsersRecyclerView = findViewById<RecyclerView>(R.id.searchedUsersRecyclerView)
        searchedUsersRecyclerView.layoutManager = LinearLayoutManager(this)
        searchedUsersAdapter = SearchedUsersAdapter(
            searchedUsers,
            currentUserId = userId!!,
            onFollowClick = { user ->
                sendFollowRequest(user)
            }
        )
        searchedUsersRecyclerView.adapter = searchedUsersAdapter

        loadRecentSearches()

        val searchButton = findViewById<Button>(R.id.SearchLogo)
        val searchEditText = findViewById<EditText>(R.id.Search)
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            }
        }

        val filterAll = findViewById<Button>(R.id.FilterAll)
        val filterFollowers = findViewById<Button>(R.id.FilterFollowers)
        val filterFollowing = findViewById<Button>(R.id.FilterFollowing)

        filterAll.setOnClickListener {
            currentFilter = "All"
            updateFilterButtons(filterAll, filterFollowers, filterFollowing)
            applyFilter()
        }

        filterFollowers.setOnClickListener {
            currentFilter = "Followers"
            updateFilterButtons(filterAll, filterFollowers, filterFollowing)
            applyFilter()
        }

        filterFollowing.setOnClickListener {
            currentFilter = "Following"
            updateFilterButtons(filterAll, filterFollowers, filterFollowing)
            applyFilter()
        }

        findViewById<Button>(R.id.myBtn).setOnClickListener {
            // Already on SearchPage
        }

        findViewById<Button>(R.id.Home).setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.NewPost).setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.Profile).setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.Contact).setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
        }
    }

    private fun loadRecentSearches() {
        val localSearches = databaseHelper.getRecentSearches(userId!!.toLong())
        recentSearches.clear()
        recentSearches.addAll(localSearches.map {
            RecentSearch(
                id = it.id,
                searchedUserId = it.searchedUserId.toString(),
                username = it.username,
                timestamp = it.timestamp
            )
        }.take(3))
        recentSearchesAdapter.notifyDataSetChanged()
        if (recentSearches.isEmpty()) {
            Toast.makeText(this, "No recent searches available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch(query: String) {
        apiService.searchUsers(query, userId!!, "Bearer $token").enqueue(object : Callback<SearchUsersResponse> {
            override fun onResponse(call: Call<SearchUsersResponse>, response: Response<SearchUsersResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "success") {
                        allUsers.clear()
                        allUsers.addAll(result.users.sortedBy { it.username.lowercase() })
                        searchedUsers.clear()
                        searchedUsers.addAll(allUsers)
                        applyFilter()
                        result.users.forEach { user ->
                            databaseHelper.insertOrUpdateUser(
                                LocalUser(
                                    userId = user.userId.toLong(),
                                    name = user.name,
                                    username = user.username,
                                    phoneNumber = "",
                                    email = "",
                                    bio = null,
                                    profileImage = user.profileImage,
                                    postsCount = 0,
                                    followersCount = if (user.isFollowing) 1 else 0,
                                    followingCount = 0
                                )
                            )
                        }
                        if (result.users.isNotEmpty()) {
                            addRecentSearch(result.users.first())
                        }
                    } else {
                        Toast.makeText(this@SearchPage, result?.message ?: "No users found", Toast.LENGTH_SHORT).show()
                        loadUsersFromSQLite(query)
                    }
                } else {
                    Toast.makeText(this@SearchPage, "Failed to search users: ${response.message()}", Toast.LENGTH_SHORT).show()
                    loadUsersFromSQLite(query)
                }
            }

            override fun onFailure(call: Call<SearchUsersResponse>, t: Throwable) {
                Toast.makeText(this@SearchPage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                loadUsersFromSQLite(query)
            }
        })
    }

    private fun loadUsersFromSQLite(query: String) {
        val localUsers = databaseHelper.getUsersByUsername(query)
        allUsers.clear()
        allUsers.addAll(localUsers.map {
            SearchUser(
                userId = it.userId.toString(),
                username = it.username,
                name = it.name,
                profileImage = it.profileImage,
                isFollowing = it.followersCount > 0
            )
        }.sortedBy { it.username.lowercase() })
        searchedUsers.clear()
        searchedUsers.addAll(allUsers)
        applyFilter()
        if (allUsers.isEmpty()) {
            Toast.makeText(this, "No users found offline", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyFilter() {
        val filteredList = when (currentFilter) {
            "All" -> searchedUsers.toList()
            "Followers" -> searchedUsers.filter { it.isFollowing }
            "Following" -> searchedUsers.filter { it.isFollowing }
            else -> searchedUsers.toList()
        }
        searchedUsers.clear()
        searchedUsers.addAll(filteredList)
        searchedUsersAdapter.notifyDataSetChanged()
    }

    private fun updateFilterButtons(filterAll: Button, filterFollowers: Button, filterFollowing: Button) {
        filterAll.setBackgroundResource(R.drawable.rectangle_button_unselected)
        filterAll.setTextColor(getColor(R.color.brown))
        filterFollowers.setBackgroundResource(R.drawable.rectangle_button_unselected)
        filterFollowers.setTextColor(getColor(R.color.brown))
        filterFollowing.setBackgroundResource(R.drawable.rectangle_button_unselected)
        filterFollowing.setTextColor(getColor(R.color.brown))

        when (currentFilter) {
            "All" -> {
                filterAll.setBackgroundResource(R.drawable.rectangle_button_green)
                filterAll.setTextColor(getColor(R.color.white))
            }
            "Followers" -> {
                filterFollowers.setBackgroundResource(R.drawable.rectangle_button_green)
                filterFollowers.setTextColor(getColor(R.color.white))
            }
            "Following" -> {
                filterFollowing.setBackgroundResource(R.drawable.rectangle_button_green)
                filterFollowing.setTextColor(getColor(R.color.white))
            }
        }
    }

    private fun addRecentSearch(user: SearchUser) {
        val existingSearch = recentSearches.find { it.searchedUserId == user.userId }
        if (existingSearch != null) {
            recentSearches.remove(existingSearch)
        }
        val newSearch = RecentSearch(
            id = UUID.randomUUID().toString(),
            searchedUserId = user.userId,
            username = user.username,
            timestamp = System.currentTimeMillis()
        )
        recentSearches.add(0, newSearch)
        if (recentSearches.size > 3) {
            recentSearches.removeAt(recentSearches.size - 1)
        }
        recentSearchesAdapter.notifyDataSetChanged()
        databaseHelper.insertOrUpdateRecentSearch(
            LocalRecentSearch(
                id = newSearch.id,
                userId = userId!!.toLong(),
                searchedUserId = user.userId.toLong(),
                username = user.username,
                timestamp = newSearch.timestamp
            )
        )
    }

    private fun removeRecentSearch(searchId: String) {
        recentSearches.removeIf { it.id == searchId }
        recentSearchesAdapter.notifyDataSetChanged()
        databaseHelper.deleteRecentSearch(searchId)
    }

    private fun sendFollowRequest(user: SearchUser) {
        if (user.userId == userId) {
            Toast.makeText(this, "You can't follow yourself", Toast.LENGTH_SHORT).show()
            return
        }
        apiService.followUser(userId!!, user.userId, "Bearer $token").enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@SearchPage, "Follow request sent to ${user.username}", Toast.LENGTH_SHORT).show()
                    searchedUsersAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@SearchPage, response.body()?.message ?: "Failed to send follow request", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@SearchPage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
