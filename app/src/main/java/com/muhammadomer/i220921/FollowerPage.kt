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

class FollowerPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var followerAdapter: FollowerAdapter
    private var allFollowers = mutableListOf<Pair<String, userCredential>>()
    private var displayedFollowers = mutableListOf<Pair<String, userCredential>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_follower_page)

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

        // Set up RecyclerView for followers
        val followersRecyclerView = findViewById<RecyclerView>(R.id.followersRecyclerView)
        followersRecyclerView.layoutManager = LinearLayoutManager(this)
        followerAdapter = FollowerAdapter(displayedFollowers)
        followersRecyclerView.adapter = followerAdapter

        // Load current user's data
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUser = snapshot.getValue(userCredential::class.java)
                currentUser?.let {
                    // Update UI with user data
                    findViewById<TextView>(R.id.Username).text = it.username
                    findViewById<Button>(R.id.myBtn).text = "${it.followers.size} Followers"
                    findViewById<Button>(R.id.Following).text = "${it.following.size} Following"

                    // Load followers
                    loadFollowers(it.followers)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowerPage, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
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
                // If search query is empty, show all followers
                displayedFollowers.clear()
                displayedFollowers.addAll(allFollowers)
                followerAdapter.notifyDataSetChanged()
            }
        }

        // Navigation buttons
        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Already on FollowerPage, no action needed (or refresh the list if desired)
        }

        val following = findViewById<Button>(R.id.Following)
        following.setOnClickListener {
            val intent = Intent(this, FollowingPage::class.java)
            startActivity(intent)
        }

        val profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            finish() // Go back to the previous activity (likely ProfilePage)
        }
    }

    private fun loadFollowers(followerUids: List<String>) {
        allFollowers.clear()
        displayedFollowers.clear()

        if (followerUids.isEmpty()) {
            followerAdapter.notifyDataSetChanged()
            return
        }

        // Fetch userCredential for each follower UID
        for (uid in followerUids) {
            database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val follower = snapshot.getValue(userCredential::class.java)
                    follower?.let {
                        allFollowers.add(Pair(uid, it))
                        // Sort by username for consistent display
                        allFollowers.sortBy { pair -> pair.second.username.lowercase() }
                        // Update displayed list
                        displayedFollowers.clear()
                        displayedFollowers.addAll(allFollowers)
                        followerAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FollowerPage, "Failed to load follower: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun performSearch(query: String) {
        val filteredFollowers = allFollowers.filter { followerPair ->
            followerPair.second.username.lowercase().contains(query.lowercase())
        }.sortedBy { it.second.username.lowercase() }.toMutableList()

        displayedFollowers.clear()
        displayedFollowers.addAll(filteredFollowers)
        followerAdapter.notifyDataSetChanged()
    }
}