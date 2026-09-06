package com.tasirin.browser.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tasirin.browser.R
import com.tasirin.browser.data.Bookmark

class BookmarkAdapter(
    private val bookmarks: MutableList<Bookmark>,
    private val onClick: (Bookmark) -> Unit,
    private val onRemove: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.bmTitle)
        val url: TextView = view.findViewById(R.id.bmUrl)
        val remove: ImageButton = view.findViewById(R.id.bmRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val bm = bookmarks[position]
        holder.title.text = bm.title
        holder.url.text = bm.url
        holder.itemView.setOnClickListener { onClick(bm) }
        holder.remove.setOnClickListener { onRemove(bm) }
    }

    override fun getItemCount() = bookmarks.size

    fun update(newList: List<Bookmark>) {
        bookmarks.clear()
        bookmarks.addAll(newList)
        notifyDataSetChanged()
    }
}
