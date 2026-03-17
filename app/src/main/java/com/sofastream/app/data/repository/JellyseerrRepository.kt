package com.sofastream.app.data.repository

import com.sofastream.app.api.*
import com.sofastream.app.data.model.*

class JellyseerrRepository(private val api: JellyseerrApi) {

    suspend fun login(email: String, password: String): Result<JellyseerrUser> {
        return try {
            val response = api.login(JellyseerrLoginRequest(email, password))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(query: String): Result<List<JellyseerrSearchResult>> {
        return try {
            val response = api.search(query)
            if (response.isSuccessful) {
                Result.success(response.body()?.results ?: emptyList())
            } else {
                Result.failure(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrending(): Result<List<JellyseerrSearchResult>> {
        return try {
            val response = api.getTrending()
            if (response.isSuccessful) {
                Result.success(response.body()?.results ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverMovies(): Result<List<JellyseerrSearchResult>> {
        return try {
            val response = api.discoverMovies()
            if (response.isSuccessful) {
                Result.success(response.body()?.results ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverTv(): Result<List<JellyseerrSearchResult>> {
        return try {
            val response = api.discoverTv()
            if (response.isSuccessful) {
                Result.success(response.body()?.results ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestMedia(mediaType: String, mediaId: Int, seasons: List<Int>? = null): Result<JellyseerrRequest> {
        return try {
            val body = JellyseerrRequestBody(
                mediaType = mediaType,
                mediaId = mediaId,
                seasons = seasons
            )
            val response = api.createRequest(body)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Request failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRequests(): Result<List<JellyseerrRequest>> {
        return try {
            val response = api.getRequests()
            if (response.isSuccessful) {
                Result.success(response.body()?.results ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieDetails(movieId: Int): Result<JellyseerrMediaDetails> {
        return try {
            val response = api.getMovieDetails(movieId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTvDetails(tvId: Int): Result<JellyseerrMediaDetails> {
        return try {
            val response = api.getTvDetails(tvId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
