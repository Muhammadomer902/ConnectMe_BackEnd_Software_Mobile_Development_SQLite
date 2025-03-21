package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChatPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_page)

        var back = findViewById<Button>(R.id.Back)
        back.setOnClickListener {
            val intent = Intent(this, DMPage::class.java)
            startActivity(intent)

        }

        var voiceCall = findViewById<Button>(R.id.VoiceCall)
        voiceCall.setOnClickListener {
            val intent = Intent(this, VoiceCallPage::class.java)
            startActivity(intent)

        }

        var videoCall = findViewById<Button>(R.id.VideoCall)
        videoCall.setOnClickListener() {
            val intent = Intent(this, VideoCallPage::class.java)
            startActivity(intent)
        }

        var profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener() {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

//        var vanishingChat = findViewById<Button>(R.id.VanishingChat)
//        vanishingChat.setOnClickListener() {
//            val intent = Intent(this, VanishingChatPage::class.java)
//            startActivity(intent)
//        }

    }
}