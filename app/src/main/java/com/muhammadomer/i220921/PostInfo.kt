package com.muhammadomer.i220921

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    val postId: String? = null,              // Unique ID for the post
    val imageUrls: List<String>? = null,     // List of URLs or Base64 strings for multiple images
    val caption: String? = null,             // Caption or description of the post
    val timestamp: Long? = null,             // When the post was created
    val likes: MutableList<String>? = null,  // List of user IDs who liked the post
    val comments: MutableList<Comment>? = null // List of comments
) {
    // Default constructor for Firebase deserialization
    constructor() : this(null, null, null, null, null, null)
}

@IgnoreExtraProperties
data class Comment(
    val userId: String? = null,              // User ID of the commenter
    val commentText: String? = null,         // The comment text
    val timestamp: Long? = null              // When the comment was added
) {
    // Default constructor for Firebase deserialization
    constructor() : this(null, null, null)
}