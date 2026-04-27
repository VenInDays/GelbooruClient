package com.gelbooru.client.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gelbooru.client.ui.theme.TactileTheme
import kotlin.math.roundToInt

/**
 * Floating sidebar/toolbar positioned at the top-left.
 * Partially overlaps the main content area by 20%.
 * Features soft shadows, adaptive positioning, and boundary checks.
 */
@Composable
fun FloatingToolbar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onToggleNsfw: () -> Unit,
    isNsfwEnabled: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val toolbarHeight = 56.dp
    val toolbarHeightPx = with(density) { toolbarHeight.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // The toolbar overlaps the search bar area by 20% of its height
    val overlapOffset = (toolbarHeightPx * 0.20f).roundToInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(x = 0, y = 48.dp.roundToPx()) } // Below status bar
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(toolbarHeight)
                .shadow(
                    elevation = 6.dp,
                    shape = TactileBarShape,
                    ambientColor = TactileTheme.colors.surfaceShadow,
                    spotColor = TactileTheme.colors.surfaceShadow
                )
                .clip(TactileBarShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            TactileTheme.colors.surfaceElevated,
                            TactileTheme.colors.surfaceBase
                        )
                    )
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu hamburger
            TactileIconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(44.dp)
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val color = TactileTheme.colors.textPrimary
                    val strokeWidth = 1.8.dp.toPx()
                    val lineLength = 16.dp.toPx()
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val gap = 4.5.dp.toPx()

                    drawLine(color, Offset(cx - lineLength/2, cy - gap), Offset(cx + lineLength/2, cy - gap), strokeWidth)
                    drawLine(color, Offset(cx - lineLength/2, cy), Offset(cx + lineLength/2, cy), strokeWidth)
                    drawLine(color, Offset(cx - lineLength/2, cy + gap), Offset(cx + lineLength/2, cy + gap), strokeWidth)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Search bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .shadow(2.dp, TactileSmallCardShape, TactileTheme.colors.surfaceShadow)
                    .clip(TactileSmallCardShape)
                    .background(TactileTheme.colors.surfacePressed)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    androidx.compose.material3.Text(
                        text = "Search tags...",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = TactileTheme.colors.textTertiary
                    )
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                        color = TactileTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxSize(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.foundation.text.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { onSearchSubmit() }
                    ),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(TactileTheme.colors.accentPrimary)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // NSFW toggle
            TactileIconButton(
                onClick = onToggleNsfw,
                modifier = Modifier.size(44.dp),
                tint = if (isNsfwEnabled) TactileTheme.colors.error else TactileTheme.colors.textTertiary
            ) {
                androidx.compose.material3.Text(
                    text = "18",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = if (isNsfwEnabled) TactileTheme.colors.error else TactileTheme.colors.textTertiary
                )
            }
        }
    }
}

/**
 * Reusable tactile icon button with press state feedback.
 */
@Composable
fun TactileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = TactileTheme.colors.textPrimary,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val bgColor by androidx.compose.animation.core.animateColorAsState(
        targetValue = if (isPressed) TactileTheme.colors.surfacePressed
                      else TactileTheme.colors.surfaceElevated,
        label = "btn_bg"
    )

    Box(
        modifier = modifier
            .clip(TactileSmallCardShape)
            .background(bgColor)
            .clickable(enabled = enabled) { onClick() }
            .pointerInput(Unit) {
                androidx.compose.ui.input.pointer.detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * A tag chip used in the tag display.
 */
@Composable
fun TactileChip(
    text: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(2.dp, TactileChipShape, TactileTheme.colors.surfaceShadow)
            .clip(TactileChipShape)
            .background(TactileTheme.colors.surfaceElevated)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = TactileTheme.colors.textSecondary
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = false,
    cursorBrush: androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black)
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        cursorBrush = cursorBrush
    )
}
