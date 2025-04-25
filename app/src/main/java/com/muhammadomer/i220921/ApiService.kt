package com.muhammadomer.i220921

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    // From RegisterationPage.kt
    @POST("check-user.php")
    fun checkUser(@Body request: CheckUserRequest): Call<CheckUserResponse>

    @POST("register.php")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    // New endpoint for login
    @POST("login.php")
    fun login(
        @Query("email") email: String,
        @Query("password") password: String
    ): Call<RegisterResponse>

    // New endpoint to fetch email by username
    @GET("get-email-by-username.php")
    fun getEmailByUsername(
        @Query("username") username: String
    ): Call<UserEmailResponse>

    // From EditProfilePage.kt
    @GET("user.php")
    fun getUser(
        @Query("userId") userId: String,
        @Header("Authorization") authToken: String
    ): Call<UserResponse>

    @GET("check-username.php")
    fun checkUsername(
        @Query("username") username: String,
        @Query("userId") userId: String,
        @Header("Authorization") authToken: String
    ): Call<CheckUsernameResponse>

    @Multipart
    @POST("upload-profile-picture.php")
    fun uploadProfilePicture(
        @Query("userId") userId: String,
        @Header("Authorization") authToken: String,
        @Part profileImage: MultipartBody.Part
    ): Call<ImageUploadResponse>

    @POST("user.php")
    fun updateUser(
        @Query("userId") userId: String,
        @Header("Authorization") authToken: String,
        @Body request: UpdateUserRequest
    ): Call<UpdateUserResponse>

    // New endpoint for fetching posts
    @GET("posts.php")
    fun getPosts(
        @Query("userId") userId: String,
        @Header("Authorization") authToken: String
    ): Call<PostsResponse>
}