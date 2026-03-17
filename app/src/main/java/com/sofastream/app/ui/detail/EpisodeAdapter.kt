package com.sofastream.app.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sofastream.app.R
import com.sofastream.app.data.model.MediaItem
import com.sofastream.app.databinding.ItemEpisodeNetflixBinding

class EpisodeAdapter(
    private val onEpisodeClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, EpisodeAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEpisodeNetflixBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemEpisodeNetflixBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            val context = binding.root.context
            
            // Format: "IndexNumber. Name"
            val title = if (item.episodeNumber != null) {
                "${item.episodeNumber}. ${item.title}"
            } else {
                item.title
            }
            binding.tvEpisodeTitle.text = title
            binding.tvEpisodeRuntime.text = item.getRuntime()
            binding.tvEpisodeOverview.text = item.overview

            val thumbUrl = item.thumbUrl ?: item.backdropUrl ?: item.posterUrl
            Glide.with(context)
                .load(thumbUrl)
                .placeholder(R.color.surface_dark)
                .error(R.color.surface_dark)
                .centerCrop()
                .into(binding.ivEpisodeThumb)

            if (item.playedPercentage > 0 && !item.isPlayed) {
                binding.episodeProgressBar.visibility = View.VISIBLE
                binding.episodeProgressBar.progress = item.playedPercentage.toInt()
            } else {
                binding.episodeProgressBar.visibility = View.GONE
            }

            binding.root.setOnClickListener { onEpisodeClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) = oldItem == newItem
    }
}
