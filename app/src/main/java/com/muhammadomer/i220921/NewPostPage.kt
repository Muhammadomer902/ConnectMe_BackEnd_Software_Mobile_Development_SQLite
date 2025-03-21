package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NewPostPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_post_page)
        var myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)

        }

        var cancel = findViewById<Button>(R.id.Cancel)
        cancel.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)

        }

        var post = findViewById<Button>(R.id.NewStory)
        post.setOnClickListener {
            val intent = Intent(this, NewStoryPage::class.java)
            startActivity(intent)

        }

        var gallery = findViewById<Button>(R.id.Gallery)
        gallery.setOnClickListener {
            val intent = Intent(this, GalleryPage::class.java)
            startActivity(intent)

        }

        var finalizePost = findViewById<Button>(R.id.FinalizePost)
        finalizePost.setOnClickListener {
            val intent = Intent(this, FinalizePostPage::class.java)
            startActivity(intent)

        }

    }
}