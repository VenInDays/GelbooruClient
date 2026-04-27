package com.gelbooru.client.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gelbooru.client.data.model.GelbooruPost
import com.gelbooru.client.data.model.PostRating
import com.gelbooru.client.ui.theme.TactileTheme
import kotlinx.coroutines.delay

/**
 * Responsive image grid for gallery display.
 */
@Composable
fun ImageGrid(
    posts: List<GelbooruPost>,
    isLoading: Boolean,
    onPostClick: (GelbooruPost) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val gridState = rememberLazyGridState()

    // Trigger load more when reaching end
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= gridState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && posts.isNotEmpty()) {
            onLoadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            contentPadding = PaddingValues(
                start = 6.dp,
                end = 6.dp,
                top = 70.dp,
                bottom = 90.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = posts,
                key = { it.postId }
            ) { post ->
                GalleryImageItem(
                    post = post,
                    onClick = { onPostClick(post) }
                )
            }

            if (isLoading) {
                item(span = { GridItemSpan(columns) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TactileLoadingIndicator()
                    }
                }
            }
        }

        // Empty state
        if (posts.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TactileTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try different tags",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TactileTheme.colors.textTertiary
                    )
                }
            }
        }
    }
}

/**
 * Single gallery image item with subtle shadow and rounded corners.
 */
@Composable
fun GalleryImageItem(
    post: GelbooruPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(2.dp, RoundedCornerShape(6.dp), ambientColor = TactileTheme.colors.surfaceShadow, spotColor = TactileTheme.colors.surfaceShadow)
            .clip(RoundedCornerShape(6.dp))
            .background(TactileTheme.colors.surfacePressed)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = post.getDisplayUrl(),
            contentDescription = "Post ${post.postId}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Score indicator
        if (post.score > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${post.score}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Rating indicator
        if (post.rating != PostRating.SAFE && post.rating != PostRating.UNKNOWN) {
            val ratingColor = when (post.rating) {
                PostRating.EXPLICIT -> Color(0xFFE53935)
                PostRating.QUESTIONABLE -> Color(0xFFFFA726)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        color = ratingColor.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .size(8.dp)
            )
        }
    }
}

/**
 * Loading indicator with tactile feel.
 */
@Composable
fun TactileLoadingIndicator(modifier: Modifier = Modifier) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            progress += 0.02f
            if (progress > 1f) progress = 0f
            delay(50)
        }
    }

    val trackColor = TactileTheme.colors.progressTrack
    val fillColor = TactileTheme.colors.progressFill
    Canvas(
        modifier = modifier.size(32.dp)
    ) {
        val strokeWidth = 2.5.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2

        // Track
        drawCircle(
            color = trackColor,
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        // Progress arc
        drawArc(
            color = fillColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * Full-screen image detail view.
 */
@Composable
fun ImageDetailContent(
    imageUrl: String,
    postId: Int,
    tags: List<String>,
    score: Int,
    rating: PostRating,
    onTagClick: (String) -> Unit,
    onDownloadClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Full-screen image
        AsyncImage(
            model = imageUrl,
            contentDescription = "Post $postId",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Top gradient overlay with buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            TactileTheme.colors.overlay,
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        ) {
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = TactileTheme.colors.surfaceShadow, spotColor = TactileTheme.colors.surfaceShadow)
                    .background(
                        color = TactileTheme.colors.surfaceElevated.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text("<", color = TactileTheme.colors.textPrimary, style = MaterialTheme.typography.titleLarge)
            }

            // Download button
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
                    .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = TactileTheme.colors.surfaceShadow, spotColor = TactileTheme.colors.surfaceShadow)
                    .background(
                        color = TactileTheme.colors.surfaceElevated.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text("DL", color = TactileTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Bottom info panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TactileTheme.colors.overlay
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Post ID and score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#$postId",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags as chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tags.take(6).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onTagClick(tag) }
                    ) {
                        Text(
                            text = tag.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (tags.size > 6) {
                    Text(
                        text = "+${tags.size - 6}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
