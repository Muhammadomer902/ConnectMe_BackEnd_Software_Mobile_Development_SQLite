package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RegisterationPage : AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var username: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registeration_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")

        name = findViewById(R.id.Name)
        username = findViewById(R.id.Username)
        phoneNumber = findViewById(R.id.PhoneNumber)
        email = findViewById(R.id.Email)
        password = findViewById(R.id.Password)
        registerButton = findViewById(R.id.myBtn)

        registerButton.setOnClickListener {
            saveUserData()
        }

        var logIn = findViewById<Button>(R.id.LogIn)
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
        database.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        callback(true, "username")
                    } else {
                        database.orderByChild("email").equalTo(email)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        callback(true, "email")
                                    } else {
                                        callback(false, null)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(this@RegisterationPage, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RegisterationPage, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun registerUser(name: String, username: String, phoneNumber: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid // Use Firebase Auth UID instead of push().key
                    if (userId != null) {
                        val registerUser = userCredential(
                            name = name,
                            username = username,
                            phoneNumber = phoneNumber,
                            email = email,
                            password = password,
                            bio = "",
                            profileImage = "",
                            posts = emptyList(),
                            followers = emptyList(),
                            following = emptyList(),
                            stories = emptyList(),           // Initialize stories
                            pendingFollowRequests = emptyList(), // Initialize pendingFollowRequests
                            recentSearches = emptyList(),     // Initialize recentSearches
                            isOnline = true                   // Set user as online upon registration
                        )

                        // Set up onDisconnect to mark user as offline
                        val userOnlineRef = database.child(userId).child("isOnline")
                        userOnlineRef.setValue(true) // Set user as online
                        userOnlineRef.onDisconnect().setValue(false) // Set to offline when disconnected

                        database.child(userId).setValue(registerUser)
                            .addOnSuccessListener {
                                Toast.makeText(this, "User Registered Successfully", Toast.LENGTH_SHORT).show()
                                clearFields()
                                val intent = Intent(this, EditProfilePage::class.java) // No need for USER_ID extra
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to register user", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun clearFields() {
        name.text.clear()
        username.text.clear()
        phoneNumber.text.clear()
        email.text.clear()
        password.text.clear()
    }
}