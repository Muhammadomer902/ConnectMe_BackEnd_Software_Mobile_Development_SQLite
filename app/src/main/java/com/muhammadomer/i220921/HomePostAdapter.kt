package com.muhammadomer.i220921

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class HomePostAdapter : RecyclerView.Adapter<HomePostAdapter.PostViewHolder>() {

    private val posts = mutableListOf<Pair<userCredential, Post>>()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("Posts")

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePicture: CircleImageView = itemView.findViewById(R.id.PostProfilePicture)
        val username: TextView = itemView.findViewById(R.id.PostUsername)
        val imageViewPager: ViewPager2 = itemView.findViewById(R.id.postImageViewPager)
        val likeButton: ImageView = itemView.findViewById(R.id.LikeButton)
        val usernameBeforeCaption: TextView = itemView.findViewById(R.id.UsernameBeforeCaption)
        val captionText: TextView = itemView.findViewById(R.id.CaptionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val (user, post) = posts[position]

        // Set username in header
        holder.username.text = user.username

        // Set username before caption
        holder.usernameBeforeCaption.text = user.username

        // Set profile picture (if available)
        if (!user.profileImage.isNullOrEmpty()) {
            try {
                val decodedImage = android.util.Base64.decode(user.profileImage, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
                holder.profilePicture.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profilePicture.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            }
        } else {
            holder.profilePicture.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }

        // Set images in ViewPager2
        val imageAdapter = HomePostImageAdapter(post.imageUrls ?: emptyList())
        holder.imageViewPager.adapter = imageAdapter

        // Set caption
        holder.captionText.text = post.caption ?: ""

        // Handle like button click
        holder.likeButton.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(holder.itemView.context, "You must be logged in to like a post", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val postId = post.postId
            if (postId == null) {
                Toast.makeText(holder.itemView.context, "Invalid post ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the current likes list from the post
            val currentLikes = post.likes?.toMutableList() ?: mutableListOf()

            if (currentLikes.contains(userId)) {
                // User has already liked the post, so remove their like
                currentLikes.remove(userId)
                Toast.makeText(holder.itemView.context, "You removed your like", Toast.LENGTH_SHORT).show()
            } else {
                // User has not liked the post, so add their like
                currentLikes.add(userId)
                Toast.makeText(holder.itemView.context, "Post is liked", Toast.LENGTH_SHORT).show()
            }

            // Update the likes list in Firebase
            database.child(postId).child("likes").setValue(currentLikes)
                .addOnSuccessListener {
                    // Update the local post object to reflect the change
                    posts[position] = Pair(user, post.copy(likes = currentLikes))
                    notifyItemChanged(position)
                }
                .addOnFailureListener { error ->
                    Toast.makeText(holder.itemView.context, "Failed to update like: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun getItemCount(): Int = posts.size

    fun submitPosts(newPosts: List<Pair<userCredential, Post>>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}