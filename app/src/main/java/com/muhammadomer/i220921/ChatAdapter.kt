package com.muhammadomer.i220921

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class ChatAdapter(
    private val chats: MutableList<Chat>,
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
        val chat = chats[position]
        val user = chat.otherUser

        holder.usernameTextView.text = user.username

        if (user.profileImage != null && user.profileImage.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.profileImage)
                .placeholder(R.drawable.dummyprofilepic)
                .error(R.drawable.dummyprofilepic)
                .into(holder.profilePic)
        } else {
            holder.profilePic.setImageResource(R.drawable.dummyprofilepic)
        }

        holder.itemView.setOnClickListener {
            onChatClick(user.userId)
        }

        holder.cameraIcon.setOnClickListener {
            val intent = Intent(holder.itemView.context, NewChatImagePage::class.java)
            intent.putExtra("recipientId", user.userId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chats.size
}