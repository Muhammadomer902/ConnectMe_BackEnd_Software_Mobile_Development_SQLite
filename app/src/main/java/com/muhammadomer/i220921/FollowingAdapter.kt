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

class FollowingAdapter(
    private val following: MutableList<User>
) : RecyclerView.Adapter<FollowingAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: CircleImageView = itemView.findViewById(R.id.followerProfilePic)
        val usernameTextView: TextView = itemView.findViewById(R.id.followerUsername)
        val messageIcon: ImageView = itemView.findViewById(R.id.messageIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follower, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val followingUser = following[position]

        holder.usernameTextView.text = followingUser.username

        if (followingUser.profileImage != null && followingUser.profileImage.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(followingUser.profileImage)
                .placeholder(R.drawable.dummyprofilepic)
                .error(R.drawable.dummyprofilepic)
                .into(holder.profilePic)
        } else {
            holder.profilePic.setImageResource(R.drawable.dummyprofilepic)
        }

        holder.messageIcon.setOnClickListener {
            val intent = Intent(holder.itemView.context, ChatPage::class.java)
            intent.putExtra("recipientUid", followingUser.userId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = following.size
}