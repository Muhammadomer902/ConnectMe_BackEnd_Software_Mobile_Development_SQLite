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

class DMPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var chatAdapter: ChatAdapter
    private var allChats = mutableListOf<Pair<String, userCredential>>()
    private var displayedChats = mutableListOf<Pair<String, userCredential>>()
    private var allRegisteredUsers = mutableListOf<Pair<String, userCredential>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dmpage)

        // Handle system bar insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference()
        val userId = auth.currentUser?.uid ?: return

        // Set up RecyclerView for chats
        val dmRecyclerView = findViewById<RecyclerView>(R.id.dmRecyclerView)
        dmRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(displayedChats) { recipientUid ->
            val intent = Intent(this, ChatPage::class.java)
            intent.putExtra("recipientUid", recipientUid)
            startActivity(intent)
        }
        dmRecyclerView.adapter = chatAdapter

        // Load current user's data
        database.child("RegisteredUsers").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUser = snapshot.getValue(userCredential::class.java)
                currentUser?.let {
                    // Update UI with user data
                    findViewById<TextView>(R.id.Username).text = it.username
                    // Load chats
                    loadChats(userId)
                    // Load all registered users for search
                    loadAllRegisteredUsers()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DMPage, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
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
                // If search query is empty, show all chats
                displayedChats.clear()
                displayedChats.addAll(allChats)
                chatAdapter.notifyDataSetChanged()
            }
        }

        // Navigation buttons
        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Already on DMPage, no action needed (or refresh the list if desired)
        }

        val request = findViewById<Button>(R.id.Request)
        request.setOnClickListener {
            // TODO: Navigate to Requests page (not implemented yet)
            Toast.makeText(this, "Requests page not implemented yet", Toast.LENGTH_SHORT).show()
        }

        val back = findViewById<Button>(R.id.Back)
        back.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }
    }

    private fun loadChats(userId: String) {
        allChats.clear()
        displayedChats.clear()

        // Fetch chats for the current user
        database.child("Chat").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    chatAdapter.notifyDataSetChanged()
                    return
                }

                // Get the list of users the current user has chatted with
                for (chatSnapshot in snapshot.children) {
                    val otherUserId = chatSnapshot.key ?: continue
                    // Fetch the other user's userCredential
                    database.child("RegisteredUsers").child(otherUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val otherUser = userSnapshot.getValue(userCredential::class.java)
                            otherUser?.let {
                                allChats.add(Pair(otherUserId, it))
                                // Sort by username for consistent display
                                allChats.sortBy { pair -> pair.second.username.lowercase() }
                                // Update displayed list
                                displayedChats.clear()
                                displayedChats.addAll(allChats)
                                chatAdapter.notifyDataSetChanged()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@DMPage, "Failed to load user: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DMPage, "Failed to load chats: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAllRegisteredUsers() {
        allRegisteredUsers.clear()

        // Fetch all registered users
        database.child("RegisteredUsers").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    if (userId == auth.currentUser?.uid) continue // Skip the current user
                    val user = userSnapshot.getValue(userCredential::class.java)
                    user?.let {
                        allRegisteredUsers.add(Pair(userId, it))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DMPage, "Failed to load registered users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performSearch(query: String) {
        // Filter all registered users based on the search query
        val filteredUsers = allRegisteredUsers.filter { userPair ->
            userPair.second.username.lowercase().contains(query.lowercase())
        }.sortedBy { it.second.username.lowercase() }.toMutableList()

        // Update the displayed list to show only users who have chats (if any) or allow starting a new chat
        displayedChats.clear()
        for (user in filteredUsers) {
            val existingChat = allChats.find { it.first == user.first }
            if (existingChat != null) {
                displayedChats.add(existingChat)
            } else {
                displayedChats.add(user) // Allow starting a new chat with this user
            }
        }
        chatAdapter.notifyDataSetChanged()
    }
}