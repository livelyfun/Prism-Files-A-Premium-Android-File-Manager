package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassFrostDark
import com.example.ui.theme.GlassFrostLight
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismMagenta
import com.example.ui.theme.PrismPurple

/**
 * Custom Modifier for Glassmorphic frosted glass panels with specular borders.
 */
fun Modifier.glassmorphic(
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            colors = listOf(
                backgroundColor ?: Color(0x38FFFFFF),
                (backgroundColor ?: Color(0x1AFFFFFF)).copy(alpha = 0.08f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                borderColor ?: Color(0x66FFFFFF),
                (borderColor ?: Color(0x1AFFFFFF)).copy(alpha = 0.1f)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) GlassFrostDark else GlassFrostLight
    val borderColor = if (isDark) GlassBorderDark else GlassBorderLight

    Box(
        modifier = modifier
            .glassmorphic(
                cornerRadius = cornerRadius,
                backgroundColor = bgColor,
                borderColor = borderColor
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0x55121824) else Color(0x80FFFFFF)
    val borderColor = if (isDark) Color(0x444F75A8) else Color(0x99FFFFFF)

    Box(
        modifier = modifier
            .glassmorphic(
                cornerRadius = cornerRadius,
                backgroundColor = bgColor,
                borderColor = borderColor,
                borderWidth = 1.2.dp
            )
            .padding(20.dp),
        content = content
    )
}

/**
 * Ambient chromatic mesh background with glowing blur spheres.
 */
@Composable
fun PrismaticMeshBackground(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0C1017) else Color(0xFFEFF3F8))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().blur(70.dp)) {
            val width = size.width
            val height = size.height

            // Top right cyan prism orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrismCyan.copy(alpha = if (isDark) 0.35f else 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.15f),
                    radius = width * 0.55f
                ),
                radius = width * 0.55f,
                center = Offset(width * 0.85f, height * 0.15f)
            )

            // Middle left magenta orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrismMagenta.copy(alpha = if (isDark) 0.30f else 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.15f, height * 0.50f),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(width * 0.15f, height * 0.50f)
            )

            // Bottom purple orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrismPurple.copy(alpha = if (isDark) 0.35f else 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.6f, height * 0.85f),
                    radius = width * 0.65f
                ),
                radius = width * 0.65f,
                center = Offset(width * 0.6f, height * 0.85f)
            )
        }
    }
}

@Composable
fun GlassDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(24.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                GlassSurface(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                    content = content
                )
            }
        }
    }
}
