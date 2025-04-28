package com.muhammadomer.i220921

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LocalUser(
    val userId: Long,
    val name: String,
    val username: String,
    val phoneNumber: String,
    val email: String,
    val bio: String?,
    val profileImage: String?,
    val postsCount: Int,
    val followersCount: Int,
    val followingCount: Int
)

data class LocalPost(
    val postId: String,
    val userId: Long,
    val imageUrls: String,
    val caption: String?,
    val timestamp: Long,
    val likes: String
)

data class LocalStory(
    val storyId: String,
    val userId: Long,
    val imageUrl: String,
    val timestamp: Long
)

data class LocalRecentSearch(
    val id: String,
    val userId: Long,
    val searchedUserId: Long,
    val username: String,
    val timestamp: Long
)

data class LocalChat(
    val chatId: String,
    val userId: Long,
    val otherUserId: Long,
    val lastMessage: String?,
    val timestamp: Long
)

data class QueuedAction(
    val actionId: String,
    val actionType: String,
    val payload: String,
    val createdAt: Long
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ConnectMe.db"
        private const val DATABASE_VERSION = 4
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                user_id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                username TEXT NOT NULL,
                phone_number TEXT NOT NULL,
                email TEXT NOT NULL,
                bio TEXT,
                profile_image TEXT,
                posts_count INTEGER,
                followers_count INTEGER,
                following_count INTEGER
            )
        """)
        db.execSQL("""
            CREATE TABLE posts (
                post_id TEXT PRIMARY KEY,
                user_id INTEGER,
                image_urls TEXT,
                caption TEXT,
                timestamp INTEGER,
                likes TEXT,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
        """)
        db.execSQL("""
            CREATE TABLE stories (
                story_id TEXT PRIMARY KEY,
                user_id INTEGER,
                image_url TEXT,
                timestamp INTEGER,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
        """)
        db.execSQL("""
            CREATE TABLE recent_searches (
                id TEXT PRIMARY KEY,
                user_id INTEGER,
                searched_user_id INTEGER,
                username TEXT,
                timestamp INTEGER,
                FOREIGN KEY (user_id) REFERENCES users(user_id),
                FOREIGN KEY (searched_user_id) REFERENCES users(user_id)
            )
        """)
        db.execSQL("""
            CREATE TABLE chats (
                chat_id TEXT PRIMARY KEY,
                user_id INTEGER,
                other_user_id INTEGER,
                last_message TEXT,
                timestamp INTEGER,
                FOREIGN KEY (user_id) REFERENCES users(user_id),
                FOREIGN KEY (other_user_id) REFERENCES users(user_id)
            )
        """)
        db.execSQL("""
            CREATE TABLE queued_actions (
                action_id TEXT PRIMARY KEY,
                action_type TEXT,
                payload TEXT,
                created_at INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE recent_searches (id TEXT PRIMARY KEY, user_id INTEGER, searched_user_id INTEGER, username TEXT, timestamp INTEGER, FOREIGN KEY (user_id) REFERENCES users(user_id), FOREIGN KEY (searched_user_id) REFERENCES users(user_id))")
            db.execSQL("CREATE TABLE queued_actions (action_id TEXT PRIMARY KEY, action_type TEXT, payload TEXT, created_at INTEGER)")
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE stories (story_id TEXT PRIMARY KEY, user_id INTEGER, image_url TEXT, timestamp INTEGER, FOREIGN KEY (user_id) REFERENCES users(user_id))")
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE chats (chat_id TEXT PRIMARY KEY, user_id INTEGER, other_user_id INTEGER, last_message TEXT, timestamp INTEGER, FOREIGN KEY (user_id) REFERENCES users(user_id), FOREIGN KEY (other_user_id) REFERENCES users(user_id))")
        }
    }

    fun insertOrUpdateUser(user: LocalUser) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", user.userId)
            put("name", user.name)
            put("username", user.username)
            put("phone_number", user.phoneNumber)
            put("email", user.email)
            put("bio", user.bio)
            put("profile_image", user.profileImage)
            put("posts_count", user.postsCount)
            put("followers_count", user.followersCount)
            put("following_count", user.followingCount)
        }
        db.replace("users", null, values)
        db.close()
    }

    fun getUserById(userId: Long): LocalUser? {
        val db = readableDatabase
        val cursor = db.query("users", null, "user_id = ?", arrayOf(userId.toString()), null, null, null)
        return if (cursor.moveToFirst()) {
            LocalUser(
                userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                bio = cursor.getString(cursor.getColumnIndexOrThrow("bio")),
                profileImage = cursor.getString(cursor.getColumnIndexOrThrow("profile_image")),
                postsCount = cursor.getInt(cursor.getColumnIndexOrThrow("posts_count")),
                followersCount = cursor.getInt(cursor.getColumnIndexOrThrow("followers_count")),
                followingCount = cursor.getInt(cursor.getColumnIndexOrThrow("following_count"))
            ).also { cursor.close() }
        } else {
            cursor.close()
            null
        }
    }

    fun getUsersByUsername(query: String): List<LocalUser> {
        val db = readableDatabase
        val cursor = db.query("users", null, "username LIKE ?", arrayOf("%$query%"), null, null, "username ASC")
        val users = mutableListOf<LocalUser>()
        while (cursor.moveToNext()) {
            users.add(
                LocalUser(
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    bio = cursor.getString(cursor.getColumnIndexOrThrow("bio")),
                    profileImage = cursor.getString(cursor.getColumnIndexOrThrow("profile_image")),
                    postsCount = cursor.getInt(cursor.getColumnIndexOrThrow("posts_count")),
                    followersCount = cursor.getInt(cursor.getColumnIndexOrThrow("followers_count")),
                    followingCount = cursor.getInt(cursor.getColumnIndexOrThrow("following_count"))
                )
            )
        }
        cursor.close()
        return users
    }

    fun insertOrUpdatePost(post: LocalPost) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("post_id", post.postId)
            put("user_id", post.userId)
            put("image_urls", post.imageUrls)
            put("caption", post.caption)
            put("timestamp", post.timestamp)
            put("likes", post.likes)
        }
        db.replace("posts", null, values)
        db.close()
    }

    fun getPostsByUserIds(userIds: List<Long>): List<LocalPost> {
        val db = readableDatabase
        val userIdsString = userIds.joinToString(",")
        val cursor = db.query("posts", null, "user_id IN ($userIdsString)", null, null, null, "timestamp DESC")
        val posts = mutableListOf<LocalPost>()
        while (cursor.moveToNext()) {
            posts.add(
                LocalPost(
                    postId = cursor.getString(cursor.getColumnIndexOrThrow("post_id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    imageUrls = cursor.getString(cursor.getColumnIndexOrThrow("image_urls")),
                    caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    likes = cursor.getString(cursor.getColumnIndexOrThrow("likes"))
                )
            )
        }
        cursor.close()
        return posts
    }

    fun insertOrUpdateStory(story: LocalStory) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("story_id", story.storyId)
            put("user_id", story.userId)
            put("image_url", story.imageUrl)
            put("timestamp", story.timestamp)
        }
        db.replace("stories", null, values)
        db.close()
    }

    fun getStoriesByUserIds(userIds: List<Long>): List<LocalStory> {
        val db = readableDatabase
        val userIdsString = userIds.joinToString(",")
        val cursor = db.query("stories", null, "user_id IN ($userIdsString)", null, null, null, "timestamp DESC")
        val stories = mutableListOf<LocalStory>()
        while (cursor.moveToNext()) {
            stories.add(
                LocalStory(
                    storyId = cursor.getString(cursor.getColumnIndexOrThrow("story_id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return stories
    }

    fun insertOrUpdateRecentSearch(search: LocalRecentSearch) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", search.id)
            put("user_id", search.userId)
            put("searched_user_id", search.searchedUserId)
            put("username", search.username)
            put("timestamp", search.timestamp)
        }
        db.replace("recent_searches", null, values)
        db.close()
    }

    fun getRecentSearches(userId: Long): List<LocalRecentSearch> {
        val db = readableDatabase
        val cursor = db.query("recent_searches", null, "user_id = ?", arrayOf(userId.toString()), null, null, "timestamp DESC")
        val searches = mutableListOf<LocalRecentSearch>()
        while (cursor.moveToNext()) {
            searches.add(
                LocalRecentSearch(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    searchedUserId = cursor.getLong(cursor.getColumnIndexOrThrow("searched_user_id")),
                    username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return searches
    }

    fun deleteRecentSearch(searchId: String) {
        val db = writableDatabase
        db.delete("recent_searches", "id = ?", arrayOf(searchId))
        db.close()
    }

    fun insertOrUpdateChat(chat: LocalChat) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("chat_id", chat.chatId)
            put("user_id", chat.userId)
            put("other_user_id", chat.otherUserId)
            put("last_message", chat.lastMessage)
            put("timestamp", chat.timestamp)
        }
        db.replace("chats", null, values)
        db.close()
    }

    fun getChatsByUserId(userId: Long): List<LocalChat> {
        val db = readableDatabase
        val cursor = db.query("chats", null, "user_id = ?", arrayOf(userId.toString()), null, null, "timestamp DESC")
        val chats = mutableListOf<LocalChat>()
        while (cursor.moveToNext()) {
            chats.add(
                LocalChat(
                    chatId = cursor.getString(cursor.getColumnIndexOrThrow("chat_id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    otherUserId = cursor.getLong(cursor.getColumnIndexOrThrow("other_user_id")),
                    lastMessage = cursor.getString(cursor.getColumnIndexOrThrow("last_message")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return chats
    }

    fun queueAction(action: QueuedAction) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("action_id", action.actionId)
            put("action_type", action.actionType)
            put("payload", action.payload)
            put("created_at", action.createdAt)
        }
        db.insert("queued_actions", null, values)
        db.close()
    }

    fun getQueuedActions(): List<QueuedAction> {
        val db = readableDatabase
        val cursor = db.query("queued_actions", null, null, null, null, null, null)
        val actions = mutableListOf<QueuedAction>()
        while (cursor.moveToNext()) {
            actions.add(
                QueuedAction(
                    actionId = cursor.getString(cursor.getColumnIndexOrThrow("action_id")),
                    actionType = cursor.getString(cursor.getColumnIndexOrThrow("action_type")),
                    payload = cursor.getString(cursor.getColumnIndexOrThrow("payload")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            )
        }
        cursor.close()
        return actions
    }

    fun clearUserData() {
        val db = writableDatabase
        db.delete("users", null, null)
        db.delete("posts", null, null)
        db.delete("stories", null, null)
        db.delete("recent_searches", null, null)
        db.delete("chats", null, null)
        db.delete("queued_actions", null, null)
        db.close()
    }
}