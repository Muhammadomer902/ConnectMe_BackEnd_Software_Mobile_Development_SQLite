package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.UUID

class FinalizePostPage : AppCompatActivity() {
    private var imagePaths: ArrayList<String>? = null
    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_finalize_post_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.2.11/CONNECTME-API/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        databaseHelper = DatabaseHelper(this)

        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
        userId = sharedPref.getString("userId", null)
        token = sharedPref.getString("token", null)

        if (userId == null || token == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LogInPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        imagePaths = intent.getStringArrayListExtra("imagePaths")
        Log.d("FinalizePostPage", "Received image paths: $imagePaths")
        Log.d("FinalizePostPage", "Number of images received: ${imagePaths?.size ?: 0}")

        val recyclerView = findViewById<RecyclerView>(R.id.imagesRecyclerView)
        if (!imagePaths.isNullOrEmpty()) {
            val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = FinalizePostAdapter(imagePaths!!)
            recyclerView.setHasFixedSize(true)
            recyclerView.isNestedScrollingEnabled = false
            Log.d("FinalizePostPage", "RecyclerView set up with ${imagePaths!!.size} items")
        } else {
            Log.w("FinalizePostPage", "No images to display")
            recyclerView.visibility = RecyclerView.GONE
        }

        val captionEditText = findViewById<EditText>(R.id.captionEditText)

        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            val caption = captionEditText.text.toString().trim()
            uploadPost(caption)
        }

        val cancel = findViewById<Button>(R.id.Cancel)
        cancel.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)
            cleanup()
            finish()
        }
    }

    private fun uploadPost(caption: String) {
        if (imagePaths.isNullOrEmpty()) {
            Toast.makeText(this, "No images to post", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUrls = mutableListOf<String>()
        var uploadedCount = 0
        val totalImages = imagePaths!!.size
        val postId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        imagePaths!!.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("image", file.name, requestFile)

                apiService.uploadPostImage(userId!!, "Bearer $token", part).enqueue(object : Callback<ImageUploadResponse> {
                    override fun onResponse(call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            imageUrls.add(response.body()!!.imageUrl)
                        }
                        uploadedCount++
                        if (uploadedCount == totalImages) {
                            createPost(postId, imageUrls, caption, timestamp)
                        }
                        file.delete()
                    }

                    override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                        uploadedCount++
                        if (uploadedCount == totalImages) {
                            queuePost(postId, imageUrls, caption, timestamp)
                        }
                        file.delete()
                    }
                })
            } else {
                uploadedCount++
                if (uploadedCount == totalImages) {
                    queuePost(postId, imageUrls, caption, timestamp)
                }
            }
        }
    }

    private fun createPost(postId: String, imageUrls: List<String>, caption: String, timestamp: Long) {
        val request = CreatePostRequest(
            imageUrls = imageUrls,
            caption = if (caption.isEmpty()) null else caption,
            timestamp = timestamp
        )

        apiService.createPost(userId!!, "Bearer $token", request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val localPost = LocalPost(
                        postId = postId,
                        userId = userId!!.toLong(),
                        imageUrls = Gson().toJson(imageUrls),
                        caption = if (caption.isEmpty()) null else caption,
                        timestamp = timestamp,
                        likes = Gson().toJson(emptyList<String>())
                    )
                    databaseHelper.insertOrUpdatePost(localPost)
                    Toast.makeText(this@FinalizePostPage, "Post created successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@FinalizePostPage, HomePage::class.java)
                    startActivity(intent)
                    cleanup()
                    finish()
                } else {
                    queuePost(postId, imageUrls, caption, timestamp)
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                queuePost(postId, imageUrls, caption, timestamp)
            }
        })
    }

    private fun queuePost(postId: String, imageUrls: List<String>, caption: String, timestamp: Long) {
        val action = QueuedAction(
            actionId = UUID.randomUUID().toString(),
            actionType = "create_post",
            payload = Gson().toJson(CreatePostRequest(
                imageUrls = imageUrls,
                caption = if (caption.isEmpty()) null else caption,
                timestamp = timestamp
            )),
            createdAt = System.currentTimeMillis()
        )
        databaseHelper.queueAction(action)
        Toast.makeText(this, "Post queued for later sync", Toast.LENGTH_SHORT).show()

        val localPost = LocalPost(
            postId = postId,
            userId = userId!!.toLong(),
            imageUrls = Gson().toJson(imageUrls),
            caption = if (caption.isEmpty()) null else caption,
            timestamp = timestamp,
            likes = Gson().toJson(emptyList<String>())
        )
        databaseHelper.insertOrUpdatePost(localPost)
    }

    private fun cleanup() {
        imagePaths?.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        imagePaths?.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}