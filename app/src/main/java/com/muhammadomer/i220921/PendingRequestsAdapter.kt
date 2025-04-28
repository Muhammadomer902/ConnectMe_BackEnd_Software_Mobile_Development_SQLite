package com.muhammadomer.i220921

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class PendingRequestsAdapter(
    private val pendingRequests: MutableList<User>,
    private val currentUserId: String,
    private val onAccept: (String) -> Unit,
    private val onReject: (String) -> Unit
) : RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: CircleImageView = itemView.findViewById(R.id.requestProfilePic)
        val usernameTextView: TextView = itemView.findViewById(R.id.requestUsername)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val rejectButton: Button = itemView.findById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = pendingRequests[position]

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

        holder.acceptButton.setOnClickListener {
            onAccept(user.userId)
        }

        holder.rejectButton.setOnClickListener {
            onReject(user.userId)
        }
    }

    override fun getItemCount(): Int = pendingRequests.size
}