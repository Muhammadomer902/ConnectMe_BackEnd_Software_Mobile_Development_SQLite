package com.muhammadomer.i220921

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class HomePostAdapter : RecyclerView.Adapter<HomePostAdapter.PostViewHolder>() {

    private val posts = mutableListOf<Pair<userCredential, Post>>()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("Posts")
    private val usersDatabase: DatabaseReference = FirebaseDatabase.getInstance().getReference("RegisteredUsers")

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
            // Removed the fixed height to allow scrolling
            // commentsRecyclerView.layoutParams.height = (3 * 40 * itemView.context.resources.displayMetrics.density).toInt()
        }
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

        // Load comments and usernames
        val comments = post.comments ?: emptyList()
        val userIds = comments.map { it.userId }.filterNotNull().distinct()
        val usernames = mutableMapOf<String, String>()
        var usersFetched = 0

        if (userIds.isNotEmpty()) {
            userIds.forEach { userId ->
                usersDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userCredential = snapshot.getValue(userCredential::class.java)
                        usernames[userId] = userCredential?.username ?: "Unknown"
                        usersFetched++

                        // When all usernames are fetched, update the comment adapter
                        if (usersFetched == userIds.size) {
                            holder.commentAdapter.submitComments(comments, usernames)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(holder.itemView.context, "Failed to load username: ${error.message}", Toast.LENGTH_SHORT).show()
                        usersFetched++
                        if (usersFetched == userIds.size) {
                            holder.commentAdapter.submitComments(comments, usernames)
                        }
                    }
                })
            }
        } else {
            holder.commentAdapter.submitComments(comments, usernames)
        }

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

            val currentLikes = post.likes?.toMutableList() ?: mutableListOf()

            if (currentLikes.contains(userId)) {
                currentLikes.remove(userId)
                Toast.makeText(holder.itemView.context, "You removed your like", Toast.LENGTH_SHORT).show()
            } else {
                currentLikes.add(userId)
                Toast.makeText(holder.itemView.context, "Post is liked", Toast.LENGTH_SHORT).show()
            }

            database.child(postId).child("likes").setValue(currentLikes)
                .addOnSuccessListener {
                    posts[position] = Pair(user, post.copy(likes = currentLikes))
                    notifyItemChanged(position)
                }
                .addOnFailureListener { error ->
                    Toast.makeText(holder.itemView.context, "Failed to update like: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Handle submit comment button click
        holder.submitCommentButton.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(holder.itemView.context, "You must be logged in to comment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val postId = post.postId
            if (postId == null) {
                Toast.makeText(holder.itemView.context, "Invalid post ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val commentText = holder.commentInput.text.toString().trim()
            if (commentText.isEmpty()) {
                Toast.makeText(holder.itemView.context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Fetch the latest comments from Firebase
            database.child(postId).child("comments").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Get the current comments list from Firebase
                    val currentComments = snapshot.getValue(object : GenericTypeIndicator<MutableList<Comment>>() {}) ?: mutableListOf()

                    // Create a new Comment object
                    val newComment = Comment(
                        userId = userId,
                        commentText = commentText,
                        timestamp = System.currentTimeMillis()
                    )

                    // Add the new comment to the list
                    currentComments.add(newComment)

                    // Update the comments list in Firebase
                    database.child(postId).child("comments").setValue(currentComments)
                        .addOnSuccessListener {
                            // Update the local post object
                            posts[position] = Pair(user, post.copy(comments = currentComments))
                            // Reload comments
                            val newUsernames = mutableMapOf<String, String>()
                            var usersFetched = 0
                            val newUserIds = currentComments.map { it.userId }.filterNotNull().distinct()
                            if (newUserIds.isNotEmpty()) {
                                newUserIds.forEach { uid ->
                                    usersDatabase.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val userCredential = snapshot.getValue(userCredential::class.java)
                                            newUsernames[uid] = userCredential?.username ?: "Unknown"
                                            usersFetched++

                                            if (usersFetched == newUserIds.size) {
                                                holder.commentAdapter.submitComments(currentComments, newUsernames)
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(holder.itemView.context, "Failed to load username: ${error.message}", Toast.LENGTH_SHORT).show()
                                            usersFetched++
                                            if (usersFetched == newUserIds.size) {
                                                holder.commentAdapter.submitComments(currentComments, newUsernames)
                                            }
                                        }
                                    })
                                }
                            } else {
                                holder.commentAdapter.submitComments(currentComments, newUsernames)
                            }
                            // Clear the input field
                            holder.commentInput.text.clear()
                            Toast.makeText(holder.itemView.context, "Comment added", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(holder.itemView.context, "Failed to add comment: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(holder.itemView.context, "Failed to fetch comments: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun getItemCount(): Int = posts.size

    fun submitPosts(newPosts: List<Pair<userCredential, Post>>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}