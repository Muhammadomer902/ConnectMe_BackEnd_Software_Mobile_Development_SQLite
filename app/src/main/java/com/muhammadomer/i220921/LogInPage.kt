package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class LogInPage : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in_page)

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

        // Check if user is already logged in
        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
        val userId = sharedPref.getString("userId", null)
        val token = sharedPref.getString("token", null)

        if (userId != null && token != null) {
            // Try to load from SQLite first
            val localUser = databaseHelper.getUserById(userId.toLong())
            if (localUser != null) {
                Toast.makeText(this, "Welcome back (offline mode)!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomePage::class.java)
                startActivity(intent)
                finish()
            } else {
                // Fetch user data from API if no local data
                fetchUserDataAndNavigate(userId, token)
            }
        }

        // Initialize UI elements
        usernameInput = findViewById(R.id.Username)
        passwordInput = findViewById(R.id.Password)
        loginBtn = findViewById(R.id.myBtn)
        registerBtn = findViewById(R.id.Registeration)

        // Handle login button click
        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                authenticateUser(username, password)
            }
        }

        // Handle registration button click
        registerBtn.setOnClickListener {
            val intent = Intent(this, RegisterationPage::class.java)
            startActivity(intent)
        }
    }

    private fun fetchUserDataAndNavigate(userId: String, token: String) {
        apiService.getUser(userId, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        // Update SQLite with API data
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

                        // Navigate to HomePage
                        Toast.makeText(this@LogInPage, "Welcome back!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LogInPage, HomePage::class.java)
                        startActivity(intent)
                        finish()
                    } ?: run {
                        // Handle null response body
                        Toast.makeText(this@LogInPage, "Failed to fetch user data, trying local storage", Toast.LENGTH_LONG).show()
                        val localUser = databaseHelper.getUserById(userId.toLong())
                        if (localUser != null) {
                            Toast.makeText(this@LogInPage, "Welcome back (offline mode)!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LogInPage, HomePage::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LogInPage, "No local data available, please log in again", Toast.LENGTH_LONG).show()
                            // Clear SharedPreferences to force re-login
                            val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
                            sharedPref.edit().clear().apply()
                        }
                    }
                } else {
                    // API failed, try to load from SQLite
                    Toast.makeText(this@LogInPage, "Failed to fetch user data from API, trying local storage", Toast.LENGTH_LONG).show()
                    val localUser = databaseHelper.getUserById(userId.toLong())
                    if (localUser != null) {
                        Toast.makeText(this@LogInPage, "Welcome back (offline mode)!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LogInPage, HomePage::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LogInPage, "No local data available, please log in again", Toast.LENGTH_LONG).show()
                        // Clear SharedPreferences to force re-login
                        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
                        sharedPref.edit().clear().apply()
                    }
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                // Network error, try to load from SQLite
                Toast.makeText(this@LogInPage, "Network error, trying local storage: ${t.message}", Toast.LENGTH_SHORT).show()
                val localUser = databaseHelper.getUserById(userId.toLong())
                if (localUser != null) {
                    Toast.makeText(this@LogInPage, "Welcome back (offline mode)!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LogInPage, HomePage::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LogInPage, "No local data available, please log in again", Toast.LENGTH_LONG).show()
                    // Clear SharedPreferences to force re-login
                    val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                }
            }
        })
    }

    private fun authenticateUser(username: String, password: String) {
        // Step 1: Fetch email by username
        apiService.getEmailByUsername(username).enqueue(object : Callback<UserEmailResponse> {
            override fun onResponse(call: Call<UserEmailResponse>, response: Response<UserEmailResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "success" && result.email != null) {
                        // Step 2: Authenticate using email and password
                        loginWithEmail(result.email, password, username)
                    } else {
                        // Handle server error response
                        val errorMessage = result?.message ?: "User does not exist"
                        Toast.makeText(this@LogInPage, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Log the raw response for debugging
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("LogInPage", "getEmailByUsername error: $errorBody")
                    Toast.makeText(this@LogInPage, "Error fetching user email: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UserEmailResponse>, t: Throwable) {
                Log.e("LogInPage", "getEmailByUsername network error: ${t.message}")
                Toast.makeText(this@LogInPage, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loginWithEmail(email: String, password: String, username: String) {
        apiService.login(email, password).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "success" && result.userId != null && result.token != null) {
                        // Save user ID and token in SharedPreferences
                        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("userId", result.userId.toString())
                            putString("token", result.token)
                            apply()
                        }

                        // Fetch user data to store in SQLite
                        apiService.getUser(result.userId.toString(), "Bearer ${result.token}").enqueue(object : Callback<UserResponse> {
                            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                                if (response.isSuccessful) {
                                    val user = response.body()
                                    user?.let {
                                        val localUser = LocalUser(
                                            userId = result.userId!!.toLong(),
                                            name = it.name ?: "",
                                            username = it.username ?: username,
                                            phoneNumber = it.phoneNumber ?: "",
                                            email = it.email ?: email,
                                            bio = it.bio,
                                            profileImage = it.profileImage,
                                            postsCount = it.postsCount ?: 0,
                                            followersCount = it.followersCount ?: 0,
                                            followingCount = it.followingCount ?: 0
                                        )
                                        databaseHelper.insertOrUpdateUser(localUser)
                                    } ?: run {
                                        // Cache minimal user data if API response is null
                                        val localUser = LocalUser(
                                            userId = result.userId!!.toLong(),
                                            name = "",
                                            username = username,
                                            phoneNumber = "",
                                            email = email,
                                            bio = null,
                                            profileImage = null,
                                            postsCount = 0,
                                            followersCount = 0,
                                            followingCount = 0
                                        )
                                        databaseHelper.insertOrUpdateUser(localUser)
                                        Toast.makeText(this@LogInPage, "Failed to fetch user data, using minimal data", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    // Cache minimal user data if API call fails
                                    val localUser = LocalUser(
                                        userId = result.userId!!.toLong(),
                                        name = "",
                                        username = username,
                                        phoneNumber = "",
                                        email = email,
                                        bio = null,
                                        profileImage = null,
                                        postsCount = 0,
                                        followersCount = 0,
                                        followingCount = 0
                                    )
                                    databaseHelper.insertOrUpdateUser(localUser)
                                    Toast.makeText(this@LogInPage, "Failed to fetch user data: ${response.message()}", Toast.LENGTH_LONG).show()
                                }
                                // Navigate to HomePage regardless of user data fetch success
                                Toast.makeText(this@LogInPage, "Login successful", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LogInPage, HomePage::class.java)
                                startActivity(intent)
                                finish()
                            }

                            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                                // Cache minimal user data if API call fails
                                val localUser = LocalUser(
                                    userId = result.userId!!.toLong(),
                                    name = "",
                                    username = username,
                                    phoneNumber = "",
                                    email = email,
                                    bio = null,
                                    profileImage = null,
                                    postsCount = 0,
                                    followersCount = 0,
                                    followingCount = 0
                                )
                                databaseHelper.insertOrUpdateUser(localUser)
                                Toast.makeText(this@LogInPage, "Network error fetching user data: ${t.message}", Toast.LENGTH_LONG).show()
                                // Navigate to HomePage even if user data fetch fails
                                Toast.makeText(this@LogInPage, "Login successful, user data not fully cached", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LogInPage, HomePage::class.java)
                                startActivity(intent)
                                finish()
                            }
                        })
                    } else {
                        // Handle server error response
                        val errorMessage = result?.message ?: "Login failed"
                        Toast.makeText(this@LogInPage, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Log the raw response for debugging
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("LogInPage", "login error: $errorBody")
                    Toast.makeText(this@LogInPage, "Error: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("LogInPage", "login network error: ${t.message}")
                Toast.makeText(this@LogInPage, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}