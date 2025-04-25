package com.muhammadomer.i220921

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ConnectMe.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PHONE_NUMBER = "phone_number"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_BIO = "bio"
        private const val COLUMN_PROFILE_IMAGE = "profile_image"
        private const val COLUMN_POSTS_COUNT = "posts_count"
        private const val COLUMN_FOLLOWERS_COUNT = "followers_count"
        private const val COLUMN_FOLLOWING_COUNT = "following_count"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_USERNAME TEXT NOT NULL UNIQUE,
                $COLUMN_PHONE_NUMBER TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL UNIQUE,
                $COLUMN_BIO TEXT,
                $COLUMN_PROFILE_IMAGE TEXT,
                $COLUMN_POSTS_COUNT INTEGER DEFAULT 0,
                $COLUMN_FOLLOWERS_COUNT INTEGER DEFAULT 0,
                $COLUMN_FOLLOWING_COUNT INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun insertOrUpdateUser(user: LocalUser) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, user.userId)
            put(COLUMN_NAME, user.name)
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_PHONE_NUMBER, user.phoneNumber)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_BIO, user.bio)
            put(COLUMN_PROFILE_IMAGE, user.profileImage)
            put(COLUMN_POSTS_COUNT, user.postsCount)
            put(COLUMN_FOLLOWERS_COUNT, user.followersCount)
            put(COLUMN_FOLLOWING_COUNT, user.followingCount)
        }
        db.replace(TABLE_USERS, null, values)
        db.close()
    }

    fun getUserById(userId: Long): LocalUser? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            val user = LocalUser(
                userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                bio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIO)),
                profileImage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE)),
                postsCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_POSTS_COUNT)),
                followersCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWERS_COUNT)),
                followingCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWING_COUNT))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun clearUserData() {
        val db = writableDatabase
        db.delete(TABLE_USERS, null, null)
        db.close()
    }
}