package com.muhammadomer.i220921

import android.app.AlertDialog
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
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

class MessageAdapter(
    private val context: Context,
    private val messages: MutableList<Message>,
    private val isVanishMode: Boolean,
    private val currentUserId: String,
    private val recipientId: String,
    private val recipientProfileBitmap: Bitmap? = null,
    private val onMessageUpdated: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_OTHER = 2
        private const val VIEW_TYPE_VANISH_MESSAGE = 3
        private const val EDIT_DELETE_WINDOW = 5 * 60 * 1000 // 5 minutes
    }

    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private var token: String? = null

    init {
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

        databaseHelper = DatabaseHelper(context)

        val sharedPref = context.getSharedPreferences("ConnectMePrefs", Context.MODE_PRIVATE)
        token = sharedPref.getString("token", null)
    }

    override fun getItemViewType(position: Int): Int {
        if (isVanishMode && position == messages.size) {
            return VIEW_TYPE_VANISH_MESSAGE
        }
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_USER else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val layout = if (isVanishMode) R.layout.item_user_message_vanish else R.layout.item_user_message
                val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_OTHER -> {
                val layout = if (isVanishMode) R.layout.item_other_message_vanish else R.layout.item_other_message
                val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
                OtherMessageViewHolder(view)
            }
            VIEW_TYPE_VANISH_MESSAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vanish_message, parent, false)
                VanishMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserMessageViewHolder -> {
                val message = messages[position]
                holder.bind(message)
                holder.itemView.setOnLongClickListener {
                    if (canEditOrDelete(message) && message.imageUrl == null) {
                        showEditDeleteDialog(message, position)
                    }
                    true
                }
                if (isVanishMode && !message.isSeen && message.senderId != currentUserId) {
                    markMessageAsSeen(message, position)
                }
            }
            is OtherMessageViewHolder -> {
                val message = messages[position]
                holder.bind(message)
                if (isVanishMode && !message.isSeen && message.senderId != currentUserId) {
                    markMessageAsSeen(message, position)
                }
            }
            is VanishMessageViewHolder -> {
                // Static text in layout
            }
        }
    }

    override fun getItemCount(): Int {
        return if (isVanishMode) messages.size + 1 else messages.size
    }

    private fun canEditOrDelete(message: Message): Boolean {
        val currentTime = System.currentTimeMillis()
        return message.senderId == currentUserId && (currentTime - message.timestamp) <= EDIT_DELETE_WINDOW
    }

    private fun showEditDeleteDialog(message: Message, position: Int) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(context)
            .setTitle("Message Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(message, position)
                    1 -> deleteMessage(message, position)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(message: Message, position: Int) {
        val editText = EditText(context).apply {
            setText(message.text)
        }
        AlertDialog.Builder(context)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    val updatedMessage = message.copy(text = newText)
                    updateMessage(updatedMessage, position)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateMessage(message: Message, position: Int) {
        val request = UpdateMessageRequest(text = message.text)
        apiService.updateMessage(message.chatId, message.messageId, "Bearer $token", request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    messages[position] = message
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
                    notifyItemChanged(position)
                    onMessageUpdated()
                } else {
                    queueAction("update_message", Gson().toJson(mapOf("message" to message)))
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                queueAction("update_message", Gson().toJson(mapOf("message" to message)))
            }
        })
    }

    private fun deleteMessage(message: Message, position: Int) {
        apiService.deleteMessage(message.chatId, message.messageId, "Bearer $token").enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    messages.removeAt(position)
                    databaseHelper.deleteMessage(message.messageId)
                    notifyItemRemoved(position)
                    onMessageUpdated()
                } else {
                    queueAction("delete_message", Gson().toJson(mapOf("messageId" to message.messageId, "chatId" to message.chatId)))
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                queueAction("delete_message", Gson().toJson(mapOf("messageId" to message.messageId, "chatId" to message.chatId)))
            }
        })
    }

    private fun markMessageAsSeen(message: Message, position: Int) {
        val updatedMessage = message.copy(isSeen = true)
        apiService.updateMessage(message.chatId, message.messageId, "Bearer $token", UpdateMessageRequest(message.text)).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    messages[position] = updatedMessage
                    databaseHelper.insertOrUpdateMessage(
                        LocalMessage(
                            messageId = updatedMessage.messageId,
                            chatId = updatedMessage.chatId,
                            text = updatedMessage.text,
                            imageUrl = updatedMessage.imageUrl,
                            senderId = updatedMessage.senderId.toLong(),
                            timestamp = updatedMessage.timestamp,
                            isSeen = updatedMessage.isSeen,
                            vanish = updatedMessage.vanish
                        )
                    )
                    notifyItemChanged(position)
                    onMessageUpdated()
                } else {
                    queueAction("mark_message_seen", Gson().toJson(mapOf("message" to updatedMessage)))
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                queueAction("mark_message_seen", Gson().toJson(mapOf("message" to updatedMessage)))
            }
        })
    }

    private fun queueAction(actionType: String, payload: String) {
        val action = QueuedAction(
            actionId = UUID.randomUUID().toString(),
            actionType = actionType,
            payload = payload,
            createdAt = System.currentTimeMillis()
        )
        databaseHelper.queueAction(action)
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.userMessageText)
        private val messageImage: ImageView = itemView.findViewById(R.id.userMessageImage)
        private val timestamp: TextView = itemView.findViewById(R.id.userMessageTimestamp)

        fun bind(message: Message) {
            if (message.text.isNotEmpty()) {
                messageText.visibility = View.VISIBLE
                messageText.text = message.text
                messageImage.visibility = View.GONE
            } else if (message.imageUrl != null && message.imageUrl.isNotEmpty()) {
                messageText.visibility = View.GONE
                messageImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.dummyprofilepic)
                    .error(R.drawable.dummyprofilepic)
                    .into(messageImage)
            } else {
                messageText.visibility = View.GONE
                messageImage.visibility = View.GONE
            }

            timestamp.text = DateUtils.getRelativeTimeSpanString(
                message.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
        }
    }

    class OtherMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profilePic: CircleImageView = itemView.findViewById(R.id.profilePic)
        private val messageText: TextView = itemView.findViewById(R.id.otherMessageText)
        private val messageImage: ImageView = itemView.findViewById(R.id.otherMessageImage)
        private val timestamp: TextView = itemView.findViewById(R.id.otherMessageTimestamp)

        fun bind(message: Message) {
            if (message.text.isNotEmpty()) {
                messageText.visibility = View.VISIBLE
                messageText.text = message.text
                messageImage.visibility = View.GONE
            } else if (message.imageUrl != null && message.imageUrl.isNotEmpty()) {
                messageText.visibility = View.GONE
                messageImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.dummyprofilepic)
                    .error(R.drawable.dummyprofilepic)
                    .into(messageImage)
            } else {
                messageText.visibility = View.GONE
                messageImage.visibility = View.GONE
            }

            timestamp.text = DateUtils.getRelativeTimeSpanString(
                message.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            // Profile picture is loaded in ChatPage
        }
    }

    class VanishMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Static text in layout
    }
}