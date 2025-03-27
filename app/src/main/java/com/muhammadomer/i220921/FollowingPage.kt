package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FollowingPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var followingAdapter: FollowingAdapter
    private var allFollowing = mutableListOf<Pair<String, userCredential>>()
    private var displayedFollowing = mutableListOf<Pair<String, userCredential>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_following_page)

        // Handle system bar insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")
        val userId = auth.currentUser?.uid ?: return

        // Set up RecyclerView for following
        val followingRecyclerView = findViewById<RecyclerView>(R.id.followingRecyclerView)
        followingRecyclerView.layoutManager = LinearLayoutManager(this)
        followingAdapter = FollowingAdapter(displayedFollowing)
        followingRecyclerView.adapter = followingAdapter

        // Load current user's data
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUser = snapshot.getValue(userCredential::class.java)
                currentUser?.let {
                    // Update UI with user data
                    findViewById<TextView>(R.id.Username).text = it.username
                    findViewById<Button>(R.id.Follower).text = "${it.followers.size} Followers"
                    findViewById<Button>(R.id.myBtn).text = "${it.following.size} Following"

                    // Load following
                    loadFollowing(it.following)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowingPage, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Search button click listener
        val searchButton = findViewById<Button>(R.id.SearchLogo)
        val searchEditText = findViewById<EditText>(R.id.Search)
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                // If search query is empty, show all following
                displayedFollowing.clear()
                displayedFollowing.addAll(allFollowing)
                followingAdapter.notifyDataSetChanged()
            }
        }

        // Navigation buttons
        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Already on FollowingPage, no action needed (or refresh the list if desired)
        }

        val follower = findViewById<Button>(R.id.Follower)
        follower.setOnClickListener {
            val intent = Intent(this, FollowerPage::class.java)
            startActivity(intent)
        }

        val profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            finish() // Go back to the previous activity (likely ProfilePage)
        }
    }

    private fun loadFollowing(followingUids: List<String>) {
        allFollowing.clear()
        displayedFollowing.clear()

        if (followingUids.isEmpty()) {
            followingAdapter.notifyDataSetChanged()
            return
        }

        // Fetch userCredential for each following UID
        for (uid in followingUids) {
            database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followingUser = snapshot.getValue(userCredential::class.java)
                    followingUser?.let {
                        allFollowing.add(Pair(uid, it))
                        // Sort by username for consistent display
                        allFollowing.sortBy { pair -> pair.second.username.lowercase() }
                        // Update displayed list
                        displayedFollowing.clear()
                        displayedFollowing.addAll(allFollowing)
                        followingAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FollowingPage, "Failed to load following: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun performSearch(query: String) {
        val filteredFollowing = allFollowing.filter { followingPair ->
            followingPair.second.username.lowercase().contains(query.lowercase())
        }.sortedBy { it.second.username.lowercase() }.toMutableList()

        displayedFollowing.clear()
        displayedFollowing.addAll(filteredFollowing)
        followingAdapter.notifyDataSetChanged()
    }
}