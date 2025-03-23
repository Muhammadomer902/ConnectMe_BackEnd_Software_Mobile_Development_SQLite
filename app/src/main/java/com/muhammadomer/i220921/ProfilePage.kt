package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class ProfilePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")
        val userId = auth.currentUser?.uid ?: return

        // Load profile picture, bio, name, and counts
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(userCredential::class.java)
                user?.let {
                    // Load profile picture into CircleImageView
                    if (it.profileImage?.isNotEmpty() == true) {
                        val decodedImage = Base64.decode(it.profileImage, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
                        findViewById<CircleImageView>(R.id.ProfilePic).setImageBitmap(bitmap)
                    }

                    // Load bio if it exists
                    if (it.bio?.isNotEmpty() == true) {
                        findViewById<TextView>(R.id.Bio).text = it.bio
                    }

                    // Load name if it exists
                    if (it.name?.isNotEmpty() == true) {
                        findViewById<TextView>(R.id.Name).text = it.name
                    }

                    // Load post count (0 if empty or null)
                    findViewById<TextView>(R.id.PostNum).text = (it.posts?.size ?: 0).toString()

                    // Load followers count (0 if empty or null)
                    findViewById<Button>(R.id.Follower).text = (it.followers?.size ?: 0).toString()

                    // Load following count (0 if empty or null)
                    findViewById<Button>(R.id.Following).text = (it.following?.size ?: 0).toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfilePage, "Failed to load profile data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        var editProfile = findViewById<Button>(R.id.EditProfile)
        editProfile.setOnClickListener {
            val intent = Intent(this, EditProfilePage::class.java)
            startActivity(intent)
        }

        var home = findViewById<Button>(R.id.Home)
        home.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        var search = findViewById<Button>(R.id.Search)
        search.setOnClickListener {
            val intent = Intent(this, SearchPage::class.java)
            startActivity(intent)
        }

        var newPost = findViewById<ImageButton>(R.id.NewPost)
        newPost.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)
        }

        var myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        var contact = findViewById<Button>(R.id.Contact)
        contact.setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
        }

        var follower = findViewById<Button>(R.id.Follower)
        follower.setOnClickListener {
            val intent = Intent(this, FollowerPage::class.java)
            startActivity(intent)
        }

        var following = findViewById<Button>(R.id.Following)
        following.setOnClickListener {
            val intent = Intent(this, FollowingPage::class.java)
            startActivity(intent)
        }

        var logout = findViewById<Button>(R.id.LogoutButton)
        logout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LogInPage::class.java)
            startActivity(intent)
            finish()
        }
    }
}