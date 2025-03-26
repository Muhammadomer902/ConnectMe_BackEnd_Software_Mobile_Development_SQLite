package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import java.util.concurrent.TimeUnit

class HomePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var postAdapter: HomePostAdapter
    private lateinit var storyContainer: LinearLayout
    private val handler = Handler(Looper.getMainLooper())

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

        // Find the story container
        storyContainer = findViewById<LinearLayout>(R.id.story_container)

        // Fetch and set the current user's profile picture for NewStory
        val newStoryView = findViewById<CircleImageView>(R.id.NewStory)
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(userCredential::class.java)
                user?.profileImage?.let { profileImage ->
                    try {
                        val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        newStoryView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        newStoryView.setImageResource(R.drawable.dummyprofilepic) // Fallback to placeholder
                    }
                } ?: newStoryView.setImageResource(R.drawable.dummyprofilepic) // Fallback if profileImage is null
            }

            override fun onCancelled(error: DatabaseError) {
                newStoryView.setImageResource(R.drawable.dummyprofilepic) // Fallback on error
            }
        })

        // Fetch and display stories
        fetchAndDisplayStories(userId)

        /*
        // Add dummy following value for testing
        val dummyUserId = "XOZrByDj3LhdWbD4Qc8qhpRRwhE2"
        database.child(userId).child("following").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followingList = snapshot.getValue(object : GenericTypeIndicator<MutableList<String>>() {}) ?: mutableListOf()
                if (!followingList.contains(dummyUserId)) {
                    followingList.add(dummyUserId)
                    database.child(userId).child("following").setValue(followingList)
                        .addOnSuccessListener {
                            Toast.makeText(this@HomePage, "Added dummy user to following list for testing", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(this@HomePage, "Failed to add dummy user: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomePage, "Failed to fetch following list: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        */

        // Fetch current user's data to get their following list and posts
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

        // Navigation button listeners
        val myBtn = findViewById<Button>(R.id.myBtn)
        myBtn.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        val newStory = findViewById<CircleImageView>(R.id.NewStory)
        newStory.setOnClickListener {
            val intent = Intent(this, NewStoryPage::class.java)
            startActivity(intent)
        }

        val storyMore = findViewById<ImageButton>(R.id.StoryMore)
        storyMore.setOnClickListener {
            val intent = Intent(this, NewStoryPage::class.java)
            startActivity(intent)
        }

        val dm = findViewById<Button>(R.id.DM)
        dm.setOnClickListener {
            val intent = Intent(this, DMPage::class.java)
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

        val contact = findViewById<Button>(R.id.Contact)
        contact.setOnClickListener {
            val intent = Intent(this, ContactPage::class.java)
            startActivity(intent)
        }
    }

    private fun fetchAndDisplayStories(userId: String) {
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(userCredential::class.java)
                user?.let {
                    val userIdsToFetch = mutableListOf(userId) // Include current user
                    user.following?.let { following -> userIdsToFetch.addAll(following) }

                    val storiesRef = FirebaseDatabase.getInstance().getReference("Stories")
                    val userStoriesMap = mutableMapOf<String, Pair<userCredential, MutableList<StoryInfo>>>()

                    userIdsToFetch.forEach { uid ->
                        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val fetchedUser = userSnapshot.getValue(userCredential::class.java)
                                fetchedUser?.let { u ->
                                    val storyIds = u.stories ?: emptyList()
                                    val userStories = mutableListOf<StoryInfo>()
                                    storyIds.forEach { storyId ->
                                        storiesRef.child(storyId).addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(storySnapshot: DataSnapshot) {
                                                val story = storySnapshot.getValue(StoryInfo::class.java)
                                                story?.let { s ->
                                                    // Check if the story is older than 24 hours
                                                    val currentTime = System.currentTimeMillis()
                                                    val storyAge = currentTime - (s.timestamp ?: 0)
                                                    val twentyFourHours = TimeUnit.HOURS.toMillis(24)
                                                    // Test story for 5 minutes
                                                    //val twentyFourHours = TimeUnit.MINUTES.toMillis(5)
                                                    if (storyAge > twentyFourHours) {
                                                        // Remove the story from Stories node
                                                        storiesRef.child(storyId).removeValue()
                                                        // Remove the story from user's stories list
                                                        val updatedStories = u.stories.toMutableList().apply { remove(storyId) }
                                                        database.child(uid).child("stories").setValue(updatedStories)
                                                    } else {
                                                        userStories.add(s)
                                                    }
                                                }
                                                // After processing all stories for this user, update the map
                                                if (storyId == storyIds.lastOrNull()) {
                                                    if (userStories.isNotEmpty()) {
                                                        userStoriesMap[uid] = Pair(u, userStories)
                                                    }
                                                    // If this is the last user, display the stories
                                                    if (uid == userIdsToFetch.last()) {
                                                        displayStories(userStoriesMap)
                                                    }
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Toast.makeText(this@HomePage, "Failed to load story: ${error.message}", Toast.LENGTH_SHORT).show()
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
    }

    private fun displayStories(userStoriesMap: Map<String, Pair<userCredential, MutableList<StoryInfo>>>) {
        // Clear existing dynamic story views (keep the first static one)
        storyContainer.removeViews(1, storyContainer.childCount - 1)

        // Add story thumbnails dynamically
        userStoriesMap.forEach { (userId, userStoriesPair) ->
            val user = userStoriesPair.first
            val stories = userStoriesPair.second
            val storyView = LayoutInflater.from(this).inflate(R.layout.home_story, storyContainer, false) as CircleImageView

            // Set the user's profile picture
            user.profileImage?.let { profileImage ->
                try {
                    val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    storyView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    storyView.setImageResource(R.drawable.profilepicture_1) // Fallback to placeholder
                }
            } ?: storyView.setImageResource(R.drawable.profilepicture_1) // Fallback if profileImage is null

            storyView.setOnClickListener {
                // Start StoryViewPage with the list of stories for this user
                val storyIds = stories.map { it.storyId ?: "" }
                val intent = Intent(this, StoryViewPage::class.java)
                intent.putStringArrayListExtra("storyIds", ArrayList(storyIds))
                intent.putExtra("userId", userId)
                startActivity(intent)
            }
            storyContainer.addView(storyView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}