package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LogInPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")

        usernameInput = findViewById(R.id.Username)
        passwordInput = findViewById(R.id.Password)
        loginBtn = findViewById(R.id.myBtn)
        registerBtn = findViewById(R.id.Registeration)

        // Check if user is already logged in
        if (auth.currentUser != null) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val userOnlineRef = database.child(userId).child("isOnline")
                userOnlineRef.setValue(true) // Set user as online
                userOnlineRef.onDisconnect().setValue(false) // Set to offline when disconnected
            }

            // If user is already logged in, go to HomePage
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
        }

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

    private fun authenticateUser(username: String, password: String) {
        database.orderByChild("username").equalTo(username).get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    var loginSuccessful = false
                    for (userSnapshot in dataSnapshot.children) {
                        val storedPassword = userSnapshot.child("password").value.toString()
                        val email = userSnapshot.child("email").value.toString()
                        if (storedPassword == password) {
                            loginSuccessful = true

                            // Sign in with Firebase Auth
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // Set user as online and set up onDisconnect
                                        val userId = auth.currentUser?.uid
                                        if (userId != null) {
                                            val userOnlineRef = database.child(userId).child("isOnline")
                                            userOnlineRef.setValue(true) // Set user as online
                                            userOnlineRef.onDisconnect().setValue(false) // Set to offline when disconnected
                                        }

                                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, HomePage::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            break
                        }
                    }
                    if (!loginSuccessful) {
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}