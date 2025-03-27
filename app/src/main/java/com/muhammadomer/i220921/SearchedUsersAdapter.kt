package com.muhammadomer.i220921

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class SearchedUsersAdapter(
    private val users: MutableList<Pair<String, userCredential>>, // Updated to Pair<String, userCredential>
    private val currentUserId: String,
    private val onFollowClick: (Pair<String, userCredential>) -> Unit // Updated callback
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
        val userPair = users[position]
        val user = userPair.second // Get the userCredential from the Pair

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

        // Check if the current user already follows this user
        val isFollowing = user.followers.contains(currentUserId)
        if (isFollowing) {
            holder.followButton.visibility = View.GONE
            holder.followedText.visibility = View.VISIBLE
        } else {
            holder.followButton.visibility = View.VISIBLE
            holder.followedText.visibility = View.GONE
            holder.followButton.setOnClickListener {
                onFollowClick(userPair) // Pass the entire Pair
            }
        }
    }

    override fun getItemCount(): Int = users.size
}