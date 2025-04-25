package com.muhammadomer.i220921

import com.google.gson.annotations.SerializedName

// From RegisterationPage.kt
data class CheckUserRequest(
    val username: String,
    val email: String
)

data class CheckUserResponse(
    val status: String,
    val message: String?
)

data class RegisterRequest(
    val name: String,
    val username: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val status: String,
    val message: String?,
    val userId: Long?,
    val token: String?
)

// From EditProfilePage.kt
data class UserResponse(
    val status: String,
    val message: String,
    val name: String?,
    val username: String?,
    val phoneNumber: String?,
    val email: String?,
    val bio: String?,
    val profileImage: String?,
    val postsCount: Int?,
    val followersCount: Int?,
    val followingCount: Int?
)

data class CheckUsernameResponse(
    val status: String,
    val message: String?,
    val isUnique: Boolean
)

data class ImageUploadResponse(
    val status: String,
    val message: String?,
    val imageUrl: String
)

data class UpdateUserRequest(
    val name: String,
    val username: String,
    val phoneNumber: String,
    val bio: String,
    val profileImage: String
)

data class UpdateUserResponse(
    val status: String,
    val message: String?
)