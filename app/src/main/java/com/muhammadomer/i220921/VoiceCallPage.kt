package com.muhammadomer.i220921

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine

class VoiceCallPage : AppCompatActivity() {
    private lateinit var agoraEngine: RtcEngine
    private val APP_ID = "ce1b1f31b1e3445cb95ef67989f063fe"
    private lateinit var CHANNEL_NAME: String
    private val TEMP_TOKEN = "007eJxTYJiYeKFcoeGCLdvkgNRjR/1VGRIvunY7HTM9wFlyOm+dHq8CQ3KqYZJhmjGQSDU2MTFNTrI0TU0zM7e0sEwzMDNOS8198Cy9IZCRYU+KNSMjAwSC+JwMmUZGBpZGhmXJDAwAdpkfKA=="
    private val PERMISSION_REQ_CODE = 10
    private var callStartTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isMicMuted = false // Track mute state manually

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_voice_call_page)

        // Get recipient UID from Intent
        val recipientUid = intent.getStringExtra("recipientUid") ?: "default"
        CHANNEL_NAME = "call_$recipientUid"

        // Check and request permissions
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        // Initialize Agora Engine
        initializeAgoraEngine()

        // Join the channel
        joinChannel()

        // Start call timer
        startCallTimer()

        // Navigation and control buttons
        val endCall = findViewById<Button>(R.id.EndCall)
        endCall.setOnClickListener {
            leaveChannel()
            val intent = Intent(this, ChatPage::class.java)
            intent.putExtra("recipientUid", recipientUid)
            startActivity(intent)
            finish()
        }

        val videoCall = findViewById<Button>(R.id.VideoCall)
        videoCall.setOnClickListener {
            val intent = Intent(this, VideoCallPage::class.java)
            startActivity(intent)
        }

        val speaker = findViewById<Button>(R.id.Speaker)
        speaker.setOnClickListener {
            agoraEngine.setEnableSpeakerphone(!agoraEngine.isSpeakerphoneEnabled)
        }

        val mic = findViewById<Button>(R.id.Mic)
        mic.setOnClickListener {
            isMicMuted = !isMicMuted // Toggle mute state
            agoraEngine.muteLocalAudioStream(isMicMuted)
            Toast.makeText(this, if (isMicMuted) "Mic Muted" else "Mic Unmuted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQ_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeAgoraEngine()
            joinChannel()
            startCallTimer()
        } else {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeAgoraEngine() {
        try {
            agoraEngine = RtcEngine.create(baseContext, APP_ID, object : IRtcEngineEventHandler() {
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    runOnUiThread { Toast.makeText(this@VoiceCallPage, "User $uid joined", Toast.LENGTH_SHORT).show() }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    runOnUiThread { Toast.makeText(this@VoiceCallPage, "User $uid offline", Toast.LENGTH_SHORT).show() }
                }
            })
            agoraEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize Agora: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun joinChannel() {
        agoraEngine.joinChannel(TEMP_TOKEN, CHANNEL_NAME, "", 0)
    }

    private fun leaveChannel() {
        agoraEngine.leaveChannel()
        handler.removeCallbacksAndMessages(null)
    }

    private fun startCallTimer() {
        callStartTime = System.currentTimeMillis()
        val callDurationText = findViewById<TextView>(R.id.callDuration)
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - callStartTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                callDurationText.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        RtcEngine.destroy()
    }
}