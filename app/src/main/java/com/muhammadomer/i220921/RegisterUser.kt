package com.muhammadomer.i220921

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class userCredential(
    val name: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val password: String = "",
    val bio: String = "",
    val profileImage: String = "",
    val posts: List<String> = emptyList(),
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val stories: List<String> = emptyList(),
    val pendingFollowRequests: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isOnline: Boolean = false // Added isOnline field
)
{
    // Default constructor for Firebase deserialization
    constructor() : this("", "", "", "", "", "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), false)
}