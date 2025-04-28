package com.muhammadomer.i220921

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentSearchesAdapter(
    private val recentSearches: MutableList<RecentSearch>,
    private val onRemoveClick: (RecentSearch) -> Unit,
    private val onClick: (RecentSearch) -> Unit
) : RecyclerView.Adapter<RecentSearchesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.searchUsername)
        val crossIcon: ImageView = itemView.findViewById(R.id.crossIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_searches, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val search = recentSearches[position]
        holder.usernameTextView.text = search.username
        holder.crossIcon.setOnClickListener {
            onRemoveClick(search)
        }
        holder.itemView.setOnClickListener {
            onClick(search)
        }
    }

    override fun getItemCount(): Int = recentSearches.size
}