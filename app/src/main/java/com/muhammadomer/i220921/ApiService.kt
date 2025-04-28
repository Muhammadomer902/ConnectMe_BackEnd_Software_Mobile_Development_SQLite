package com.muhammadomer.i220921

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("check-user.php")
    fun checkUser(@Query("email") email: String): Call<CheckUserResponse>

    @POST("register.php")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @GET("login.php")
    fun login(@Query("email") email: String, @Query("password") password: String): Call<RegisterResponse>

    @GET("get-email-by-username.php")
    fun getEmailByUsername(@Query("username") username: String): Call<EmailResponse>

    @GET("user.php")
    fun getUser(@Query("userId") userId: String, @Header("Authorization") authToken: String): Call<UserResponse>

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

    @GET("posts.php")
    fun getPosts(@Query("userId") userId: String, @Header("Authorization") authToken: String): Call<PostsResponse>

    // New endpoints for search and follow
    @GET("search-users.php")
    fun searchUsers(
        @Query("query") query: String,
        @Query("userId") userId: String,
        @Header("Authorization") authToken: String
    ): Call<SearchUsersResponse>

    @POST("follow.php")
    fun followUser(
        @Query("userId") userId: String,
        @Query("targetUserId") targetUserId: String,
        @Header("Authorization") authToken: String
    ): Call<GenericResponse>
}

data class SearchUser(
    val userId: String,
    val username: String,
    val name: String,
    val profileImage: String?,
    val isFollowing: Boolean
)

data class SearchUsersResponse(
    val status: String,
    val message: String,
    val users: List<SearchUser>
)

data class GenericResponse(
    val status: String,
    val message: String
)
