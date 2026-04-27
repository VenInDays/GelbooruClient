package com.gelbooru.client.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Soft, organic shapes for the Tactile Minimalism design system.
 */

val TactileCardShape = RoundedCornerShape(16.dp)
val TactileButtonShape = RoundedCornerShape(12.dp)
val TactileDialogShape = RoundedCornerShape(24.dp)

/**
 * A squircle (superellipse) shape for the floating elements.
 */
class SquircleShape(private val cornerRadius: Dp = 16.dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cornerRadius.toPx() }
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val k = r * 0.552284749831f

            moveTo(r, 0f)
            lineTo(w - r, 0f)
            cubicTo(w - r + k, 0f, w, r - k, w, r)
            cubicTo(w, h - r + k, w - r + k, h, w - r, h)
            cubicTo(r - k, h, 0f, h - r + k, 0f, h - r)
            cubicTo(0f, r - k, r - k, 0f, r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Pill shape with uniform corner radius.
 */
class PillShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                topLeft = CornerRadius(size.height / 2f),
                topRight = CornerRadius(size.height / 2f),
                bottomRight = CornerRadius(size.height / 2f),
                bottomLeft = CornerRadius(size.height / 2f)
            )
        )
    }
}
