package com.gelbooru.client.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gelbooru.client.data.model.DownloadStatus
import com.gelbooru.client.data.model.GelbooruPost
import com.gelbooru.client.data.model.UserPreferences
import com.gelbooru.client.data.repository.GelbooruRepository
import com.gelbooru.client.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GalleryUiState(
    val posts: List<GelbooruPost> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val currentQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedPost: GelbooruPost? = null,
    val detailImageUrl: String? = null,
    val isDetailLoading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadStatus: DownloadStatus? = null
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GelbooruRepository(application)
    private val prefsRepository = PreferencesRepository(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    val userPreferences = prefsRepository.preferences

    private var isLoadingMore = false

    init {
        viewModelScope.launch {
            prefsRepository.preferences.collect { prefs ->
                // React to preference changes
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, currentQuery = query, posts = emptyList(), currentPage = 1) }

            prefsRepository.preferences.first().let { prefs ->
                try {
                    val result = repository.search(query, 1, prefs)
                    _uiState.update {
                        it.copy(
                            posts = result.posts,
                            currentPage = result.currentPage,
                            totalPages = result.totalPages,
                            isLoading = false
                        )
                    }
                    prefsRepository.setLastSearchQuery(query)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.localizedMessage ?: "Failed to load results"
                        )
                    }
                }
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore) return
        val state = _uiState.value
        if (!state.hasNextPage() || state.isLoading || state.currentQuery.isBlank()) return

        isLoadingMore = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            prefsRepository.preferences.first().let { prefs ->
                try {
                    val nextPage = state.currentPage + 1
                    val result = repository.search(state.currentQuery, nextPage, prefs)
                    _uiState.update {
                        it.copy(
                            posts = it.posts + result.posts,
                            currentPage = result.currentPage,
                            totalPages = result.totalPages,
                            isLoadingMore = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoadingMore = false, error = e.localizedMessage) }
                }
            }
            isLoadingMore = false
        }
    }

    fun selectPost(post: GelbooruPost) {
        _uiState.update { it.copy(selectedPost = post, detailImageUrl = null, isDetailLoading = true) }

        viewModelScope.launch {
            prefsRepository.preferences.first().let { prefs ->
                try {
                    val fullUrl = repository.resolveFullImageUrl(post, prefs)
                    _uiState.update { it.copy(detailImageUrl = fullUrl, isDetailLoading = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(detailImageUrl = post.getBestImageUrl(), isDetailLoading = false) }
                }
            }
        }
    }

    fun clearDetail() {
        _uiState.update { it.copy(selectedPost = null, detailImageUrl = null, downloadStatus = null) }
    }

    fun downloadCurrentImage() {
        val state = _uiState.value
        val post = state.selectedPost ?: return
        val imageUrl = state.detailImageUrl ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(downloadStatus = DownloadStatus.DOWNLOADING, downloadProgress = 0f) }

            try {
                repository.downloadImage(imageUrl, post.postId, "Gelbooru").collect { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }
                }
                _uiState.update { it.copy(downloadStatus = DownloadStatus.COMPLETED) }
            } catch (e: Exception) {
                _uiState.update { it.copy(downloadStatus = DownloadStatus.FAILED) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearDownloadStatus() {
        _uiState.update { it.copy(downloadStatus = null, downloadProgress = 0f) }
    }

    fun toggleNsfw() {
        viewModelScope.launch {
            prefsRepository.preferences.first().let { prefs ->
                prefsRepository.setNsfw(!prefs.showNsfw)
            }
        }
    }

    private fun GalleryUiState.hasNextPage() = currentPage < totalPages
}
