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

class DMPage : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var chatAdapter: ChatAdapter
    private var allChats = mutableListOf<Chat>()
    private var displayedChats = mutableListOf<Chat>()
    private var allRegisteredUsers = mutableListOf<User>()
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dmpage)

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

        val dmRecyclerView = findViewById<RecyclerView>(R.id.dmRecyclerView)
        dmRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(displayedChats) { recipientId ->
            apiService.startChat(userId!!, recipientId, "Bearer $token").enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val intent = Intent(this@DMPage, ChatPage::class.java)
                        intent.putExtra("recipientUid", recipientId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@DMPage, "Failed to start chat", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@DMPage, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
        }
        dmRecyclerView.adapter = chatAdapter

        loadUserData()

        val searchButton = findViewById<Button>(R.id.SearchLogo)
        val searchEditText = findViewById<EditText>(R.id.Search)
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                displayedChats.clear()
                displayedChats.addAll(allChats)
                chatAdapter.notifyDataSetChanged()
            }
        }

        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Already on DMPage
        }

        val request = findViewById<Button>(R.id.Request)
        request.setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
            finish()
        }

        val back = findViewById<Button>(R.id.Back)
        back.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserData() {
        apiService.getUser(userId!!, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val user = response.body()!!.user
                    findViewById<TextView>(R.id.Username).text = user.username
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
                    loadChats()
                    loadAllRegisteredUsers()
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
            loadChatsFromSQLite()
        } else {
            Toast.makeText(this, "User data not available offline", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadChats() {
        apiService.getChats(userId!!, "Bearer $token").enqueue(object : Callback<ChatsResponse> {
            override fun onResponse(call: Call<ChatsResponse>, response: Response<ChatsResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    allChats.clear()
                    allChats.addAll(response.body()!!.chats)
                    allChats.forEach { chat ->
                        databaseHelper.insertOrUpdateUser(
                            LocalUser(
                                userId = chat.otherUser.userId.toLong(),
                                name = chat.otherUser.name,
                                username = chat.otherUser.username,
                                phoneNumber = chat.otherUser.phoneNumber,
                                email = chat.otherUser.email,
                                bio = chat.otherUser.bio,
                                profileImage = chat.otherUser.profileImage,
                                postsCount = chat.otherUser.postsCount,
                                followersCount = chat.otherUser.followersCount,
                                followingCount = chat.otherUser.followingCount
                            )
                        )
                        databaseHelper.insertOrUpdateChat(
                            LocalChat(
                                chatId = chat.chatId,
                                userId = userId!!.toLong(),
                                otherUserId = chat.otherUser.userId.toLong(),
                                lastMessage = chat.lastMessage,
                                timestamp = chat.timestamp
                            )
                        )
                    }
                    displayedChats.clear()
                    displayedChats.addAll(allChats.sortedByDescending { it.timestamp })
                    chatAdapter.notifyDataSetChanged()
                } else {
                    loadChatsFromSQLite()
                }
            }

            override fun onFailure(call: Call<ChatsResponse>, t: Throwable) {
                loadChatsFromSQLite()
            }
        })
    }

    private fun loadChatsFromSQLite() {
        val localChats = databaseHelper.getChatsByUserId(userId!!.toLong())
        allChats.clear()
        localChats.forEach { localChat ->
            val otherUser = databaseHelper.getUserById(localChat.otherUserId)
            if (otherUser != null) {
                allChats.add(
                    Chat(
                        chatId = localChat.chatId,
                        otherUser = User(
                            userId = otherUser.userId.toString(),
                            name = otherUser.name,
                            username = otherUser.username,
                            phoneNumber = otherUser.phoneNumber,
                            email = otherUser.email,
                            bio = otherUser.bio,
                            profileImage = otherUser.profileImage,
                            postsCount = otherUser.postsCount,
                            followersCount = otherUser.followersCount,
                            followingCount = otherUser.followingCount
                        ),
                        lastMessage = localChat.lastMessage,
                        timestamp = localChat.timestamp
                    )
                )
            }
        }
        displayedChats.clear()
        displayedChats.addAll(allChats.sortedByDescending { it.timestamp })
        chatAdapter.notifyDataSetChanged()
    }

    private fun loadAllRegisteredUsers() {
        apiService.searchUsers("", userId!!, "Bearer $token").enqueue(object : Callback<SearchUsersResponse> {
            override fun onResponse(call: Call<SearchUsersResponse>, response: Response<SearchUsersResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    allRegisteredUsers.clear()
                    allRegisteredUsers.addAll(response.body()!!.users.map { searchUser ->
                        User(
                            userId = searchUser.userId,
                            name = searchUser.name,
                            username = searchUser.username,
                            phoneNumber = "", // Not provided in search
                            email = "", // Not provided in search
                            bio = null,
                            profileImage = searchUser.profileImage,
                            postsCount = 0, // Not provided in search
                            followersCount = 0, // Not provided in search
                            followingCount = 0 // Not provided in search
                        )
                    })
                    allRegisteredUsers.forEach { user ->
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
                }
            }

            override fun onFailure(call: Call<SearchUsersResponse>, t: Throwable) {
                // Fallback to SQLite if needed
            }
        })
    }

    private fun performSearch(query: String) {
        apiService.searchUsers(query, userId!!, "Bearer $token").enqueue(object : Callback<SearchUsersResponse> {
            override fun onResponse(call: Call<SearchUsersResponse>, response: Response<SearchUsersResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val filteredUsers = response.body()!!.users.map { searchUser ->
                        User(
                            userId = searchUser.userId,
                            name = searchUser.name,
                            username = searchUser.username,
                            phoneNumber = "",
                            email = "",
                            bio = null,
                            profileImage = searchUser.profileImage,
                            postsCount = 0,
                            followersCount = 0,
                            followingCount = 0
                        )
                    }
                    displayedChats.clear()
                    filteredUsers.forEach { user ->
                        val existingChat = allChats.find { it.otherUser.userId == user.userId }
                        if (existingChat != null) {
                            displayedChats.add(existingChat)
                        } else {
                            displayedChats.add(
                                Chat(
                                    chatId = "", // Placeholder, will be set on startChat
                                    otherUser = user,
                                    lastMessage = null,
                                    timestamp = 0
                                )
                            )
                        }
                    }
                    displayedChats.sortByDescending { it.timestamp }
                    chatAdapter.notifyDataSetChanged()
                } else {
                    performSearchOffline(query)
                }
            }

            override fun onFailure(call: Call<SearchUsersResponse>, t: Throwable) {
                performSearchOffline(query)
            }
        })
    }

    private fun performSearchOffline(query: String) {
        val localUsers = databaseHelper.getUsersByUsername(query)
        displayedChats.clear()
        localUsers.forEach { localUser ->
            val existingChat = allChats.find { it.otherUser.userId == localUser.userId.toString() }
            if (existingChat != null) {
                displayedChats.add(existingChat)
            } else {
                displayedChats.add(
                    Chat(
                        chatId = "",
                        otherUser = User(
                            userId = localUser.userId.toString(),
                            name = localUser.name,
                            username = localUser.username,
                            phoneNumber = localUser.phoneNumber,
                            email = localUser.email,
                            bio = localUser.bio,
                            profileImage = localUser.profileImage,
                            postsCount = localUser.postsCount,
                            followersCount = localUser.followersCount,
                            followingCount = localUser.followingCount
                        ),
                        lastMessage = null,
                        timestamp = 0
                    )
                )
            }
        }
        displayedChats.sortByDescending { it.timestamp }
        chatAdapter.notifyDataSetChanged()
    }
}