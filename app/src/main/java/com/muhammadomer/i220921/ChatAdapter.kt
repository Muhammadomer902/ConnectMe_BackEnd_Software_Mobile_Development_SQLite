package com.muhammadomer.i220921

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class ChatAdapter(
    private val chats: MutableList<Pair<String, userCredential>>,
    private val onChatClick: (String) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: CircleImageView = itemView.findViewById(R.id.chatProfilePic)
        val usernameTextView: TextView = itemView.findViewById(R.id.chatUsername)
        val cameraIcon: ImageView = itemView.findViewById(R.id.cameraIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatPair = chats[position]
        val user = chatPair.second // Get the userCredential

        holder.usernameTextView.text = user.username

        // Load profile picture
        if (user.profileImage.isNotEmpty()) {
            try {
                val decodedImage = Base64.decode(user.profileImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
                holder.profilePic.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profilePic.setImageResource(R.drawable.dummyprofilepic)
            }
        } else {
            holder.profilePic.setImageResource(R.drawable.dummyprofilepic)
        }

        // Click listener to open chat
        holder.itemView.setOnClickListener {
            onChatClick(chatPair.first) // Pass the other user's UID
        }

        // Camera icon click listener (can be implemented later)
        holder.cameraIcon.setOnClickListener {
            // TODO: Implement camera functionality if needed
        }
    }

    override fun getItemCount(): Int = chats.size
}