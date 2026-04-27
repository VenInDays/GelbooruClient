package com.gelbooru.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.detectTapGestures

private val ToolbarShape = RoundedCornerShape(20.dp)
private val SmallCardShape = RoundedCornerShape(12.dp)
private val ChipShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalFoundationApi::class)
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
    val density = LocalDensity.current
    val toolbarHeight = 56.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(x = 0, y = 48.dp.roundToPx()) }
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(toolbarHeight)
                .shadow(
                    elevation = 6.dp,
                    shape = ToolbarShape,
                    ambientColor = TactileTheme.colors.surfaceShadow,
                    spotColor = TactileTheme.colors.surfaceShadow
                )
                .clip(ToolbarShape)
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
            // Menu hamburger button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SmallCardShape)
                    .background(TactileTheme.colors.surfaceElevated)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val color = TactileTheme.colors.textPrimary
                    val strokeWidth = 1.8.dp.toPx()
                    val lineLength = 16.dp.toPx()
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val gap = 4.5.dp.toPx()
                    drawLine(color, Offset(cx - lineLength / 2, cy - gap), Offset(cx + lineLength / 2, cy - gap), strokeWidth)
                    drawLine(color, Offset(cx - lineLength / 2, cy), Offset(cx + lineLength / 2, cy), strokeWidth)
                    drawLine(color, Offset(cx - lineLength / 2, cy + gap), Offset(cx + lineLength / 2, cy + gap), strokeWidth)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Search bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .shadow(2.dp, SmallCardShape, TactileTheme.colors.surfaceShadow)
                    .clip(SmallCardShape)
                    .background(TactileTheme.colors.surfacePressed)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search tags...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TactileTheme.colors.textTertiary
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = TactileTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxSize(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    singleLine = true,
                    cursorBrush = SolidColor(TactileTheme.colors.accentPrimary)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // NSFW toggle button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SmallCardShape)
                    .background(TactileTheme.colors.surfaceElevated)
                    .clickable { onToggleNsfw() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "18",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNsfwEnabled) TactileTheme.colors.error else TactileTheme.colors.textTertiary
                )
            }
        }
    }
}

@Composable
fun TactileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = TactileTheme.colors.textPrimary,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) TactileTheme.colors.surfacePressed
        else TactileTheme.colors.surfaceElevated,
        label = "btn_bg"
    )

    Box(
        modifier = modifier
            .clip(SmallCardShape)
            .background(bgColor)
            .clickable(enabled = enabled) { onClick() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun TactileChip(
    text: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(2.dp, ChipShape, TactileTheme.colors.surfaceShadow)
            .clip(ChipShape)
            .background(TactileTheme.colors.surfaceElevated)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TactileTheme.colors.textSecondary
        )
    }
}
