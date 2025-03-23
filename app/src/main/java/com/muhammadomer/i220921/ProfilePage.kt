package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfilePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)

        // Get the USER_ID from the intent that opened this page
        val userId = intent.getStringExtra("USER_ID")

        var editProfile = findViewById<Button>(R.id.EditProfile)
        editProfile.setOnClickListener {
            val intent = Intent(this, EditProfilePage::class.java).apply {
                putExtra("USER_ID", userId) // Pass the USER_ID to EditProfilePage
            }
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
    }
}