package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var postAdapter: HomePostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")
        val userId = auth.currentUser?.uid ?: return

        // Set up RecyclerView for posts
        val recyclerView = findViewById<RecyclerView>(R.id.postRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = HomePostAdapter()
        recyclerView.adapter = postAdapter

        // Fetch current user's data to get their following list
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(userCredential::class.java)
                user?.let {
                    val userIdsToFetch = mutableListOf(userId) // Include current user
                    user.following?.let { following -> userIdsToFetch.addAll(following) }

                    // Fetch posts from all relevant users
                    val postsRef = FirebaseDatabase.getInstance().getReference("Posts")
                    val postList = mutableListOf<Pair<userCredential, Post>>()

                    userIdsToFetch.forEach { uid ->
                        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val fetchedUser = userSnapshot.getValue(userCredential::class.java)
                                fetchedUser?.let { u ->
                                    u.posts?.forEach { postId ->
                                        postsRef.child(postId).addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(postSnapshot: DataSnapshot) {
                                                val post = postSnapshot.getValue(Post::class.java)
                                                post?.let { p ->
                                                    postList.add(Pair(u, p))
                                                    // Sort by timestamp descending (latest first)
                                                    postList.sortByDescending { it.second.timestamp }
                                                    postAdapter.submitPosts(postList)
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Toast.makeText(this@HomePage, "Failed to load post: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@HomePage, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomePage, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Existing button listeners
        var myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        var newStory = findViewById<ImageButton>(R.id.NewStory)
        newStory.setOnClickListener {
            val intent = Intent(this, NewStoryPage::class.java)
            startActivity(intent)
        }

        var storyMore = findViewById<ImageButton>(R.id.StoryMore)
        storyMore.setOnClickListener {
            val intent = Intent(this, NewStoryPage::class.java)
            startActivity(intent)
        }

        var DM = findViewById<Button>(R.id.DM)
        DM.setOnClickListener {
            val intent = Intent(this, DMPage::class.java)
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

        var profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        var contact = findViewById<Button>(R.id.Contact)
        contact.setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
        }
    }
}