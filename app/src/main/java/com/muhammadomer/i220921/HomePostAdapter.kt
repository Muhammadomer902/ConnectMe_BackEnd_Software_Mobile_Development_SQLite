package com.muhammadomer.i220921

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import de.hdodenhof.circleimageview.CircleImageView
import java.net.URL
import android.graphics.BitmapFactory

class HomePostAdapter : RecyclerView.Adapter<HomePostAdapter.PostViewHolder>() {

    private val posts = mutableListOf<Pair<LocalUser, Post>>()
    private lateinit var databaseHelper: DatabaseHelper

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePicture: CircleImageView = itemView.findViewById(R.id.PostProfilePicture)
        val username: TextView = itemView.findViewById(R.id.PostUsername)
        val imageViewPager: ViewPager2 = itemView.findViewById(R.id.postImageViewPager)
        val likeButton: ImageView = itemView.findViewById(R.id.LikeButton)
        val commentButton: ImageView = itemView.findViewById(R.id.CommentButton)
        val usernameBeforeCaption: TextView = itemView.findViewById(R.id.UsernameBeforeCaption)
        val captionText: TextView = itemView.findViewById(R.id.CaptionText)
        val commentDropdown: LinearLayout = itemView.findViewById(R.id.CommentDropdown)
        val commentInput: EditText = itemView.findViewById(R.id.CommentInput)
        val submitCommentButton: Button = itemView.findViewById(R.id.SubmitCommentButton)
        val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.CommentsRecyclerView)
        val divider4: View = itemView.findViewById(R.id.Divider_4)

        // Comment adapter for this post
        val commentAdapter = HomeCommentAdapter()

        init {
            // Set up the CommentsRecyclerView
            commentsRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            commentsRecyclerView.adapter = commentAdapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_post, parent, false)
        databaseHelper = DatabaseHelper(parent.context)
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
            Thread {
                try {
                    val url = URL(user.profileImage)
                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                    holder.itemView.post {
                        holder.profilePicture.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    holder.itemView.post {
                        holder.profilePicture.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    }
                }
            }.start()
        } else {
            holder.profilePicture.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }

        // Set images in ViewPager2
        val imageAdapter = HomePostImageAdapter(post.imageUrls ?: emptyList())
        holder.imageViewPager.adapter = imageAdapter

        // Set caption
        holder.captionText.text = post.caption ?: ""

        // Load comments (empty as no API endpoint exists)
        val comments = post.comments ?: emptyList()
        holder.commentAdapter.submitComments(comments, emptyMap())

        // Handle comment button click (toggle dropdown visibility)
        holder.commentButton.setOnClickListener {
            if (holder.commentDropdown.visibility == View.VISIBLE) {
                holder.commentDropdown.visibility = View.GONE
                holder.divider4.visibility = View.GONE
            } else {
                holder.commentDropdown.visibility = View.VISIBLE
                holder.divider4.visibility = View.VISIBLE
            }
        }

        // Like button (disabled as no API endpoint exists)
        holder.likeButton.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Like functionality not implemented", Toast.LENGTH_SHORT).show()
        }

        // Submit comment button (disabled as no API endpoint exists)
        holder.submitCommentButton.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Comment functionality not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = posts.size

    fun submitPosts(newPosts: List<Pair<LocalUser, Post>>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}