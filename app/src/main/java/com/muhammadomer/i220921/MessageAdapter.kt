package com.muhammadomer.i220921

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class MessageAdapter(
    private val context: Context,
    private val messages: MutableList<Message>,
    private val isVanishMode: Boolean,
    private val currentUserId: String,
    private val recipientUid: String,
    private val recipientProfileBitmap: Bitmap? = null,
    private val onMessageUpdated: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_OTHER = 2
        private const val VIEW_TYPE_VANISH_MESSAGE = 3
        private const val EDIT_DELETE_WINDOW = 5 * 60 * 1000 // 5 minutes in milliseconds
    }

    override fun getItemViewType(position: Int): Int {
        // If in Vanish Mode and at the last position, show the VanishMssg
        if (isVanishMode && position == messages.size) {
            return VIEW_TYPE_VANISH_MESSAGE
        }
        // Otherwise, determine if it's a user or other message
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
                    if (canEditOrDelete(message) && message.image.isEmpty()) { // Only allow edit/delete for text messages
                        showEditDeleteDialog(message, position)
                    }
                    true
                }
                // Mark message as seen in Vanish Mode
                if (isVanishMode && !message.isSeen) {
                    message.isSeen = true
                    updateMessageInFirebase(message)
                }
            }
            is OtherMessageViewHolder -> {
                val message = messages[position]
                holder.bind(message, recipientProfileBitmap)
                // Mark message as seen in Vanish Mode
                if (isVanishMode && !message.isSeen) {
                    message.isSeen = true
                    updateMessageInFirebase(message)
                }
            }
            is VanishMessageViewHolder -> {
                // No binding needed, the text is static in the layout
            }
        }
    }

    override fun getItemCount(): Int {
        // Add 1 to the count if in Vanish Mode to account for the VanishMssg
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
                    0 -> showEditDialog(message, position) // Edit
                    1 -> deleteMessage(message, position) // Delete
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
                    updateMessageInFirebase(updatedMessage)
                    messages[position] = updatedMessage
                    notifyItemChanged(position)
                    onMessageUpdated()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage(message: Message, position: Int) {
        val database = FirebaseDatabase.getInstance().reference
        val chatPath1 = "Chat/$currentUserId/$recipientUid/messages/${message.messageId}"
        val chatPath2 = "Chat/$recipientUid/$currentUserId/messages/${message.messageId}"

        database.child(chatPath1).removeValue()
        database.child(chatPath2).removeValue()
        messages.removeAt(position)
        notifyItemRemoved(position)
        onMessageUpdated()
    }

    private fun updateMessageInFirebase(message: Message) {
        val database = FirebaseDatabase.getInstance().reference
        val chatPath1 = "Chat/$currentUserId/$recipientUid/messages/${message.messageId}"
        val chatPath2 = "Chat/$recipientUid/$currentUserId/messages/${message.messageId}"

        database.child(chatPath1).setValue(message)
        database.child(chatPath2).setValue(message)
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.userMessageText)
        private val messageImage: ImageView = itemView.findViewById(R.id.userMessageImage)
        private val timestamp: TextView = itemView.findViewById(R.id.userMessageTimestamp)

        fun bind(message: Message) {
            // Display text message
            if (message.text.isNotEmpty()) {
                messageText.visibility = View.VISIBLE
                messageText.text = message.text
                messageImage.visibility = View.GONE
            }
            // Display image message
            else if (message.image.isNotEmpty()) {
                messageText.visibility = View.GONE
                messageImage.visibility = View.VISIBLE
                try {
                    val decodedImage = Base64.decode(message.image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
                    messageImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    messageImage.setImageResource(R.drawable.dummyprofilepic) // Use existing drawable
                }
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

        fun bind(message: Message, profileBitmap: Bitmap?) {
            // Display text message
            if (message.text.isNotEmpty()) {
                messageText.visibility = View.VISIBLE
                messageText.text = message.text
                messageImage.visibility = View.GONE
            }
            // Display image message
            else if (message.image.isNotEmpty()) {
                messageText.visibility = View.GONE
                messageImage.visibility = View.VISIBLE
                try {
                    val decodedImage = Base64.decode(message.image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
                    messageImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    messageImage.setImageResource(R.drawable.dummyprofilepic) // Use existing drawable
                }
            } else {
                messageText.visibility = View.GONE
                messageImage.visibility = View.GONE
            }

            timestamp.text = DateUtils.getRelativeTimeSpanString(
                message.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            // Set the profile picture if available
            profileBitmap?.let {
                profilePic.setImageBitmap(it)
            }
        }
    }

    class VanishMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // No additional binding needed, the text is static in the layout
    }
}