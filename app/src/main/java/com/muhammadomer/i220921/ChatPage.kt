package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class ChatPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var recipientUid: String = ""
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private var recipientProfileBitmap: Bitmap? = null
    private lateinit var gestureDetector: GestureDetector
    private var onlineStatusListener: ValueEventListener? = null // To remove the listener later

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_page)

        // Handle system bar insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference()

        // Get recipient UID from Intent
        recipientUid = intent.getStringExtra("recipientUid") ?: run {
            Toast.makeText(this, "Recipient UID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize RecyclerView
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messageAdapter = MessageAdapter(
            this,
            messages,
            false,
            auth.currentUser?.uid ?: "",
            recipientUid,
            recipientProfileBitmap
        ) { fetchMessages() }
        messagesRecyclerView.adapter = messageAdapter

        // Load recipient's data (name, profile picture, and online status)
        loadRecipientData()

        // Fetch messages from Firebase
        fetchMessages()

        // Set up sending messages
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                messageInput.text.clear()
                // Scroll to the last message after sending
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }
        }

        // Set up swipe gesture for the entire screen using GestureDetector
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                val swipeThreshold = 250f
                if (Math.abs(diffY) > Math.abs(diffX) && diffY < 0 && Math.abs(diffY) > swipeThreshold) {
                    val intent = Intent(this@ChatPage, VanishingChatPage::class.java)
                    intent.putExtra("recipientUid", recipientUid)
                    startActivity(intent)
                    return true
                }
                return false
            }
        })

        // Navigation buttons
        val back = findViewById<Button>(R.id.Back)
        back.setOnClickListener {
            val intent = Intent(this, DMPage::class.java)
            startActivity(intent)
        }

        val voiceCall = findViewById<Button>(R.id.VoiceCall)
        voiceCall.setOnClickListener {
            val intent = Intent(this, VoiceCallPage::class.java)
            startActivity(intent)
        }

        val videoCall = findViewById<Button>(R.id.VideoCall)
        videoCall.setOnClickListener {
            val intent = Intent(this, VideoCallPage::class.java)
            startActivity(intent)
        }

        val profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            intent.putExtra("userId", recipientUid)
            startActivity(intent)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Process the touch event with GestureDetector
        if (gestureDetector.onTouchEvent(event)) {
            return true // Consume the event if a swipe is detected
        }
        // Otherwise, let the event propagate to child views
        return super.dispatchTouchEvent(event)
    }

    private fun fetchMessages() {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatPath = "Chat/$currentUserId/$recipientUid/messages"
        database.child(chatPath).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                for (data in snapshot.children) {
                    val message = data.getValue(Message::class.java)?.copy(messageId = data.key ?: "")
                    message?.let { messages.add(it) }
                }
                messageAdapter.notifyDataSetChanged()
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatPage, "Failed to load messages: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessage(text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageId = database.push().key ?: return
        val message = Message(
            messageId = messageId,
            text = text,
            senderId = currentUserId,
            timestamp = System.currentTimeMillis(),
            vanish = false // Messages in ChatPage are not in Vanish Mode
        )

        val chatPath1 = "Chat/$currentUserId/$recipientUid/messages/$messageId"
        val chatPath2 = "Chat/$recipientUid/$currentUserId/messages/$messageId"

        database.child(chatPath1).setValue(message)
        database.child(chatPath2).setValue(message)
    }

    private fun loadRecipientData() {
        val recipientRef = database.child("RegisteredUsers").child(recipientUid)
        onlineStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val recipient = snapshot.getValue(userCredential::class.java)
                recipient?.let {
                    // Update username
                    findViewById<TextView>(R.id.Username).text = it.username

                    // Update profile picture
                    val profilePic = findViewById<CircleImageView>(R.id.ProfilePic)
                    if (it.profileImage.isNotEmpty()) {
                        try {
                            val decodedImage = Base64.decode(it.profileImage, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
                            profilePic.setImageBitmap(bitmap)
                            recipientProfileBitmap = bitmap
                        } catch (e: Exception) {
                            profilePic.setImageResource(R.drawable.chatprofilepicture1)
                        }
                    } else {
                        profilePic.setImageResource(R.drawable.chatprofilepicture1)
                    }

                    // Update online status
                    val onlineStatusText = findViewById<TextView>(R.id.OnlineStatus)
                    val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                    onlineStatusText.text = if (isOnline) "Online" else "Offline"
                    onlineStatusText.setTextColor(
                        if (isOnline) resources.getColor(android.R.color.holo_green_dark)
                        else resources.getColor(android.R.color.darker_gray)
                    )

                    // Update the message adapter with the new profile bitmap
                    messageAdapter = MessageAdapter(
                        this@ChatPage,
                        messages,
                        false,
                        auth.currentUser?.uid ?: "",
                        recipientUid,
                        recipientProfileBitmap
                    ) { fetchMessages() }
                    messagesRecyclerView.adapter = messageAdapter
                    fetchMessages()
                } ?: run {
                    Toast.makeText(this@ChatPage, "Recipient data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatPage, "Failed to load recipient: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        recipientRef.addValueEventListener(onlineStatusListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the online status listener to prevent memory leaks
        onlineStatusListener?.let {
            database.child("RegisteredUsers").child(recipientUid).removeEventListener(it)
        }
    }
}