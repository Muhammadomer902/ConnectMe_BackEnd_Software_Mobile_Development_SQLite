package com.muhammadomer.i220921

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.net.URL

class ProfilePostAdapter : RecyclerView.Adapter<ProfilePostAdapter.PostViewHolder>() {
    private val postList = mutableListOf<Post>()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Submit a new list of posts and notify the adapter of the change
    fun submitPosts(newPosts: List<Post>) {
        postList.clear()
        postList.addAll(newPosts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.profile_post_image, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.bind(post)
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.clear() // Clear the ImageView to prevent memory leaks
    }

    override fun getItemCount(): Int = postList.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private var currentBitmap: Bitmap? = null // Track the current Bitmap for recycling

        fun bind(post: Post) {
            // Clear the previous Bitmap if it exists
            currentBitmap?.recycle()
            currentBitmap = null
            postImage.setImageBitmap(null)

            // Get the first image from imageUrls (if available)
            val firstImageUrl = post.imageUrls?.firstOrNull()
            if (!firstImageUrl.isNullOrEmpty()) {
                // Load image from URL in a background thread
                Thread {
                    try {
                        val url = URL(firstImageUrl)
                        val bitmap = BitmapFactory.decodeStream(url.openStream())
                        // Resize the Bitmap to reduce memory usage
                        val resizedBitmap = resizeBitmap(bitmap, 300, 300)
                        currentBitmap = resizedBitmap
                        mainHandler.post {
                            postImage.setImageBitmap(resizedBitmap)
                        }
                    } catch (e: Exception) {
                        mainHandler.post {
                            postImage.setImageResource(android.R.drawable.ic_menu_gallery) // Fallback image
                        }
                    }
                }.start()
            } else {
                // No image available
                postImage.setImageResource(android.R.drawable.ic_menu_gallery) // Fallback image
            }
        }

        // Clear the ImageView and recycle the Bitmap
        fun clear() {
            currentBitmap?.recycle()
            currentBitmap = null
            postImage.setImageBitmap(null)
        }

        // Utility function to resize a Bitmap
        private fun resizeBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
            val width = source.width
            val height = source.height
            val aspectRatio = width.toFloat() / height.toFloat()

            var newWidth = maxWidth
            var newHeight = maxHeight
            if (width > height) {
                newHeight = (maxWidth / aspectRatio).toInt()
            } else {
                newWidth = (maxHeight * aspectRatio).toInt()
            }

            return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
        }
    }
}