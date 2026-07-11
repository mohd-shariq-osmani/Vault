package com.shariq.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shariq.vault.ui.theme.AccentGlow

/**
 * Premium glassmorphic document card.
 *
 * Features:
 *  - Deep gradient base from the provided colour list
 *  - Hairline white top-edge shimmer border
 *  - Soft coloured ambient glow via drawBehind layer
 *  - 20dp corner radius (Cinema Mobile spec)
 */
@Composable
fun GlassmorphicCard(
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    glowColor: Color = AccentGlow,
    glowRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val accentColor = gradient.firstOrNull() ?: Color.Transparent

    val cardModifier = modifier
        .fillMaxWidth()
        // Coloured ambient glow layer behind the card
        .drawBehind {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(
                            glowRadius.toPx(),
                            0f,
                            6f,
                            accentColor.copy(alpha = 0.35f).toArgb()
                        )
                    }
                }
                canvas.drawRoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    radiusX = 20.dp.toPx(),
                    radiusY = 20.dp.toPx(),
                    paint = paint
                )
            }
        }
        .clip(shape)
        // Base gradient fill
        .background(
            brush = Brush.linearGradient(
                colors = gradient.map { it.copy(alpha = 0.92f) }
            )
        )
        // Top-edge shimmer accent (diagonal gradient with bright top-left corner)
        .background(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.07f),
                    0.3f to Color.Transparent
                )
            )
        )
        // Hairline border — bright on top, fades to transparent at bottom
        .border(
            width = 0.8.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.04f)
                )
            ),
            shape = shape
        )

    Column(
        modifier = cardModifier.padding(horizontal = 24.dp, vertical = 22.dp),
        content = content
    )
}
