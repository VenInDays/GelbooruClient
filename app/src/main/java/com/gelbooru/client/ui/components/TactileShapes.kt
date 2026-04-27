package com.gelbooru.client.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft, organic shapes for the Tactile Minimalism design system.
 */

val TactileCardShape = RoundedCornerShape(16.dp)
val TactileButtonShape = RoundedCornerShape(12.dp)
val TactileFabShape = androidx.compose.foundation.shape.CircleShape
val TactileDialogShape = RoundedCornerShape(24.dp)
val TactileBarShape = RoundedCornerShape(20.dp)
val TactileChipShape = RoundedCornerShape(8.dp)
val TactileSmallCardShape = RoundedCornerShape(12.dp)

/**
 * A squircle (superellipse) shape for the floating elements.
 */
class SquircleShape(private val cornerRadius: Dp = 16.dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val r = with(density) { cornerRadius.toPx() }
        val path = Path().apply {
            val w = size.width
            val h = size.height
            // Approximate squircle with bezier curves
            val k = r * 0.552284749831f // Circle approximation constant

            moveTo(r, 0f)
            lineTo(w - r, 0f)
            cubicTo(w - r + k, 0f, w, r - k, w, r)
            cubicTo(w, h - r + k, w - r + k, h, w - r, h)
            cubicTo(r - k, h, 0f, h - r + k, 0f, h - r)
            cubicTo(0f, r - k, r - k, 0f, r, 0f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/**
 * Pill shape with uniform corner radius.
 */
class PillShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                topLeft = CornerRadius(size.height / 2f),
                topRight = CornerRadius(size.height / 2f),
                bottomRight = CornerRadius(size.height / 2f),
                bottomLeft = CornerRadius(size.height / 2f)
            )
        )
    }
}
