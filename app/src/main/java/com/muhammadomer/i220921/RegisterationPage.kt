package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterationPage : AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var username: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registeration_page)

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")

        // Initialize UI elements
        name = findViewById(R.id.Name)
        username = findViewById(R.id.Username)
        phoneNumber = findViewById(R.id.PhoneNumber)
        email = findViewById(R.id.Email)
        password = findViewById(R.id.Password)
        registerButton = findViewById(R.id.myBtn)

        registerButton.setOnClickListener {
            saveUserData()
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

        // Create a unique key for each user
        val userId = database.push().key ?: return

        val registerUser = userCredential(
            name = userName,
            username = userUsername,
            phoneNumber = userPhoneNumber,
            email = userEmail,
            password = userPassword
        )

        // Save to Firebase
        database.child(userId).setValue(registerUser)
            .addOnSuccessListener {
                Toast.makeText(this, "User Registered Successfully", Toast.LENGTH_SHORT).show()
                clearFields()
                val intent = Intent(this, LogInPage::class.java)
                startActivity(intent)
                finish() // Finish current activity to prevent going back to it using back button
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to register user", Toast.LENGTH_SHORT).show()
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
