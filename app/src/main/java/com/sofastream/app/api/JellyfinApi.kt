package com.sofastream.app.api

import retrofit2.Response
import retrofit2.http.*

// Jellyfin API endpoints
interface JellyfinApi {

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Header("X-Emby-Authorization") authorization: String,
        @Body body: JellyfinAuthRequest
    ): Response<JellyfinAuthResponse>

    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Path("userId") userId: String,
        @Header("X-Emby-Token") token: String
    ): Response<JellyfinItemsResponse>

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestMedia(
        @Path("userId") userId: String,
        @Header("X-Emby-Token") token: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,MediaSourceCount",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb"
    ): Response<List<JellyfinItem>>

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Token") token: String,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,MediaSourceCount",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 50
    ): Response<JellyfinItemsResponse>

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemDetails(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Token") token: String,
        @Query("Fields") fields: String = "Overview,Genres,Studios,People,MediaSources,MediaStreams,ExternalUrls,CommunityRating,OfficialRating,RunTimeTicks,Taglines"
    ): Response<JellyfinItem>

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Path("seriesId") seriesId: String,
        @Header("X-Emby-Token") token: String,
        @Query("userId") userId: String,
        @Query("Fields") fields: String = "BasicSyncInfo"
    ): Response<JellyfinItemsResponse>

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Path("seriesId") seriesId: String,
        @Header("X-Emby-Token") token: String,
        @Query("seasonId") seasonId: String,
        @Query("userId") userId: String,
        @Query("Fields") fields: String = "Overview,MediaSources"
    ): Response<JellyfinItemsResponse>

    @GET("Users/{userId}/Items")
    suspend fun searchItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Token") token: String,
        @Query("searchTerm") searchTerm: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Series",
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("Limit") limit: Int = 50
    ): Response<JellyfinItemsResponse>

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(
        @Header("X-Emby-Token") token: String,
        @Body body: PlaybackStartInfo
    ): Response<Unit>

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStop(
        @Header("X-Emby-Token") token: String,
        @Body body: PlaybackStopInfo
    ): Response<Unit>

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Header("X-Emby-Token") token: String,
        @Body body: PlaybackProgressInfo
    ): Response<Unit>

    @POST("Users/{userId}/Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Token") token: String,
        @Body body: PlaybackInfoRequest
    ): Response<JellyfinPlaybackInfo>
}

data class JellyfinAuthRequest(
    val Username: String,
    val Pw: String
)

data class JellyfinAuthResponse(
    val User: JellyfinUser,
    val AccessToken: String,
    val ServerId: String
)

data class JellyfinUser(
    val Id: String,
    val Name: String
)

data class JellyfinItemsResponse(
    val Items: List<JellyfinItem>,
    val TotalRecordCount: Int,
    val StartIndex: Int
)

data class JellyfinItem(
    val Id: String,
    val Name: String,
    val Type: String,
    val Overview: String?,
    val ProductionYear: Int?,
    val CommunityRating: Float?,
    val OfficialRating: String?,
    val RunTimeTicks: Long?,
    val ImageTags: Map<String, String>?,
    val BackdropImageTags: List<String>?,
    val Genres: List<String>?,
    val Studios: List<JellyfinStudio>?,
    val MediaSources: List<JellyfinMediaSource>?,
    val SeriesName: String?,
    val SeriesId: String?,
    val SeasonId: String?,
    val IndexNumber: Int?,
    val ParentIndexNumber: Int?,
    val UserData: JellyfinUserData?,
    val Taglines: List<String>?,
    val People: List<JellyfinPerson>?
)

data class JellyfinStudio(val Name: String)

data class JellyfinMediaSource(
    val Id: String,
    val Name: String,
    val Path: String?,
    val Protocol: String?,
    val TranscodingUrl: String?,
    val DirectStreamUrl: String?,
    val SupportsDirectStream: Boolean,
    val SupportsTranscoding: Boolean,
    val MediaStreams: List<JellyfinMediaStream>?
)

data class JellyfinMediaStream(
    val Type: String,
    val Codec: String?,
    val Language: String?,
    val DisplayTitle: String?,
    val Index: Int,
    val IsDefault: Boolean,
    val IsForced: Boolean
)

data class JellyfinUserData(
    val PlaybackPositionTicks: Long,
    val PlayCount: Int,
    val IsFavorite: Boolean,
    val Played: Boolean,
    val PlayedPercentage: Double?
)

data class JellyfinPerson(
    val Name: String,
    val Type: String,
    val Role: String?
)

data class JellyfinPlaybackInfo(
    val MediaSources: List<JellyfinMediaSource>,
    val PlaySessionId: String?
)

data class PlaybackInfoRequest(
    val UserId: String,
    val MaxStreamingBitrate: Long = 140_000_000,
    val DeviceProfile: PlaybackDeviceProfile = PlaybackDeviceProfile()
)

data class PlaybackDeviceProfile(
    val DirectPlayProfiles: List<DirectPlayProfile> = listOf(
        DirectPlayProfile(Type = "Video"),
        DirectPlayProfile(Type = "Audio")
    ),
    val TranscodingProfiles: List<TranscodingProfile> = listOf(
        TranscodingProfile(
            Container = "ts",
            Type = "Video",
            VideoCodec = "h264",
            AudioCodec = "aac,mp3,ac3"
        )
    )
)

data class DirectPlayProfile(
    val Type: String
)

data class TranscodingProfile(
    val Container: String,
    val Type: String,
    val VideoCodec: String,
    val AudioCodec: String
)

data class PlaybackStartInfo(
    val ItemId: String,
    val SessionId: String? = null,
    val PlaySessionId: String? = null,
    val MediaSourceId: String? = null,
    val PositionTicks: Long = 0
)

data class PlaybackStopInfo(
    val ItemId: String,
    val SessionId: String? = null,
    val PlaySessionId: String? = null,
    val MediaSourceId: String? = null,
    val PositionTicks: Long = 0
)

data class PlaybackProgressInfo(
    val ItemId: String,
    val SessionId: String? = null,
    val PlaySessionId: String? = null,
    val MediaSourceId: String? = null,
    val PositionTicks: Long = 0,
    val IsPaused: Boolean = false,
    val IsMuted: Boolean = false
)
