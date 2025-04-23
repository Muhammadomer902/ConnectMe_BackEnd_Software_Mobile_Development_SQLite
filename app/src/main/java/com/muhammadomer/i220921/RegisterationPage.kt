package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import android.content.Context
import com.google.gson.annotations.SerializedName

class RegisterationPage : AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var username: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registeration_page)

        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.2.11/CONNECTME-API/api/") // Updated for physical device
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        name = findViewById(R.id.Name)
        username = findViewById(R.id.Username)
        phoneNumber = findViewById(R.id.PhoneNumber)
        email = findViewById(R.id.Email)
        password = findViewById(R.id.Password)
        registerButton = findViewById(R.id.myBtn)

        registerButton.setOnClickListener {
            saveUserData()
        }

        val logIn = findViewById<Button>(R.id.LogIn)
        logIn.setOnClickListener {
            val intent = Intent(this, LogInPage::class.java)
            startActivity(intent)
        }
    }

    private fun saveUserData() {
        val userName = name.text.toString().trim()
        val userUsername = username.text.toString().trim()
        val userPhoneNumber = phoneNumber.text.toString().trim()
        val userEmail = email.text.toString().trim()
        val userPassword = password.text.toString().trim()

        if (userName.isEmpty() || userUsername.isEmpty() || userPhoneNumber.isEmpty() || userEmail.isEmpty() || userPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        checkIfUserExists(userUsername, userEmail) { exists, field ->
            if (exists) {
                when (field) {
                    "username" -> Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                    "email" -> Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show()
                }
            } else {
                registerUser(userName, userUsername, userPhoneNumber, userEmail, userPassword)
            }
        }
    }

    private fun checkIfUserExists(username: String, email: String, callback: (Boolean, String?) -> Unit) {
        val request = CheckUserRequest(username, email)
        apiService.checkUser(request).enqueue(object : Callback<CheckUserResponse> {
            override fun onResponse(call: Call<CheckUserResponse>, response: Response<CheckUserResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "error") {
                        callback(true, if (result.message?.contains("username", ignoreCase = true) == true) "username" else "email")
                    } else {
                        callback(false, null)
                    }
                } else {
                    Toast.makeText(this@RegisterationPage, "Error checking user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CheckUserResponse>, t: Throwable) {
                Toast.makeText(this@RegisterationPage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerUser(name: String, username: String, phoneNumber: String, email: String, password: String) {
        val request = RegisterRequest(name, username, phoneNumber, email, password)
        apiService.register(request).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "success") {
                        // Store userId and token in SharedPreferences
                        val sharedPref = getSharedPreferences("ConnectMePrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("userId", result.userId.toString())
                            putString("token", result.token)
                            apply()
                        }

                        Toast.makeText(this@RegisterationPage, "User Registered Successfully", Toast.LENGTH_SHORT).show()
                        clearFields()
                        val intent = Intent(this@RegisterationPage, EditProfilePage::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegisterationPage, result?.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RegisterationPage, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(this@RegisterationPage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clearFields() {
        name.text.clear()
        username.text.clear()
        phoneNumber.text.clear()
        email.text.clear()
        password.text.clear()
    }
}

// Retrofit API interface
interface ApiService {
    @POST("check-user.php")
    fun checkUser(@Body request: CheckUserRequest): Call<CheckUserResponse>

    @POST("register.php")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>
}

// Data classes for API requests and responses
data class CheckUserRequest(
    val username: String,
    val email: String
)

data class CheckUserResponse(
    val status: String,
    val message: String?
)

data class RegisterRequest(
    val name: String,
    val username: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val status: String,
    val message: String?,
    val userId: Long?,
    val token: String?
)