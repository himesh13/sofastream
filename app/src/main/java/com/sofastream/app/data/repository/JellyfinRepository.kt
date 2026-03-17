package com.sofastream.app.data.repository

import com.sofastream.app.api.*
import com.sofastream.app.data.model.*

class JellyfinRepository(
    private val api: JellyfinApi,
    private val baseUrl: String,
    private val token: String,
    private val userId: String
) {

    private fun getImageUrl(itemId: String, imageType: String = "Primary", tag: String? = null): String {
        val base = "${baseUrl.trimEnd('/')}/Items/$itemId/Images/$imageType"
        return if (tag != null) "$base?tag=$tag" else base
    }

    private fun getBackdropUrl(itemId: String, tag: String? = null): String {
        val base = "${baseUrl.trimEnd('/')}/Items/$itemId/Images/Backdrop"
        return if (tag != null) "$base?tag=$tag" else base
    }

    private fun JellyfinItem.toMediaItem(): MediaItem {
        val posterTag = ImageTags?.get("Primary")
        val backdropTag = BackdropImageTags?.firstOrNull()
        val thumbTag = ImageTags?.get("Thumb")

        return MediaItem(
            id = Id,
            title = Name,
            overview = Overview,
            type = when (Type) {
                "Movie" -> MediaType.MOVIE
                "Series" -> MediaType.SERIES
                "Episode" -> MediaType.EPISODE
                else -> MediaType.MOVIE
            },
            year = ProductionYear,
            rating = CommunityRating,
            contentRating = OfficialRating,
            runtimeTicks = RunTimeTicks,
            posterUrl = if (posterTag != null) getImageUrl(Id, "Primary", posterTag) else null,
            backdropUrl = if (backdropTag != null) getBackdropUrl(Id, backdropTag) else null,
            thumbUrl = if (thumbTag != null) getImageUrl(Id, "Thumb", thumbTag) else null,
            genres = Genres,
            studios = Studios?.map { it.Name },
            seriesId = SeriesId,
            seriesName = SeriesName,
            seasonId = SeasonId,
            episodeNumber = IndexNumber,
            seasonNumber = ParentIndexNumber,
            tagline = Taglines?.firstOrNull(),
            isFavorite = UserData?.IsFavorite ?: false,
            playbackPositionTicks = UserData?.PlaybackPositionTicks ?: 0,
            playedPercentage = UserData?.PlayedPercentage ?: 0.0,
            isPlayed = UserData?.Played ?: false,
            cast = People?.map { CastMember(it.Name, it.Role, it.Type) }
        )
    }

    suspend fun getLatestMedia(): Result<List<MediaItem>> {
        return try {
            val response = api.getLatestMedia(userId, token)
            if (response.isSuccessful) {
                Result.success(response.body()?.map { it.toMediaItem() } ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovies(startIndex: Int = 0, limit: Int = 50): Result<Pair<List<MediaItem>, Int>> {
        return try {
            val response = api.getItems(
                userId = userId,
                token = token,
                includeItemTypes = "Movie",
                startIndex = startIndex,
                limit = limit
            )
            if (response.isSuccessful) {
                val body = response.body()
                Result.success(Pair(body?.Items?.map { it.toMediaItem() } ?: emptyList(), body?.TotalRecordCount ?: 0))
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeries(startIndex: Int = 0, limit: Int = 50): Result<Pair<List<MediaItem>, Int>> {
        return try {
            val response = api.getItems(
                userId = userId,
                token = token,
                includeItemTypes = "Series",
                startIndex = startIndex,
                limit = limit
            )
            if (response.isSuccessful) {
                val body = response.body()
                Result.success(Pair(body?.Items?.map { it.toMediaItem() } ?: emptyList(), body?.TotalRecordCount ?: 0))
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemDetails(itemId: String): Result<MediaItem> {
        return try {
            val response = api.getItemDetails(userId, itemId, token)
            if (response.isSuccessful) {
                val item = response.body() ?: return Result.failure(Exception("No data"))
                Result.success(item.toMediaItem())
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeasons(seriesId: String): Result<List<Season>> {
        return try {
            val response = api.getSeasons(seriesId, token, userId)
            if (response.isSuccessful) {
                val seasons = response.body()?.Items?.map { item ->
                    val posterTag = item.ImageTags?.get("Primary")
                    Season(
                        id = item.Id,
                        name = item.Name,
                        seasonNumber = item.IndexNumber ?: 0,
                        episodeCount = null,
                        overview = item.Overview,
                        posterUrl = if (posterTag != null) getImageUrl(item.Id, "Primary", posterTag) else null
                    )
                } ?: emptyList()
                Result.success(seasons)
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEpisodes(seriesId: String, seasonId: String): Result<List<MediaItem>> {
        return try {
            val response = api.getEpisodes(seriesId, token, seasonId, userId)
            if (response.isSuccessful) {
                Result.success(response.body()?.Items?.map { it.toMediaItem() } ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchItems(query: String): Result<List<MediaItem>> {
        return try {
            val response = api.searchItems(userId, token, query)
            if (response.isSuccessful) {
                Result.success(response.body()?.Items?.map { it.toMediaItem() } ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaybackInfo(itemId: String): Result<PlaybackInfo> {
        return try {
            val response = api.getPlaybackInfo(userId, itemId, token, PlaybackInfoRequest(UserId = userId))
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(Exception("No data"))
                val mediaSource = body.MediaSources.firstOrNull()
                    ?: return Result.failure(Exception("No media sources"))

                val streamUrl = when {
                    mediaSource.SupportsDirectStream -> {
                        "${baseUrl.trimEnd('/')}/Videos/$itemId/stream?Static=true&mediaSourceId=${mediaSource.Id}&api_key=$token"
                    }
                    mediaSource.SupportsTranscoding && mediaSource.TranscodingUrl != null -> {
                        "${baseUrl.trimEnd('/')}${mediaSource.TranscodingUrl}"
                    }
                    else -> {
                        "${baseUrl.trimEnd('/')}/Videos/$itemId/stream?mediaSourceId=${mediaSource.Id}&api_key=$token"
                    }
                }

                val itemDetails = api.getItemDetails(userId, itemId, token).body()

                val playbackInfo = PlaybackInfo(
                    mediaItemId = itemId,
                    streamUrl = streamUrl,
                    mediaSourceId = mediaSource.Id,
                    playSessionId = body.PlaySessionId,
                    startPositionTicks = itemDetails?.UserData?.PlaybackPositionTicks ?: 0,
                    title = itemDetails?.Name ?: "",
                    seriesName = itemDetails?.SeriesName,
                    seasonNumber = itemDetails?.ParentIndexNumber,
                    episodeNumber = itemDetails?.IndexNumber,
                    backdropUrl = itemDetails?.BackdropImageTags?.firstOrNull()?.let {
                        getBackdropUrl(itemId, it)
                    }
                )
                Result.success(playbackInfo)
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportPlaybackStart(itemId: String, mediaSourceId: String?, playSessionId: String?) {
        try {
            api.reportPlaybackStart(token, PlaybackStartInfo(
                ItemId = itemId,
                MediaSourceId = mediaSourceId,
                PlaySessionId = playSessionId
            ))
        } catch (_: Exception) {}
    }

    suspend fun reportPlaybackStop(itemId: String, mediaSourceId: String?, playSessionId: String?, positionTicks: Long) {
        try {
            api.reportPlaybackStop(token, PlaybackStopInfo(
                ItemId = itemId,
                MediaSourceId = mediaSourceId,
                PlaySessionId = playSessionId,
                PositionTicks = positionTicks
            ))
        } catch (_: Exception) {}
    }

    suspend fun reportPlaybackProgress(itemId: String, mediaSourceId: String?, playSessionId: String?, positionTicks: Long, isPaused: Boolean) {
        try {
            api.reportPlaybackProgress(token, PlaybackProgressInfo(
                ItemId = itemId,
                MediaSourceId = mediaSourceId,
                PlaySessionId = playSessionId,
                PositionTicks = positionTicks,
                IsPaused = isPaused
            ))
        } catch (_: Exception) {}
    }
}
