package com.muhammadomer.i220921

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FinalizePostAdapter(private val imagePaths: List<String>) : RecyclerView.Adapter<FinalizePostAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.finalize_post_image, parent, false)
        Log.d("ImageAdapter", "Created ViewHolder for viewType: $viewType")
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = imagePaths[position]
        Log.d("ImageAdapter", "Binding position $position with image path: $imagePath")

        // Check if the file exists
        val file = File(imagePath)
        if (file.exists()) {
            Log.d("ImageAdapter", "File exists at position $position: $imagePath")
        } else {
            Log.w("ImageAdapter", "File does NOT exist at position $position: $imagePath")
        }

        // Load the bitmap
        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap != null) {
            Log.d("ImageAdapter", "Bitmap loaded successfully for position $position, size: ${bitmap.byteCount} bytes")
            holder.imageView.setImageBitmap(bitmap)
            holder.imageView.visibility = View.VISIBLE
        } else {
            Log.w("ImageAdapter", "Failed to load bitmap for position $position")
            holder.imageView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // Placeholder for debugging
            holder.imageView.visibility = View.VISIBLE // Keep visible to debug
        }
    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        Log.d("ImageAdapter", "Recycled ViewHolder at position: ${holder.adapterPosition}")
        holder.imageView.setImageBitmap(null)
    }

    override fun getItemCount(): Int {
        Log.d("ImageAdapter", "Item count: ${imagePaths.size}")
        return imagePaths.size
    }
}