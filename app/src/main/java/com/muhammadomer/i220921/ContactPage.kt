package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import java.util.UUID

class ContactPage : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var pendingRequestsAdapter: PendingRequestsAdapter
    private var pendingRequests = mutableListOf<User>()
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contact_page)

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

        val pendingRequestsRecyclerView = findViewById<RecyclerView>(R.id.pendingRequestsRecyclerView)
        pendingRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        pendingRequestsAdapter = PendingRequestsAdapter(
            pendingRequests,
            currentUserId = userId!!,
            onAccept = { followerId -> handleRequest(followerId, true) },
            onReject = { followerId -> handleRequest(followerId, false) }
        )
        pendingRequestsRecyclerView.adapter = pendingRequestsAdapter

        fetchPendingRequests()

        val back = findViewById<Button>(R.id.Back)
        back.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
        }

        val home = findViewById<Button>(R.id.Home)
        home.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
        }

        val search = findViewById<Button>(R.id.Search)
        search.setOnClickListener {
            val intent = Intent(this, SearchPage::class.java)
            startActivity(intent)
            finish()
        }

        val newPost = findViewById<ImageButton>(R.id.NewPost)
        newPost.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)
            finish()
        }

        val profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
            finish()
        }

        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Already on ContactPage, no action needed
        }
    }

    private fun fetchPendingRequests() {
        apiService.getPendingRequests(userId!!, "Bearer $token").enqueue(object : Callback<PendingRequestsResponse> {
            override fun onResponse(call: Call<PendingRequestsResponse>, response: Response<PendingRequestsResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    pendingRequests.clear()
                    pendingRequests.addAll(response.body()!!.pendingRequests)
                    pendingRequests.forEach { user ->
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
                    pendingRequestsAdapter.notifyDataSetChanged()
                } else {
                    loadPendingRequestsFromSQLite()
                }
            }

            override fun onFailure(call: Call<PendingRequestsResponse>, t: Throwable) {
                loadPendingRequestsFromSQLite()
            }
        })
    }

    private fun loadPendingRequestsFromSQLite() {
        // Note: Pending requests are transient and not cached in SQLite for simplicity
        // If offline support is critical, we could cache them similarly to other data
        Toast.makeText(this, "Offline pending requests not available", Toast.LENGTH_SHORT).show()
    }

    private fun handleRequest(followerId: String, accept: Boolean) {
        val call = if (accept) {
            apiService.acceptFollowRequest(userId!!, followerId, "Bearer $token")
        } else {
            apiService.rejectFollowRequest(userId!!, followerId, "Bearer $token")
        }
        call.enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(
                        this@ContactPage,
                        if (accept) "Request accepted" else "Request rejected",
                        Toast.LENGTH_SHORT
                    ).show()
                    fetchPendingRequests()
                } else {
                    queueAction(followerId, accept)
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                queueAction(followerId, accept)
            }
        })
    }

    private fun queueAction(followerId: String, accept: Boolean) {
        val action = QueuedAction(
            actionId = UUID.randomUUID().toString(),
            actionType = if (accept) "accept_follow_request" else "reject_follow_request",
            payload = Gson().toJson(mapOf("followerId" to followerId)),
            createdAt = System.currentTimeMillis()
        )
        databaseHelper.queueAction(action)
        Toast.makeText(
            this,
            if (accept) "Request acceptance queued" else "Request rejection queued",
            Toast.LENGTH_SHORT
        ).show()
    }
}