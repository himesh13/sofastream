package com.sofastream.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    val id: String,
    val title: String,
    val overview: String?,
    val type: MediaType,
    val year: Int?,
    val rating: Float?,
    val contentRating: String?,
    val runtimeTicks: Long?,
    val backdropUrl: String?,
    val posterUrl: String?,
    val thumbUrl: String?,
    val genres: List<String>?,
    val studios: List<String>?,
    val seriesId: String?,
    val seriesName: String?,
    val seasonId: String?,
    val episodeNumber: Int?,
    val seasonNumber: Int?,
    val tagline: String?,
    val isFavorite: Boolean = false,
    val playbackPositionTicks: Long = 0,
    val playedPercentage: Double = 0.0,
    val isPlayed: Boolean = false,
    val cast: List<CastMember>?
) : Parcelable {

    fun getRuntime(): String {
        if (runtimeTicks == null) return ""
        val totalMinutes = runtimeTicks / 600_000_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

@Parcelize
data class CastMember(
    val name: String,
    val role: String?,
    val type: String
) : Parcelable

enum class MediaType {
    MOVIE, SERIES, EPISODE, COLLECTION
}

@Parcelize
data class Season(
    val id: String,
    val name: String,
    val seasonNumber: Int,
    val episodeCount: Int?,
    val overview: String?,
    val posterUrl: String?
) : Parcelable

@Parcelize
data class PlaybackInfo(
    val mediaItemId: String,
    val streamUrl: String,
    val mediaSourceId: String?,
    val playSessionId: String?,
    val startPositionTicks: Long = 0,
    val title: String,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val backdropUrl: String? = null
) : Parcelable
