package com.muhammadomer.i220921

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
    val following: List<String> = emptyList()
)
