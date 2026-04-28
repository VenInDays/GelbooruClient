package com.gelbooru.client.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gelbooru.client.data.model.DownloadStatus
import com.gelbooru.client.data.model.GelbooruPost
import com.gelbooru.client.data.model.UserPreferences
import com.gelbooru.client.service.DownloadService
import com.gelbooru.client.ui.components.*
import com.gelbooru.client.ui.theme.TactileTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = viewModel(),
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState(initial = UserPreferences())
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Track notification permission state
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Permission not required below Android 13
            }
        )
    }

    // Permission launcher — properly requests POST_NOTIFICATIONS on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Auto-request notification permission on first composition (Android 13+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Observe preference changes for NSFW
    val isNsfwEnabled by rememberUpdatedState(preferences.showNsfw)

    Box(modifier = Modifier.fillMaxSize().background(TactileTheme.colors.surfaceBase)) {
        // Main gallery content
        ImageGrid(
            posts = uiState.posts,
            isLoading = uiState.isLoading,
            onPostClick = { viewModel.selectPost(it) },
            onLoadMore = { viewModel.loadMore() }
        )

        // Floating toolbar
        FloatingToolbar(
            isSearchActive = searchQuery.isNotBlank(),
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearchSubmit = {
                if (searchQuery.isNotBlank()) {
                    viewModel.search(searchQuery.trim())
                }
            },
            onToggleNsfw = {
                viewModel.toggleNsfw()
            },
            isNsfwEnabled = isNsfwEnabled,
            onMenuClick = onSettingsClick
        )

        // Floating command center
        FloatingCommandCenter(
            onNavigateSearch = {
                if (searchQuery.isNotBlank()) {
                    viewModel.search(searchQuery.trim())
                }
            },
            onNavigateSettings = onSettingsClick
        )

        // Detail overlay
        AnimatedVisibility(
            visible = uiState.selectedPost != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val post = uiState.selectedPost
            if (post != null) {
                ImageDetailContent(
                    imageUrl = uiState.detailImageUrl ?: "",
                    postId = post.postId,
                    tags = post.tags,
                    score = post.score,
                    rating = post.rating,
                    onTagClick = { tag ->
                        searchQuery = tag
                        viewModel.clearDetail()
                        viewModel.search(tag)
                    },
                    onDownloadClick = {
                        val url = uiState.detailImageUrl ?: return@ImageDetailContent
                        DownloadService.startDownload(context, url, post.postId)
                    },
                    onBackClick = { viewModel.clearDetail() }
                )
            }
        }

        // Error overlay
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TactileTheme.colors.scrim),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Card(
                    shape = TactileDialogShape,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            text = uiState.error ?: "Error",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = TactileTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.TextButton(onClick = { viewModel.clearError() }) {
                            androidx.compose.material3.Text("OK")
                        }
                    }
                }
            }
        }
    }
}
