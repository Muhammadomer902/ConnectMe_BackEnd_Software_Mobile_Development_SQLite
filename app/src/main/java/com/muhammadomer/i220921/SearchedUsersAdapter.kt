package com.muhammadomer.i220921

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import java.net.URL

class SearchedUsersAdapter(
    private val users: MutableList<SearchUser>,
    private val currentUserId: String,
    private val onFollowClick: (SearchUser) -> Unit
) : RecyclerView.Adapter<SearchedUsersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: CircleImageView = itemView.findViewById(R.id.searchedProfilePic)
        val usernameTextView: TextView = itemView.findViewById(R.id.searchedUsername)
        val followButton: Button = itemView.findViewById(R.id.followButton)
        val followedText: TextView = itemView.findViewById(R.id.followedText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_searched_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.usernameTextView.text = user.username

        if (!user.profileImage.isNullOrEmpty()) {
            Thread {
                try {
                    val url = URL(user.profileImage)
                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                    holder.itemView.post {
                        holder.profilePic.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    holder.itemView.post {
                        holder.profilePic.setImageResource(R.drawable.dummyprofilepic)
                    }
                }
            }.start()
        } else {
            holder.profilePic.setImageResource(R.drawable.dummyprofilepic)
        }

        if (user.isFollowing) {
            holder.followButton.visibility = View.GONE
            holder.followedText.visibility = View.VISIBLE
        } else {
            holder.followButton.visibility = View.VISIBLE
            holder.followedText.visibility = View.GONE
            holder.followButton.setOnClickListener {
                onFollowClick(user)
            }
        }
    }

    override fun getItemCount(): Int = users.size
}
