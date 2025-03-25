package com.muhammadomer.i220921

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class HomePostImageAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<HomePostImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.postImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_post_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        try {
            val decodedImage = Base64.decode(imageUrl, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
            holder.imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // Placeholder on error
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}