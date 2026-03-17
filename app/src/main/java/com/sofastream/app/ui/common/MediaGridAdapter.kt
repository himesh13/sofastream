package com.sofastream.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sofastream.app.R
import com.sofastream.app.data.model.MediaItem
import com.sofastream.app.databinding.ItemMediaGridBinding

class MediaGridAdapter(
    private val onItemClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, MediaGridAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMediaGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            binding.tvTitle.text = item.title

            val imageUrl = item.posterUrl ?: item.thumbUrl ?: item.backdropUrl
            if (imageUrl != null) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(binding.ivPoster)
            } else {
                binding.ivPoster.setImageResource(R.drawable.ic_placeholder)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) = oldItem == newItem
    }
}
