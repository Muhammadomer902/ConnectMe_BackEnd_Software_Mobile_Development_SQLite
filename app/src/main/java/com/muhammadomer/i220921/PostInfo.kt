package com.muhammadomer.i220921

data class Post(
    val postId: String? = null,              // Unique ID for the post
    val imageUrls: List<String>? = null,     // List of image URLs
    val caption: String? = null,             // Caption or description of the post
    val timestamp: Long? = null,             // When the post was created
    val likes: List<String>? = null,         // List of user IDs who liked the post
    val comments: List<Comment>? = null      // List of comments
)

data class Comment(
    val userId: String? = null,              // User ID of the commenter
    val commentText: String? = null,         // The comment text
    val timestamp: Long? = null              // When the comment was added
)

data class PostsResponse(
    val status: String,
    val message: String,
    val posts: List<Post>
)