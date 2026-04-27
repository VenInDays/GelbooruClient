package com.gelbooru.client.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gelbooru.client.ui.theme.TactileTheme
import kotlin.math.roundToInt

/**
 * Floating Command Center — the main navigation hub.
 * A large tactile circle centered at the bottom, partially overlapping the content area by 20%.
 * Expands into a radial menu on tap.
 */
@Composable
fun FloatingCommandCenter(
    onNavigateSearch: () -> Unit = {},
    onNavigateGallery: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onNavigateDownloads: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val fabSize = 64.dp
    val fabSizePx = with(density) { fabSize.toPx() }

    var isExpanded by remember { mutableStateOf(false) }
    val elevation by animateFloatAsState(
        targetValue = if (isExpanded) 16f else 8f,
        label = "fab_elevation"
    )

    // The FAB overlaps content by 20% of its size
    val overlapOffset = (fabSizePx * 0.20f).roundToInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Expanded radial menu
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f),
            modifier = Modifier.offset { IntOffset(x = 0, y = -(fabSizePx + overlapOffset).roundToInt()) }
        ) {
            RadialMenuContent(
                onSearchClick = {
                    isExpanded = false
                    onNavigateSearch()
                },
                onGalleryClick = {
                    isExpanded = false
                    onNavigateGallery()
                },
                onSettingsClick = {
                    isExpanded = false
                    onNavigateSettings()
                },
                onDownloadsClick = {
                    isExpanded = false
                    onNavigateDownloads()
                }
            )
        }

        // Dim overlay when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TactileTheme.colors.scrim)
                    .pointerInput(isExpanded) {
                        if (isExpanded) {
                            detectTapGestures { isExpanded = false }
                        }
                    }
            )
        }

        // Main FAB button
        Box(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = -overlapOffset) }
                .shadow(
                    elevation = elevation.dp,
                    shape = CircleShape,
                    ambientColor = TactileTheme.colors.surfaceShadow,
                    spotColor = TactileTheme.colors.surfaceShadow
                )
                .size(fabSize)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TactileTheme.colors.fabBackground,
                            TactileTheme.colors.accentSecondary
                        ),
                        start = Offset.Zero,
                        end = Offset(fabSizePx, fabSizePx)
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures { isExpanded = !isExpanded }
                },
            contentAlignment = Alignment.Center
        ) {
            // Tactile inner circle with hamburger icon
            Canvas(modifier = Modifier.size(44.dp)) {
                val innerRadius = size.minDimension / 2 - 2.dp.toPx()
                drawCircle(
                    color = TactileTheme.colors.fabForeground.copy(alpha = 0.15f),
                    radius = innerRadius,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                val iconColor = TactileTheme.colors.fabForeground
                val lineLength = 16.dp.toPx()
                val strokeWidth = 2.dp.toPx()
                val cx = size.width / 2
                val cy = size.height / 2
                val gap = 5.dp.toPx()

                drawLine(iconColor, Offset(cx - lineLength/2, cy - gap), Offset(cx + lineLength/2, cy - gap), strokeWidth)
                drawLine(iconColor, Offset(cx - lineLength/2, cy), Offset(cx + lineLength/2, cy), strokeWidth)
                drawLine(iconColor, Offset(cx - lineLength/2, cy + gap), Offset(cx + lineLength/2, cy + gap), strokeWidth)
            }
        }
    }
}

/**
 * Radial menu that appears when the FAB is expanded.
 */
@Composable
private fun RadialMenuContent(
    onSearchClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TactileMenuButton(label = "Search", onClick = onSearchClick)
        TactileMenuButton(label = "Gallery", onClick = onGalleryClick)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TactileMenuButton(label = "Downloads", onClick = onDownloadsClick, modifier = Modifier.weight(1f))
            TactileMenuButton(label = "Settings", onClick = onSettingsClick, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TactileMenuButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(6.dp, TactileSmallCardShape, TactileTheme.colors.surfaceShadow)
            .clip(TactileSmallCardShape)
            .background(TactileTheme.colors.surfaceElevated)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = TactileTheme.colors.textPrimary
        )
    }
}
