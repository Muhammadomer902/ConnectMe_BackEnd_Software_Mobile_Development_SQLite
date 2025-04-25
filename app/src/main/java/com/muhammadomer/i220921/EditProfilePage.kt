package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayOutputStream
import java.io.InputStream

class EditProfilePage : AppCompatActivity() {

    private lateinit var profileImage: CircleImageView
    private lateinit var name: EditText
    private lateinit var username: EditText
    private lateinit var contact: EditText
    private lateinit var bio: EditText
    private lateinit var updateButton: Button
    private lateinit var usernameDisplay: TextView

    private lateinit var apiService: ApiService
    private var userId: String? = null
    private var token: String? = null
    private var profileImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            profileImageUri = result.data?.data
            profileImageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                profileImage.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile_page)

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

        val sharedPref = getSharedPreferences("ConnectMePrefs", MODE_PRIVATE)
        userId = sharedPref.getString("userId", null)
        token = sharedPref.getString("token", null)

        if (userId == null || token == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("EditProfilePage", "UserId: $userId, Token: $token")

        profileImage = findViewById(R.id.ProfilePicture)
        name = findViewById(R.id.nameEditText)
        username = findViewById(R.id.usernameEditText)
        contact = findViewById(R.id.contactEditText)
        bio = findViewById(R.id.bioEditText)
        updateButton = findViewById(R.id.myBtn)
        usernameDisplay = findViewById(R.id.Username)

        loadUserData(userId!!)

        profileImage.setOnClickListener {
            openImagePicker()
        }

        updateButton.setOnClickListener {
            updateUserData(userId!!)
        }
    }

    private fun loadUserData(userId: String) {
        apiService.getUser(userId, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        name.setHint(it.name ?: "")
                        username.setHint(it.username ?: "")
                        contact.setHint(it.phoneNumber ?: "")
                        bio.setHint(it.bio.takeIf { b -> b?.isNotEmpty() == true } ?: "write your bio...")
                        usernameDisplay.text = it.name ?: ""

                        if (!it.profileImage.isNullOrEmpty()) {
                            Thread {
                                try {
                                    val url = java.net.URL(it.profileImage)
                                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                                    runOnUiThread {
                                        profileImage.setImageBitmap(bitmap)
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        Toast.makeText(this@EditProfilePage, "Failed to load image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.start()
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("EditProfilePage", "Failed to load data: ${response.code()} - ${response.message()} - $errorBody")
                    Toast.makeText(this@EditProfilePage, "Failed to load data: ${response.message()} - $errorBody", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("EditProfilePage", "Network error: ${t.message}")
                Toast.makeText(this@EditProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun checkUsernameUnique(newUsername: String, currentUserId: String, onResult: (Boolean) -> Unit) {
        apiService.checkUsername(newUsername, currentUserId, "Bearer $token").enqueue(object : Callback<CheckUsernameResponse> {
            override fun onResponse(call: Call<CheckUsernameResponse>, response: Response<CheckUsernameResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "success") {
                        onResult(result.isUnique)
                    } else {
                        Toast.makeText(this@EditProfilePage, result?.message ?: "Error checking username", Toast.LENGTH_SHORT).show()
                        onResult(false)
                    }
                } else {
                    Toast.makeText(this@EditProfilePage, "Error checking username: ${response.message()}", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<CheckUsernameResponse>, t: Throwable) {
                Toast.makeText(this@EditProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        })
    }

    private fun updateUserData(userId: String) {
        apiService.getUser(userId, "Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        val updatedName = name.text.toString().trim().ifEmpty { it.name ?: "" }
                        val updatedUsernameInput = username.text.toString().trim()
                        val updatedUsername = if (updatedUsernameInput.isNotEmpty()) updatedUsernameInput else it.username ?: ""
                        val updatedContact = contact.text.toString().trim().ifEmpty { it.phoneNumber ?: "" }
                        val updatedBio = bio.text.toString().trim().ifEmpty { it.bio ?: "" }

                        if (updatedUsername != it.username && updatedUsername.isNotEmpty()) {
                            checkUsernameUnique(updatedUsername, userId) { isUnique ->
                                if (!isUnique) {
                                    Toast.makeText(this@EditProfilePage, "Username already taken", Toast.LENGTH_SHORT).show()
                                    return@checkUsernameUnique
                                }
                                uploadImageAndUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, it.profileImage)
                            }
                        } else {
                            uploadImageAndUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, it.profileImage)
                        }
                    }
                } else {
                    Toast.makeText(this@EditProfilePage, "Failed to load data: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(this@EditProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadImageAndUpdate(
        userId: String,
        updatedName: String,
        updatedUsername: String,
        updatedContact: String,
        updatedBio: String,
        currentProfileImage: String?
    ) {
        if (profileImageUri == null) {
            performUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, currentProfileImage ?: "")
            return
        }

        val requestFile = prepareFilePart("profileImage", profileImageUri!!)
        apiService.uploadProfilePicture(userId, "Bearer $token", requestFile).enqueue(object : Callback<ImageUploadResponse> {
            override fun onResponse(call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "success") {
                        performUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, result.imageUrl)
                    } else {
                        Toast.makeText(this@EditProfilePage, result?.message ?: "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EditProfilePage, "Failed to upload image: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                Toast.makeText(this@EditProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part {
        val inputStream: InputStream? = contentResolver.openInputStream(fileUri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream?.use { input ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
        }
        val fileBytes = byteArrayOutputStream.toByteArray()
        val requestBody = RequestBody.create("image/jpeg".toMediaType(), fileBytes)
        return MultipartBody.Part.createFormData(partName, "profile_image.jpg", requestBody)
    }

    private fun performUpdate(
        userId: String,
        updatedName: String,
        updatedUsername: String,
        updatedContact: String,
        updatedBio: String,
        profileImageUrl: String
    ) {
        val updateRequest = UpdateUserRequest(
            name = updatedName,
            username = updatedUsername,
            phoneNumber = updatedContact,
            bio = updatedBio,
            profileImage = profileImageUrl
        )

        apiService.updateUser(userId, "Bearer $token", updateRequest).enqueue(object : Callback<UpdateUserResponse> {
            override fun onResponse(call: Call<UpdateUserResponse>, response: Response<UpdateUserResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "success") {
                        Toast.makeText(this@EditProfilePage, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@EditProfilePage, ProfilePage::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@EditProfilePage, result?.message ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EditProfilePage, "Failed to update profile: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateUserResponse>, t: Throwable) {
                Toast.makeText(this@EditProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}