package com.gelbooru.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gelbooru.client.data.model.GelbooruPost
import com.gelbooru.client.ui.theme.TactileTheme

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
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(
                start = 6.dp,
                end = 6.dp,
                top = 70.dp, // Space for floating toolbar
                bottom = 90.dp // Space for floating command center
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

            // Load more trigger
            item(span = { GridItemSpan(columns) }) {
                LaunchedEffect(Unit) { onLoadMore() }
                if (isLoading) {
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
            .shadow(2.dp, RoundedCornerShape(6.dp), TactileTheme.colors.surfaceShadow)
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
        if (post.rating != com.gelbooru.client.data.model.PostRating.SAFE &&
            post.rating != com.gelbooru.client.data.model.PostRating.UNKNOWN) {
            val ratingColor = when (post.rating) {
                com.gelbooru.client.data.model.PostRating.EXPLICIT -> Color(0xFFE53935)
                com.gelbooru.client.data.model.PostRating.QUESTIONABLE -> Color(0xFFFFA726)
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
            kotlinx.coroutines.delay(50)
        }
    }

    Canvas(
        modifier = modifier.size(32.dp)
    ) {
        val strokeWidth = 2.5.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2

        // Track
        drawCircle(
            color = TactileTheme.colors.progressTrack,
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // Progress arc
        drawArc(
            color = TactileTheme.colors.progressFill,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

/**
 * Full-screen image detail view with pinch-to-zoom support.
 */
@Composable
fun ImageDetailContent(
    imageUrl: String,
    postId: Int,
    tags: List<String>,
    score: Int,
    rating: com.gelbooru.client.data.model.PostRating,
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

        // Top gradient overlay with back button
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
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .shadow(4.dp, RoundedCornerShape(12.dp), TactileTheme.colors.surfaceShadow)
                    .background(
                        color = TactileTheme.colors.surfaceElevated.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack
                Text("<", color = TactileTheme.colors.textPrimary, style = MaterialTheme.typography.titleLarge)
            }

            // Download button
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
                    .shadow(4.dp, RoundedCornerShape(12.dp), TactileTheme.colors.surfaceShadow)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.take(8).forEach { tag ->
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
                if (tags.size > 8) {
                    Text(
                        text = "+${tags.size - 8}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
