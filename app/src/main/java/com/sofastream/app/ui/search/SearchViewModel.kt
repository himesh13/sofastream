package com.sofastream.app.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sofastream.app.SofaStreamApp
import com.sofastream.app.api.ApiClient
import com.sofastream.app.data.model.MediaItem
import com.sofastream.app.data.repository.JellyfinRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val prefs = SofaStreamApp.instance.userPreferences

    private val _results = MutableLiveData<List<MediaItem>>()
    val results: LiveData<List<MediaItem>> = _results

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var searchJob: Job? = null

    private fun getRepository(): JellyfinRepository {
        return JellyfinRepository(
            api = ApiClient.getJellyfinApi(prefs.getJellyfinUrlSync()),
            baseUrl = prefs.getJellyfinUrlSync(),
            token = prefs.getJellyfinTokenSync(),
            userId = prefs.getJellyfinUserIdSync()
        )
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            _error.value = null
            val result = getRepository().searchItems(query)
            result.onSuccess { _results.value = it }
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
