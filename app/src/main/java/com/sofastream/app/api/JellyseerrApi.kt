package com.sofastream.app.api

import retrofit2.Response
import retrofit2.http.*

// Jellyseerr API endpoints
interface JellyseerrApi {

    @POST("api/v1/auth/local")
    suspend fun login(
        @Body body: JellyseerrLoginRequest
    ): Response<JellyseerrUser>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<JellyseerrUser>

    @GET("api/v1/movie/{movieId}")
    suspend fun getMovieDetails(
        @Path("movieId") movieId: Int
    ): Response<JellyseerrMediaDetails>

    @GET("api/v1/tv/{tvId}")
    suspend fun getTvDetails(
        @Path("tvId") tvId: Int
    ): Response<JellyseerrMediaDetails>

    @POST("api/v1/request")
    suspend fun createRequest(
        @Body body: JellyseerrRequestBody
    ): Response<JellyseerrRequest>

    @GET("api/v1/request")
    suspend fun getRequests(
        @Query("take") take: Int = 20,
        @Query("skip") skip: Int = 0,
        @Query("filter") filter: String = "all"
    ): Response<JellyseerrRequestsResponse>

    @GET("api/v1/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en"
    ): Response<JellyseerrSearchResponse>

    @GET("api/v1/discover/movies")
    suspend fun discoverMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en"
    ): Response<JellyseerrDiscoverResponse>

    @GET("api/v1/discover/tv")
    suspend fun discoverTv(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en"
    ): Response<JellyseerrDiscoverResponse>

    @GET("api/v1/discover/trending")
    suspend fun getTrending(
        @Query("page") page: Int = 1
    ): Response<JellyseerrDiscoverResponse>
}

data class JellyseerrLoginRequest(
    val email: String,
    val password: String
)

data class JellyseerrUser(
    val id: Int,
    val displayName: String,
    val email: String,
    val avatar: String?
)

data class JellyseerrMediaDetails(
    val id: Int,
    val title: String?,
    val name: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val firstAirDate: String?,
    val voteAverage: Double?,
    val genres: List<JellyseerrGenre>?,
    val mediaInfo: JellyseerrMediaInfo?
)

data class JellyseerrGenre(
    val id: Int,
    val name: String
)

data class JellyseerrMediaInfo(
    val id: Int,
    val tmdbId: Int,
    val status: Int,
    val requests: List<JellyseerrRequest>?
)

data class JellyseerrRequestBody(
    val mediaType: String,
    val mediaId: Int,
    val tvdbId: Int? = null,
    val seasons: List<Int>? = null
)

data class JellyseerrRequest(
    val id: Int,
    val status: Int,
    val media: JellyseerrMediaInfo?,
    val requestedBy: JellyseerrUser?,
    val createdAt: String?
)

data class JellyseerrRequestsResponse(
    val pageInfo: JellyseerrPageInfo,
    val results: List<JellyseerrRequest>
)

data class JellyseerrPageInfo(
    val pages: Int,
    val pageSize: Int,
    val results: Int,
    val page: Int
)

data class JellyseerrSearchResponse(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<JellyseerrSearchResult>
)

data class JellyseerrSearchResult(
    val id: Int,
    val mediaType: String,
    val title: String?,
    val name: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val firstAirDate: String?,
    val voteAverage: Double?,
    val mediaInfo: JellyseerrMediaInfo?
)

data class JellyseerrDiscoverResponse(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<JellyseerrSearchResult>
)
