package com.muhammadomer.i220921

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
        private const val EDIT_DELETE_WINDOW = 5 * 60 * 1000 // 5 minutes in milliseconds
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_USER else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val layout = if (isVanishMode) R.layout.item_user_message_vanish else R.layout.item_user_message
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            UserMessageViewHolder(view)
        } else {
            val layout = if (isVanishMode) R.layout.item_other_message_vanish else R.layout.item_other_message
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            OtherMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserMessageViewHolder) {
            holder.bind(message)
            holder.itemView.setOnLongClickListener {
                if (canEditOrDelete(message)) {
                    showEditDeleteDialog(message, position)
                }
                true
            }
        } else if (holder is OtherMessageViewHolder) {
            holder.bind(message, recipientProfileBitmap)
        }
        // Mark message as seen in Vanish Mode
        if (isVanishMode && !message.isSeen) {
            message.isSeen = true
            updateMessageInFirebase(message)
        }
    }

    override fun getItemCount(): Int = messages.size

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
        private val timestamp: TextView = itemView.findViewById(R.id.userMessageTimestamp)

        fun bind(message: Message) {
            messageText.text = message.text
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
        private val timestamp: TextView = itemView.findViewById(R.id.otherMessageTimestamp)

        fun bind(message: Message, profileBitmap: Bitmap?) {
            messageText.text = message.text
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
}