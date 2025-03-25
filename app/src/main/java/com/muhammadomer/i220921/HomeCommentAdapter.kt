package com.muhammadomer.i220921

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeCommentAdapter : RecyclerView.Adapter<HomeCommentAdapter.CommentViewHolder>() {

    private val comments = mutableListOf<Comment>() // List of Comment objects
    private val usernames = mutableMapOf<String, String>() // Map<userId, username>

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.CommentUsername)
        val commentText: TextView = itemView.findViewById(R.id.CommentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_post_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        val username = usernames[comment.userId] ?: "Unknown"
        holder.username.text = username
        holder.commentText.text = comment.commentText
    }

    override fun getItemCount(): Int = comments.size

    fun submitComments(newComments: List<Comment>, newUsernames: Map<String, String>) {
        comments.clear()
        // Sort comments by timestamp (newest first)
        comments.addAll(newComments.sortedByDescending { it.timestamp })
        usernames.clear()
        usernames.putAll(newUsernames)
        notifyDataSetChanged()
    }
}