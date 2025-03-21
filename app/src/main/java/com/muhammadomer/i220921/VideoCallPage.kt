package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VideoCallPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video_call_page)

        var chat = findViewById<Button>(R.id.Chat)
        chat.setOnClickListener {
            val intent = Intent(this, ChatPage::class.java)
            startActivity(intent)
        }

        var voiceCall = findViewById<Button>(R.id.VoiceCall)
        voiceCall.setOnClickListener() {
            val intent = Intent(this, VoiceCallPage::class.java)
            startActivity(intent)
        }

    }
}