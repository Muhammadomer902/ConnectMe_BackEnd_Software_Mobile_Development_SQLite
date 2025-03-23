package com.muhammadomer.i220921

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProfilePostAdapter : RecyclerView.Adapter<ProfilePostAdapter.PostViewHolder>() {
    private var posts: List<Post> = emptyList()

    fun submitPosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.profile_post_image, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        if (post.imageUrl?.isNotEmpty() == true) {
            val decodedImage = Base64.decode(post.imageUrl, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
            holder.imageView.setImageBitmap(bitmap)
        }
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.postImage)
    }
}