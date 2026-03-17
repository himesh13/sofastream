package com.sofastream.app.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sofastream.app.SofaStreamApp
import com.sofastream.app.api.ApiClient
import com.sofastream.app.data.model.*
import com.sofastream.app.data.repository.JellyfinRepository
import com.sofastream.app.data.repository.JellyseerrRepository
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {

    private val prefs = SofaStreamApp.instance.userPreferences

    private val _mediaItem = MutableLiveData<MediaItem>()
    val mediaItem: LiveData<MediaItem> = _mediaItem

    private val _seasons = MutableLiveData<List<Season>>()
    val seasons: LiveData<List<Season>> = _seasons

    private val _episodes = MutableLiveData<List<MediaItem>>()
    val episodes: LiveData<List<MediaItem>> = _episodes

    private val _selectedSeason = MutableLiveData<Season?>()
    val selectedSeason: LiveData<Season?> = _selectedSeason

    private val _playbackInfo = MutableLiveData<PlaybackInfo?>()
    val playbackInfo: LiveData<PlaybackInfo?> = _playbackInfo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _requestSuccess = MutableLiveData<Boolean?>()
    val requestSuccess: LiveData<Boolean?> = _requestSuccess

    private fun getJellyfinRepo(): JellyfinRepository {
        return JellyfinRepository(
            api = ApiClient.getJellyfinApi(prefs.getJellyfinUrlSync()),
            baseUrl = prefs.getJellyfinUrlSync(),
            token = prefs.getJellyfinTokenSync(),
            userId = prefs.getJellyfinUserIdSync()
        )
    }

    private fun getJellyseerrRepo(): JellyseerrRepository? {
        val url = prefs.getJellyseerrUrlSync()
        if (url.isBlank()) return null
        return JellyseerrRepository(ApiClient.getJellyseerrApi(url))
    }

    fun loadDetails(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = getJellyfinRepo().getItemDetails(itemId)
            result.onSuccess { item ->
                _mediaItem.value = item
                if (item.type == MediaType.SERIES) {
                    loadSeasons(itemId)
                }
            }
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    private fun loadSeasons(seriesId: String) {
        viewModelScope.launch {
            val result = getJellyfinRepo().getSeasons(seriesId)
            result.onSuccess { seasons ->
                _seasons.value = seasons
                seasons.firstOrNull()?.let { loadEpisodes(seriesId, it) }
            }
        }
    }

    fun selectSeason(season: Season) {
        _selectedSeason.value = season
        val seriesId = _mediaItem.value?.id ?: return
        loadEpisodes(seriesId, season)
    }

    private fun loadEpisodes(seriesId: String, season: Season) {
        viewModelScope.launch {
            val result = getJellyfinRepo().getEpisodes(seriesId, season.id)
            result.onSuccess { _episodes.value = it }
        }
    }

    fun getPlaybackInfo(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = getJellyfinRepo().getPlaybackInfo(itemId)
            result.onSuccess { _playbackInfo.value = it }
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun requestMedia(mediaType: String, tmdbId: Int) {
        viewModelScope.launch {
            val repo = getJellyseerrRepo() ?: run {
                _error.value = "Jellyseerr not configured"
                return@launch
            }
            val result = repo.requestMedia(mediaType, tmdbId)
            result.onSuccess { _requestSuccess.value = true }
            result.onFailure { _requestSuccess.value = false }
        }
    }

    fun clearPlaybackInfo() {
        _playbackInfo.value = null
    }

    fun clearRequestStatus() {
        _requestSuccess.value = null
    }
}
