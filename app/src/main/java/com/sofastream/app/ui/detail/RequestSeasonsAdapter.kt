package com.sofastream.app.ui.detail

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sofastream.app.api.JellyseerrSeason
import com.sofastream.app.databinding.ItemRequestSeasonBinding

class RequestSeasonsAdapter(
    private val seasons: List<JellyseerrSeason>,
    private val onSelectionChanged: (List<Int>) -> Unit
) : RecyclerView.Adapter<RequestSeasonsAdapter.ViewHolder>() {

    private val selectedSeasons = mutableSetOf<Int>()

    init {
        // Pre-select seasons that are already available or requested (status >= 2)
        seasons.forEach { season ->
            if (season.status != null && season.status >= 2) {
                selectedSeasons.add(season.seasonNumber)
            }
        }
        onSelectionChanged(selectedSeasons.toList())
    }

    inner class ViewHolder(val binding: ItemRequestSeasonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRequestSeasonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val season = seasons[position]
        holder.binding.tvSeasonName.text = season.name ?: "Season ${season.seasonNumber}"
        holder.binding.tvEpisodeCount.text = "${season.episodeCount}"
        
        val isChecked = selectedSeasons.contains(season.seasonNumber)
        
        // Ensure the checkbox reflects the data state
        holder.binding.cbSeason.setOnCheckedChangeListener(null)
        holder.binding.cbSeason.isChecked = isChecked

        // Jellyseerr status: 1=Unknown, 2=Pending, 3=Processing, 4=Partially Available, 5=Available
        val statusText: String
        val statusColor: Int
        val isSelectable: Boolean
        
        when (season.status) {
            5 -> {
                statusText = "Available"
                statusColor = Color.parseColor("#10B981") // Green
                isSelectable = false
            }
            3, 2 -> {
                statusText = if (season.status == 3) "Processing" else "Pending"
                statusColor = Color.parseColor("#F59E0B") // Amber
                isSelectable = false
            }
            4 -> {
                statusText = "Partially Available"
                statusColor = Color.parseColor("#3B82F6") // Blue
                isSelectable = true
            }
            else -> {
                statusText = "Not Requested"
                statusColor = Color.parseColor("#4F46E5") // Indigo
                isSelectable = true
            }
        }

        holder.binding.tvStatus.text = statusText
        holder.binding.tvStatus.background.setTint(statusColor)
        
        // Disable interaction if already available/processing
        holder.binding.cbSeason.isEnabled = isSelectable
        holder.itemView.isEnabled = isSelectable
        holder.binding.root.alpha = if (isSelectable) 1.0f else 0.6f

        val toggleAction = {
            if (isSelectable) {
                if (selectedSeasons.contains(season.seasonNumber)) {
                    selectedSeasons.remove(season.seasonNumber)
                } else {
                    selectedSeasons.add(season.seasonNumber)
                }
                onSelectionChanged(selectedSeasons.toList())
                notifyItemChanged(position)
            }
        }

        holder.itemView.setOnClickListener { toggleAction() }
        holder.binding.cbSeason.setOnClickListener { toggleAction() }
    }

    override fun getItemCount() = seasons.size
}
