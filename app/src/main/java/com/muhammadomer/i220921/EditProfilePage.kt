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
import com.google.gson.Gson
import java.util.UUID

class EditProfilePage : AppCompatActivity() {

    private lateinit var profileImage: CircleImageView
    private lateinit var name: EditText
    private lateinit var username: EditText
    private lateinit var contact: EditText
    private lateinit var bio: EditText
    private lateinit var updateButton: Button
    private lateinit var usernameDisplay: TextView

    private lateinit var apiService: ApiService
    private lateinit var databaseHelper: DatabaseHelper
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

        databaseHelper = DatabaseHelper(this)

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

                        val localUser = LocalUser(
                            userId = userId.toLong(),
                            name = it.name ?: "",
                            username = it.username ?: "",
                            phoneNumber = it.phoneNumber ?: "",
                            email = it.email ?: "",
                            bio = it.bio,
                            profileImage = it.profileImage,
                            postsCount = it.postsCount ?: 0,
                            followersCount = it.followersCount ?: 0,
                            followingCount = it.followingCount ?: 0
                        )
                        databaseHelper.insertOrUpdateUser(localUser)
                    }
                } else {
                    Log.e("EditProfilePage", "Failed to load data: ${response.message()}")
                    Toast.makeText(this@EditProfilePage, "Failed to load data from API, trying local storage", Toast.LENGTH_LONG).show()
                    loadFromSQLite(userId.toLong())
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("EditProfilePage", "Network error: ${t.message}")
                Toast.makeText(this@EditProfilePage, "Network error, trying local storage", Toast.LENGTH_SHORT).show()
                loadFromSQLite(userId.toLong())
            }
        })
    }

    private fun loadFromSQLite(userId: Long) {
        val localUser = databaseHelper.getUserById(userId)
        if (localUser != null) {
            name.setHint(localUser.name)
            username.setHint(localUser.username)
            contact.setHint(localUser.phoneNumber)
            bio.setHint(localUser.bio.takeIf { b -> b?.isNotEmpty() == true } ?: "write your bio...")
            usernameDisplay.text = localUser.name

            if (!localUser.profileImage.isNullOrEmpty()) {
                Thread {
                    try {
                        val url = java.net.URL(localUser.profileImage)
                        val bitmap = BitmapFactory.decodeStream(url.openStream())
                        runOnUiThread {
                            profileImage.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@EditProfilePage, "Failed to load image from local data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        } else {
            Toast.makeText(this@EditProfilePage, "No local data available", Toast.LENGTH_SHORT).show()
        }
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
                        val nameInput = name.text.toString().trim()
                        val updatedName = if (nameInput.isNotEmpty()) nameInput else it.name ?: ""
                        val usernameInput = username.text.toString().trim()
                        val updatedUsername = if (usernameInput.isNotEmpty()) usernameInput else it.username ?: ""
                        val contactInput = contact.text.toString().trim()
                        val updatedContact = if (contactInput.isNotEmpty()) contactInput else it.phoneNumber ?: ""
                        val bioInput = bio.text.toString().trim()
                        val updatedBio = if (bioInput.isEmpty() && it.bio?.isNotEmpty() == true) it.bio else bioInput
                        val updatedEmail = it.email ?: databaseHelper.getUserById(userId.toLong())?.email ?: ""

                        if (updatedUsername != it.username && usernameInput.isNotEmpty()) {
                            checkUsernameUnique(updatedUsername, userId) { isUnique ->
                                if (!isUnique) {
                                    Toast.makeText(this@EditProfilePage, "Username already taken", Toast.LENGTH_SHORT).show()
                                    return@checkUsernameUnique
                                }
                                uploadImageAndUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, it.profileImage)
                            }
                        } else {
                            uploadImageAndUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, it.profileImage)
                        }
                    }
                } else {
                    Toast.makeText(this@EditProfilePage, "Failed to load data: ${response.message()}", Toast.LENGTH_SHORT).show()
                    val localUser = databaseHelper.getUserById(userId.toLong())
                    if (localUser != null) {
                        val nameInput = name.text.toString().trim()
                        val updatedName = if (nameInput.isNotEmpty()) nameInput else localUser.name
                        val usernameInput = username.text.toString().trim()
                        val updatedUsername = if (usernameInput.isNotEmpty()) usernameInput else localUser.username
                        val contactInput = contact.text.toString().trim()
                        val updatedContact = if (contactInput.isNotEmpty()) contactInput else localUser.phoneNumber
                        val bioInput = bio.text.toString().trim()
                        val updatedBio = if (bioInput.isEmpty() && localUser.bio?.isNotEmpty() == true) localUser.bio else bioInput
                        val updatedEmail = localUser.email

                        if (updatedUsername != localUser.username && usernameInput.isNotEmpty()) {
                            checkUsernameUnique(updatedUsername, userId) { isUnique ->
                                if (!isUnique) {
                                    Toast.makeText(this@EditProfilePage, "Username already taken", Toast.LENGTH_SHORT).show()
                                    return@checkUsernameUnique
                                }
                                uploadImageAndUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, localUser.profileImage)
                            }
                        } else {
                            uploadImageAndUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, localUser.profileImage)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(this@EditProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                val localUser = databaseHelper.getUserById(userId.toLong())
                if (localUser != null) {
                    val nameInput = name.text.toString().trim()
                    val updatedName = if (nameInput.isNotEmpty()) nameInput else localUser.name
                    val usernameInput = username.text.toString().trim()
                    val updatedUsername = if (usernameInput.isNotEmpty()) usernameInput else localUser.username
                    val contactInput = contact.text.toString().trim()
                    val updatedContact = if (contactInput.isNotEmpty()) contactInput else localUser.phoneNumber
                    val bioInput = bio.text.toString().trim()
                    val updatedBio = if (bioInput.isEmpty() && localUser.bio?.isNotEmpty() == true) localUser.bio else bioInput
                    val updatedEmail = localUser.email

                    if (updatedUsername != localUser.username && usernameInput.isNotEmpty()) {
                        checkUsernameUnique(updatedUsername, userId) { isUnique ->
                            if (!isUnique) {
                                Toast.makeText(this@EditProfilePage, "Username already taken", Toast.LENGTH_SHORT).show()
                                return@checkUsernameUnique
                            }
                            uploadImageAndUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, localUser.profileImage)
                        }
                    } else {
                        uploadImageAndUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, localUser.profileImage)
                    }
                }
            }
        })
    }

    private fun uploadImageAndUpdate(
        userId: String,
        updatedName: String,
        updatedUsername: String,
        updatedContact: String,
        updatedBio: String?,
        updatedEmail: String,
        currentProfileImage: String?
    ) {
        if (profileImageUri == null) {
            performUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, currentProfileImage)
            return
        }

        val mimeType = contentResolver.getType(profileImageUri!!)?.let { type ->
            if (type == "image/png") "image/png" else "image/jpeg"
        } ?: "image/jpeg"
        val requestFile = prepareFilePart("profileImage", profileImageUri!!, mimeType)
        apiService.uploadProfilePicture(userId, "Bearer $token", requestFile).enqueue(object : Callback<ImageUploadResponse> {
            override fun onResponse(call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.status == "success") {
                        performUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, result.imageUrl)
                    } else {
                        Toast.makeText(this@EditProfilePage, result?.message ?: "Failed to upload image", Toast.LENGTH_SHORT).show()
                        queueUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, null)
                    }
                } else {
                    Toast.makeText(this@EditProfilePage, "Failed to upload image: ${response.message()}", Toast.LENGTH_SHORT).show()
                    queueUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, null)
                }
            }

            override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                Toast.makeText(this@EditProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                queueUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, null)
            }
        })
    }

    private fun prepareFilePart(partName: String, fileUri: Uri, mimeType: String): MultipartBody.Part {
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
        val requestBody = RequestBody.create(mimeType.toMediaType(), fileBytes)
        return MultipartBody.Part.createFormData(partName, "profile_image.${if (mimeType == "image/png") "png" else "jpg"}", requestBody)
    }

    private fun performUpdate(
        userId: String,
        updatedName: String,
        updatedUsername: String,
        updatedContact: String,
        updatedBio: String?,
        updatedEmail: String,
        profileImageUrl: String?
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
                        val localUser = LocalUser(
                            userId = userId.toLong(),
                            name = updatedName,
                            username = updatedUsername,
                            phoneNumber = updatedContact,
                            email = updatedEmail,
                            bio = updatedBio,
                            profileImage = profileImageUrl,
                            postsCount = databaseHelper.getUserById(userId.toLong())?.postsCount ?: 0,
                            followersCount = databaseHelper.getUserById(userId.toLong())?.followersCount ?: 0,
                            followingCount = databaseHelper.getUserById(userId.toLong())?.followingCount ?: 0
                        )
                        databaseHelper.insertOrUpdateUser(localUser)

                        Toast.makeText(this@EditProfilePage, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@EditProfilePage, ProfilePage::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@EditProfilePage, result?.message ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
                        queueUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, profileImageUrl)
                    }
                } else {
                    Toast.makeText(this@EditProfilePage, "Failed to update profile: ${response.message()}", Toast.LENGTH_SHORT).show()
                    queueUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, profileImageUrl)
                }
            }

            override fun onFailure(call: Call<UpdateUserResponse>, t: Throwable) {
                Log.e("EditProfilePage", "Network error: ${t.message}")
                Toast.makeText(this@EditProfilePage, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                queueUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio, updatedEmail, profileImageUrl)
            }
        })
    }

    private fun queueUpdate(
        userId: String,
        updatedName: String,
        updatedUsername: String,
        updatedContact: String,
        updatedBio: String?,
        updatedEmail: String,
        profileImageUrl: String?
    ) {
        val action = QueuedAction(
            actionId = UUID.randomUUID().toString(),
            actionType = "update_profile",
            payload = Gson().toJson(UpdateUserRequest(
                name = updatedName,
                username = updatedUsername,
                phoneNumber = updatedContact,
                bio = updatedBio,
                profileImage = profileImageUrl
            )),
            createdAt = System.currentTimeMillis()
        )
        databaseHelper.queueAction(action)
        Toast.makeText(this@EditProfilePage, "Update queued for later sync", Toast.LENGTH_SHORT).show()

        val localUser = LocalUser(
            userId = userId.toLong(),
            name = updatedName,
            username = updatedUsername,
            phoneNumber = updatedContact,
            email = updatedEmail,
            bio = updatedBio,
            profileImage = profileImageUrl,
            postsCount = databaseHelper.getUserById(userId.toLong())?.postsCount ?: 0,
            followersCount = databaseHelper.getUserById(userId.toLong())?.followersCount ?: 0,
            followingCount = databaseHelper.getUserById(userId.toLong())?.followingCount ?: 0
        )
        databaseHelper.insertOrUpdateUser(localUser)
    }
}
