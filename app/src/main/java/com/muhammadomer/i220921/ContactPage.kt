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

class ContactPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var pendingRequestsAdapter: PendingRequestsAdapter
    private var pendingRequests = mutableListOf<String>()
    private var currentUser: userCredential? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contact_page)

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

        // Set up RecyclerView for Pending Requests
        val pendingRequestsRecyclerView = findViewById<RecyclerView>(R.id.pendingRequestsRecyclerView)
        pendingRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        pendingRequestsAdapter = PendingRequestsAdapter(
            pendingRequests,
            currentUserId = userId,
            onRequestHandled = { fetchPendingRequests() } // Refresh the list after handling a request
        )
        pendingRequestsRecyclerView.adapter = pendingRequestsAdapter

        // Load current user's data (for pendingFollowRequests)
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(userCredential::class.java)
                currentUser?.let {
                    fetchPendingRequests()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ContactPage, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Navigation buttons
        val back = findViewById<Button>(R.id.Back)
        back.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        val home = findViewById<Button>(R.id.Home)
        home.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        val search = findViewById<Button>(R.id.Search)
        search.setOnClickListener {
            val intent = Intent(this, SearchPage::class.java)
            startActivity(intent)
        }

        val newPost = findViewById<ImageButton>(R.id.NewPost)
        newPost.setOnClickListener {
            val intent = Intent(this, NewPostPage::class.java)
            startActivity(intent)
        }

        val profile = findViewById<Button>(R.id.Profile)
        profile.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
        }
    }

    private fun fetchPendingRequests() {
        database.child(auth.currentUser?.uid ?: return).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(userCredential::class.java)
            user?.let {
                pendingRequests.clear()
                pendingRequests.addAll(it.pendingFollowRequests)
                pendingRequestsAdapter.notifyDataSetChanged()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch pending requests", Toast.LENGTH_SHORT).show()
        }
    }
}