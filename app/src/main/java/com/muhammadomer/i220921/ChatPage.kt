package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import java.util.UUID

class ChatPage : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private var recipientId: String = ""
    private var chatId: String = ""
    private var userId: String? = null
    private var token: String? = null
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

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

        databaseHelper = DatabaseHelper(this)

        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
        userId = sharedPref.getString("userId", null)
        token = sharedPref.getString("token", null)

        if (userId == null || token == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LogInPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        recipientId = intent.getStringExtra("recipientId") ?: run {
            Toast.makeText(this, "Recipient ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Generate chatId deterministically
        chatId = if (userId!! < recipientId) "$userId-$recipientId" else "$recipientId-$userId"

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messageAdapter = MessageAdapter(
            this,
            messages,
            false,
            userId!!,
            recipientId,
            null,
            { fetchMessages() }
        )
        messagesRecyclerView.adapter = messageAdapter

        loadRecipientData()

        fetchMessages()

        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text, false)
                messageInput.text.clear()
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }
        }

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
                    intent.putExtra("recipientId", recipientId)
                    startActivity(intent)
                    return true
                }
                return false
            }
        })

        val back = findViewById<Button>(R.id.Back)
        back.setOnClickListener {
            val intent = Intent(this, DMPage::class.java)
            startActivity(intent)
            finish()
        }

        val voiceCall = findViewById<Button>(R.id.VoiceCall)
        voiceCall.setOnClickListener {
            val intent = Intent(this, VoiceCallPage::class.java)
            intent.putExtra("recipientId", recipientId)
            startActivity(intent)
        }

        val videoCall = findViewById<Button>(R.id.VideoCall)
        videoCall.setOnClickListener {
            val intent = Intent(this, VideoCallPage::class.java)
            intent.putExtra("recipientId", recipientId)
            startActivity(intent)
        }

        val profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            intent.putExtra("userId", recipientId)
            startActivity(intent)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    private fun fetchMessages() {
        apiService.getMessages(chatId, "Bearer $token").enqueue(object : Callback<MessagesResponse> {
            override fun onResponse(call: Call<MessagesResponse>, response: Response<MessagesResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    messages.clear()
                    messages.addAll(response.body()!!.messages)
                    messages.forEach { message ->
                        databaseHelper.insertOrUpdateMessage(
                            LocalMessage(
                                messageId = message.messageId,
                                chatId = message.chatId,
                                text = message.text,
                                imageUrl = message.imageUrl,
                                senderId = message.senderId.toLong(),
                                timestamp = message.timestamp,
                                isSeen = message.isSeen,
                                vanish = message.vanish
                            )
                        )
                    }
                    messageAdapter.notifyDataSetChanged()
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                } else {
                    loadMessagesFromSQLite()
                }
            }

            override fun onFailure(call: Call<MessagesResponse>, t: Throwable) {
                loadMessagesFromSQLite()
            }
        })
    }

    private fun loadMessagesFromSQLite() {
        val localMessages = databaseHelper.getMessagesByChatId(chatId)
        messages.clear()
        messages.addAll(localMessages.map { localMessage ->
            Message(
                messageId = localMessage.messageId,
                chatId = localMessage.chatId,
                text = localMessage.text,
                imageUrl = localMessage.imageUrl,
                senderId = localMessage.senderId.toString(),
                timestamp = localMessage.timestamp,
                isSeen = localMessage.isSeen,
                vanish = localMessage.vanish
            )
        })
        messageAdapter.notifyDataSetChanged()
        messagesRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun sendMessage(text: String, vanish: Boolean) {
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            messageId = messageId,
            chatId = chatId,
            text = text,
            imageUrl = null,
            senderId = userId!!,
            timestamp = System.currentTimeMillis(),
            isSeen = false,
            vanish = vanish
        )
        val request = SendMessageRequest(
            text = text,
            imageUrl = null,
            senderId = userId!!,
            timestamp = System.currentTimeMillis(),
            isSeen = false,
            vanish = vanish
        )
        apiService.sendMessage(chatId, "Bearer $token", request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    messages.add(message)
                    databaseHelper.insertOrUpdateMessage(
                        LocalMessage(
                            messageId = message.messageId,
                            chatId = message.chatId,
                            text = message.text,
                            imageUrl = message.imageUrl,
                            senderId = message.senderId.toLong(),
                            timestamp = message.timestamp,
                            isSeen = message.isSeen,
                            vanish = message.vanish
                        )
                    )
                    messageAdapter.notifyDataSetChanged()
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                } else {
                    queueMessage(message)
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                queueMessage(message)
            }
        })
    }

    private fun queueMessage(message: Message) {
        val action = QueuedAction(
            actionId = UUID.randomUUID().toString(),
            actionType = "send_message",
            payload = Gson().toJson(message),
            createdAt = System.currentTimeMillis()
        )
        databaseHelper.queueAction(action)
        Toast.makeText(this, "Message queued for sending", Toast.LENGTH_SHORT).show()
    }

    private fun loadRecipientData() {
        apiService.getUser(recipientId, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val user = response.body()!!.user
                    findViewById<TextView>(R.id.Username).text = user.username
                    val profilePic = findViewById<CircleImageView>(R.id.ProfilePic)
                    if (user.profileImage != null && user.profileImage.isNotEmpty()) {
                        Glide.with(this@ChatPage)
                            .load(user.profileImage)
                            .placeholder(R.drawable.chatprofilepicture1)
                            .error(R.drawable.chatprofilepicture1)
                            .into(profilePic)
                    } else {
                        profilePic.setImageResource(R.drawable.chatprofilepicture1)
                    }
                    databaseHelper.insertOrUpdateUser(
                        LocalUser(
                            userId = user.userId.toLong(),
                            name = user.name,
                            username = user.username,
                            phoneNumber = user.phoneNumber,
                            email = user.email,
                            bio = user.bio,
                            profileImage = user.profileImage,
                            postsCount = user.postsCount,
                            followersCount = user.followersCount,
                            followingCount = user.followingCount
                        )
                    )
                    loadOnlineStatus()
                } else {
                    loadRecipientDataFromSQLite()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                loadRecipientDataFromSQLite()
            }
        })
    }

    private fun loadRecipientDataFromSQLite() {
        val localUser = databaseHelper.getUserById(recipientId.toLong())
        if (localUser != null) {
            findViewById<TextView>(R.id.Username).text = localUser.username
            val profilePic = findViewById<CircleImageView>(R.id.ProfilePic)
            if (localUser.profileImage != null && localUser.profileImage.isNotEmpty()) {
                Glide.with(this@ChatPage)
                    .load(localUser.profileImage)
                    .placeholder(R.drawable.chatprofilepicture1)
                    .error(R.drawable.chatprofilepicture1)
                    .into(profilePic)
            } else {
                profilePic.setImageResource(R.drawable.chatprofilepicture1)
            }
            // Online status not available offline
            findViewById<TextView>(R.id.OnlineStatus).text = "Offline"
            findViewById<TextView>(R.id.OnlineStatus).setTextColor(resources.getColor(android.R.color.darker_gray))
        } else {
            Toast.makeText(this, "Recipient data not available offline", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOnlineStatus() {
        apiService.getUserStatus(recipientId, "Bearer $token").enqueue(object : Callback<UserStatusResponse> {
            override fun onResponse(call: Call<UserStatusResponse>, response: Response<UserStatusResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val onlineStatusText = findViewById<TextView>(R.id.OnlineStatus)
                    val isOnline = response.body()!!.isOnline
                    onlineStatusText.text = if (isOnline) "Online" else "Offline"
                    onlineStatusText.setTextColor(
                        if (isOnline) resources.getColor(android.R.color.holo_green_dark)
                        else resources.getColor(android.R.color.darker_gray)
                    )
                }
            }

            override fun onFailure(call: Call<UserStatusResponse>, t: Throwable) {
                // Fallback to offline status
                findViewById<TextView>(R.id.OnlineStatus).text = "Offline"
                findViewById<TextView>(R.id.OnlineStatus).setTextColor(resources.getColor(android.R.color.darker_gray))
            }
        })
    }
}