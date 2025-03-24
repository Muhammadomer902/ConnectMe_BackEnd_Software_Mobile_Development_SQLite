package com.muhammadomer.i220921

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.*
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class ProfilePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var postAdapter: ProfilePostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("RegisteredUsers")
        val userId = auth.currentUser?.uid ?: return

        // Set up RecyclerView with 3 columns
        val recyclerView = findViewById<RecyclerView>(R.id.postRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        postAdapter = ProfilePostAdapter()
        recyclerView.adapter = postAdapter

        // Load profile picture, bio, name, counts, and posts
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(userCredential::class.java)
                user?.let {
                    // Load profile picture into CircleImageView
                    if (it.profileImage?.isNotEmpty() == true) {
                        try {
                            val decodedImage = Base64.decode(it.profileImage, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
                            findViewById<CircleImageView>(R.id.ProfilePic).setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Toast.makeText(this@ProfilePage, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                        }
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

                    // Load and sort posts
                    val postsRef = FirebaseDatabase.getInstance().getReference("Posts")
                    val postList = mutableListOf<Post>()

                    // Add 7 dummy posts with imageUrls (list of Base64 strings)
                    val dummyImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" // 1x1 red pixel
                    postList.add(Post(
                        postId = "dummy1",
                        imageUrls = listOf(dummyImageBase64),
                        caption = "Dummy Post 1",
                        timestamp = System.currentTimeMillis() - 1000
                    ))
                    postList.add(Post(
                        postId = "dummy2",
                        imageUrls = listOf(dummyImageBase64),
                        caption = "Dummy Post 2",
                        timestamp = System.currentTimeMillis() - 2000
                    ))
                    postList.add(Post(
                        postId = "dummy3",
                        imageUrls = listOf(dummyImageBase64),
                        caption = "Dummy Post 3",
                        timestamp = System.currentTimeMillis() - 3000
                    ))
                    postList.add(Post(
                        postId = "dummy4",
                        imageUrls = listOf(dummyImageBase64),
                        caption = "Dummy Post 4",
                        timestamp = System.currentTimeMillis() - 4000
                    ))
                    postList.add(Post(
                        postId = "dummy5",
                        imageUrls = listOf(dummyImageBase64),
                        caption = "Dummy Post 5",
                        timestamp = System.currentTimeMillis() - 5000
                    ))
                    postList.add(Post(
                        postId = "dummy6",
                        imageUrls = listOf(dummyImageBase64),
                        caption = "Dummy Post 6",
                        timestamp = System.currentTimeMillis() - 6000
                    ))
                    postList.add(Post(
                        postId = "dummy7",
                        imageUrls = listOf(dummyImageBase64),
                        caption = "Dummy Post 7",
                        timestamp = System.currentTimeMillis() - 7000
                    ))

                    // Fetch real posts from Firebase
                    it.posts?.forEach { postId ->
                        postsRef.child(postId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(postSnapshot: DataSnapshot) {
                                val post = postSnapshot.getValue(Post::class.java)
                                post?.let { p ->
                                    postList.add(p)
                                    // Sort by timestamp descending (latest first)
                                    postList.sortByDescending { it.timestamp }
                                    postAdapter.submitPosts(postList)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@ProfilePage, "Failed to load post: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    // If no real posts, submit dummy posts immediately
                    if (it.posts.isNullOrEmpty()) {
                        postList.sortByDescending { it.timestamp }
                        postAdapter.submitPosts(postList)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfilePage, "Failed to load profile data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Rest of the button listeners remain unchanged...
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
        newPost as ImageButton
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