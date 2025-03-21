package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VanishingChatPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vanishing_chat_page)

        var back = findViewById<Button>(R.id.Back)
        back.setOnClickListener {
            val intent = Intent(this, DMPage::class.java)
            startActivity(intent)

        }

        var profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener() {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

    }
}