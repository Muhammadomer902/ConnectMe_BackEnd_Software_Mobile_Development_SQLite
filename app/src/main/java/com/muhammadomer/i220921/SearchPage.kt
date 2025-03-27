package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class SearchPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var recentSearchesAdapter: RecentSearchesAdapter
    private lateinit var searchedUsersAdapter: SearchedUsersAdapter
    private var currentFilter = "All" // Default filter
    private var allUsers = mutableListOf<userCredential>()
    private var currentUser: userCredential? = null
    private var recentSearches = mutableListOf<String>()
    private var searchedUsers = mutableListOf<userCredential>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search_page)

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

        // Set up RecyclerView for Recent Searches
        val recentSearchesRecyclerView = findViewById<RecyclerView>(R.id.recentSearchesRecyclerView)
        recentSearchesRecyclerView.layoutManager = LinearLayoutManager(this)
        recentSearchesAdapter = RecentSearchesAdapter(
            recentSearches,
            onRemoveClick = { searchQuery ->
                removeRecentSearch(searchQuery)
            },
            onClick = { searchQuery ->
                findViewById<EditText>(R.id.Search).setText(searchQuery)
                performSearch(searchQuery)
            }
        )
        recentSearchesRecyclerView.adapter = recentSearchesAdapter

        // Set up RecyclerView for Searched Users
        val searchedUsersRecyclerView = findViewById<RecyclerView>(R.id.searchedUsersRecyclerView)
        searchedUsersRecyclerView.layoutManager = LinearLayoutManager(this)
        searchedUsersAdapter = SearchedUsersAdapter(
            searchedUsers,
            currentUserId = userId,
            onFollowClick = { user ->
                sendFollowRequest(user)
            }
        )
        searchedUsersRecyclerView.adapter = searchedUsersAdapter

        // Load current user's data (for followers, following, and recent searches)
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(userCredential::class.java)
                currentUser?.let {
                    // Load recent searches (top 3 latest)
                    recentSearches.clear()
                    recentSearches.addAll(it.recentSearches.take(3))
                    recentSearchesAdapter.notifyDataSetChanged()

                    // Load all users for searching
                    loadAllUsers()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SearchPage, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Search button click listener
        val searchButton = findViewById<Button>(R.id.SearchLogo)
        val searchEditText = findViewById<EditText>(R.id.Search)
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
                // Add to recent searches
                addRecentSearch(query)
            } else {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            }
        }

        // Filter buttons
        val filterAll = findViewById<Button>(R.id.FilterAll)
        val filterFollowers = findViewById<Button>(R.id.FilterFollowers)
        val filterFollowing = findViewById<Button>(R.id.FilterFollowing)

        filterAll.setOnClickListener {
            currentFilter = "All"
            updateFilterButtons(filterAll, filterFollowers, filterFollowing)
            applyFilter()
        }

        filterFollowers.setOnClickListener {
            currentFilter = "Followers"
            updateFilterButtons(filterAll, filterFollowers, filterFollowing)
            applyFilter()
        }

        filterFollowing.setOnClickListener {
            currentFilter = "Following"
            updateFilterButtons(filterAll, filterFollowers, filterFollowing)
            applyFilter()
        }

        // Navigation buttons (unchanged)
        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            // Already on SearchPage, no need to start a new instance
        }

        val home = findViewById<Button>(R.id.Home)
        home.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
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

        val contact = findViewById<Button>(R.id.Contact)
        contact.setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
        }
    }

    private fun loadAllUsers() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(userCredential::class.java)
                    user?.let {
                        allUsers.add(it)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SearchPage, "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performSearch(query: String) {
        val filteredUsers = allUsers.filter { user ->
            user.username.lowercase().contains(query.lowercase())
        }.sortedBy { it.username.lowercase() }.toMutableList()

        searchedUsers.clear()
        searchedUsers.addAll(filteredUsers)
        applyFilter()
    }

    private fun applyFilter() {
        val filteredList = when (currentFilter) {
            "All" -> searchedUsers.toList()
            "Followers" -> searchedUsers.filter { user ->
                currentUser?.followers?.contains(user.username) == true
            }
            "Following" -> searchedUsers.filter { user ->
                currentUser?.following?.contains(user.username) == true
            }
            else -> searchedUsers.toList()
        }

        searchedUsers.clear()
        searchedUsers.addAll(filteredList)
        searchedUsersAdapter.notifyDataSetChanged()
    }

    private fun updateFilterButtons(filterAll: Button, filterFollowers: Button, filterFollowing: Button) {
        // Reset all buttons to unselected state
        filterAll.setBackgroundResource(R.drawable.rectangle_button_unselected)
        filterAll.setTextColor(getColor(R.color.brown))
        filterFollowers.setBackgroundResource(R.drawable.rectangle_button_unselected)
        filterFollowers.setTextColor(getColor(R.color.brown))
        filterFollowing.setBackgroundResource(R.drawable.rectangle_button_unselected)
        filterFollowing.setTextColor(getColor(R.color.brown))

        // Set selected state for the current filter
        when (currentFilter) {
            "All" -> {
                filterAll.setBackgroundResource(R.drawable.rectangle_button_green)
                filterAll.setTextColor(getColor(R.color.white))
            }
            "Followers" -> {
                filterFollowers.setBackgroundResource(R.drawable.rectangle_button_green)
                filterFollowers.setTextColor(getColor(R.color.white))
            }
            "Following" -> {
                filterFollowing.setBackgroundResource(R.drawable.rectangle_button_green)
                filterFollowing.setTextColor(getColor(R.color.white))
            }
        }
    }

    private fun addRecentSearch(query: String) {
        if (recentSearches.contains(query)) {
            recentSearches.remove(query)
        }
        recentSearches.add(0, query) // Add to the top
        if (recentSearches.size > 3) { // Limit to 3 recent searches
            recentSearches.removeAt(recentSearches.size - 1)
        }
        recentSearchesAdapter.notifyDataSetChanged()

        // Update recent searches in Firebase
        val userId = auth.currentUser?.uid ?: return
        database.child(userId).child("recentSearches").setValue(recentSearches)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save recent search", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeRecentSearch(query: String) {
        recentSearches.remove(query)
        recentSearchesAdapter.notifyDataSetChanged()

        // Update recent searches in Firebase
        val userId = auth.currentUser?.uid ?: return
        database.child(userId).child("recentSearches").setValue(recentSearches)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to remove recent search", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendFollowRequest(user: userCredential) {
        val currentUserId = auth.currentUser?.uid ?: return
        val targetUsername = user.username

        // Check if the user is trying to follow themselves by comparing usernames first
        if (currentUser?.username == targetUsername) {
            Toast.makeText(this@SearchPage, "You can't follow yourself", Toast.LENGTH_SHORT).show()
            return
        }

        // Search for the user in RegisteredUsers by username
        database.orderByChild("username").equalTo(targetUsername).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // There should be only one user with this username (usernames are typically unique)
                    for (userSnapshot in snapshot.children) {
                        val targetUserId = userSnapshot.key ?: return@onDataChange
                        // Double-check using UIDs to ensure the user isn't following themselves
                        if (currentUserId == targetUserId) {
                            Toast.makeText(this@SearchPage, "You can't follow yourself", Toast.LENGTH_SHORT).show()
                            return@onDataChange
                        }
                        // Get the current pendingFollowRequests list
                        database.child(targetUserId).child("pendingFollowRequests").get().addOnSuccessListener { requestSnapshot ->
                            val pendingRequests = requestSnapshot.getValue(object : GenericTypeIndicator<List<String>>() {})?.toMutableList() ?: mutableListOf()
                            if (!pendingRequests.contains(currentUserId)) {
                                pendingRequests.add(currentUserId)
                                // Update the pendingFollowRequests list in Firebase
                                database.child(targetUserId).child("pendingFollowRequests").setValue(pendingRequests)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@SearchPage, "Follow request sent to $targetUsername", Toast.LENGTH_SHORT).show()
                                        // Update UI to reflect the change
                                        searchedUsersAdapter.notifyDataSetChanged()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@SearchPage, "Failed to send follow request", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this@SearchPage, "Follow request already sent to $targetUsername", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this@SearchPage, "Failed to fetch pending requests", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@SearchPage, "User $targetUsername not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SearchPage, "Failed to search for user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}