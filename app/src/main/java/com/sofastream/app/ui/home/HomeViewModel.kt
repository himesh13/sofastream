package com.sofastream.app.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sofastream.app.SofaStreamApp
import com.sofastream.app.api.ApiClient
import com.sofastream.app.data.model.MediaItem
import com.sofastream.app.data.repository.JellyfinRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val prefs = SofaStreamApp.instance.userPreferences

    private val _continueWatching = MutableLiveData<List<MediaItem>>()
    val continueWatching: LiveData<List<MediaItem>> = _continueWatching

    private val _recentMovies = MutableLiveData<List<MediaItem>>()
    val recentMovies: LiveData<List<MediaItem>> = _recentMovies

    private val _recentSeries = MutableLiveData<List<MediaItem>>()
    val recentSeries: LiveData<List<MediaItem>> = _recentSeries

    private val _featuredItem = MutableLiveData<MediaItem?>()
    val featuredItem: LiveData<MediaItem?> = _featuredItem

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private fun getRepository(): JellyfinRepository {
        return JellyfinRepository(
            api = ApiClient.getJellyfinApi(prefs.getJellyfinUrlSync()),
            baseUrl = prefs.getJellyfinUrlSync(),
            token = prefs.getJellyfinTokenSync(),
            userId = prefs.getJellyfinUserIdSync()
        )
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val repo = getRepository()

                val latestResult = repo.getLatestMedia()
                latestResult.onSuccess { items ->
                    val movies = items.filter { it.type == com.sofastream.app.data.model.MediaType.MOVIE }
                    val series = items.filter { it.type == com.sofastream.app.data.model.MediaType.SERIES }
                    _recentMovies.value = movies
                    _recentSeries.value = series
                    _featuredItem.value = items.firstOrNull()
                }

                val continueResult = repo.getMovies(0, 10)
                continueResult.onSuccess { (movies, _) ->
                    _continueWatching.value = movies.filter { it.playedPercentage > 0 && it.playedPercentage < 100 }
                }

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getMovies(startIndex: Int = 0, onResult: (List<MediaItem>, Int) -> Unit) {
        viewModelScope.launch {
            val result = getRepository().getMovies(startIndex)
            result.onSuccess { (items, total) -> onResult(items, total) }
        }
    }

    fun getSeries(startIndex: Int = 0, onResult: (List<MediaItem>, Int) -> Unit) {
        viewModelScope.launch {
            val result = getRepository().getSeries(startIndex)
            result.onSuccess { (items, total) -> onResult(items, total) }
        }
    }
}
