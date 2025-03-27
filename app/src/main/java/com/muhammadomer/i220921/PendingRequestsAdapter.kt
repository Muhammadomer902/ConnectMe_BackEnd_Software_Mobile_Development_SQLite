package com.muhammadomer.i220921

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class PendingRequestsAdapter(
    private val pendingRequests: MutableList<String>, // List of UIDs of users who sent requests
    private val currentUserId: String, // Current user's UID
    private val onRequestHandled: () -> Unit // Callback to refresh the list after handling a request
) : RecyclerView.Adapter<PendingRequestsAdapter.PendingRequestViewHolder>() {

    private val database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")
    private val userCredentials = mutableMapOf<String, userCredential>() // Cache user data

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_request, parent, false)
        return PendingRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: PendingRequestViewHolder, position: Int) {
        val requesterId = pendingRequests[position]

        // Fetch the requester's data if not already cached
        if (!userCredentials.containsKey(requesterId)) {
            database.child(requesterId).get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(userCredential::class.java)
                if (user != null) {
                    userCredentials[requesterId] = user
                    holder.bind(user, requesterId)
                }
            }
        } else {
            holder.bind(userCredentials[requesterId]!!, requesterId)
        }
    }

    override fun getItemCount(): Int = pendingRequests.size

    inner class PendingRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profilePic: CircleImageView = itemView.findViewById(R.id.pendingProfilePic)
        private val username: TextView = itemView.findViewById(R.id.pendingUsername)
        private val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)

        fun bind(user: userCredential, requesterId: String) {
            // Set the username
            username.text = user.username

            // Decode and set the profile picture
            if (user.profileImage.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(user.profileImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profilePic.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to a default image if decoding fails
                    profilePic.setImageResource(R.drawable.dummyprofilepic)
                }
            } else {
                // If no profile image is set, use a default image
                profilePic.setImageResource(R.drawable.dummyprofilepic)
            }

            // Set click listeners for Accept and Reject buttons
            acceptButton.setOnClickListener {
                acceptRequest(requesterId)
            }

            rejectButton.setOnClickListener {
                rejectRequest(requesterId)
            }
        }

        private fun acceptRequest(requesterId: String) {
            // Step 1: Add requester's UID to current user's followers
            database.child(currentUserId).get().addOnSuccessListener { snapshot ->
                val currentUser = snapshot.getValue(userCredential::class.java)
                if (currentUser != null) {
                    val updatedFollowers = currentUser.followers.toMutableList()
                    if (!updatedFollowers.contains(requesterId)) {
                        updatedFollowers.add(requesterId) // Add requester's UID
                    }
                    database.child(currentUserId).child("followers").setValue(updatedFollowers)
                }
            }

            // Step 2: Add current user's UID to requester's following
            database.child(requesterId).get().addOnSuccessListener { snapshot ->
                val requester = snapshot.getValue(userCredential::class.java)
                if (requester != null) {
                    val updatedFollowing = requester.following.toMutableList()
                    if (!updatedFollowing.contains(currentUserId)) {
                        updatedFollowing.add(currentUserId) // Add current user's UID
                    }
                    database.child(requesterId).child("following").setValue(updatedFollowing)
                }
            }

            // Step 3: Remove the request from pendingFollowRequests
            removeRequest(requesterId)
        }

        private fun rejectRequest(requesterId: String) {
            // Simply remove the request from pendingFollowRequests
            removeRequest(requesterId)
        }

        private fun removeRequest(requesterId: String) {
            database.child(currentUserId).get().addOnSuccessListener { snapshot ->
                val currentUser = snapshot.getValue(userCredential::class.java)
                if (currentUser != null) {
                    val updatedRequests = currentUser.pendingFollowRequests.toMutableList()
                    updatedRequests.remove(requesterId)
                    database.child(currentUserId).child("pendingFollowRequests").setValue(updatedRequests)
                        .addOnSuccessListener {
                            // Remove from local list and notify adapter
                            val position = pendingRequests.indexOf(requesterId)
                            if (position != -1) {
                                pendingRequests.removeAt(position)
                                notifyItemRemoved(position)
                            }
                            onRequestHandled() // Refresh the list
                        }
                }
            }
        }
    }
}