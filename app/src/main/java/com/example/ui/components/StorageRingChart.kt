package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileCategory
import com.example.model.StorageStats
import com.example.ui.theme.PrismCyan

@Composable
fun StorageRingChart(
    stats: StorageStats,
    modifier: Modifier = Modifier,
    chartSize: Dp = 190.dp,
    strokeWidth: Dp = 16.dp
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(stats) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(chartSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Background inactive ring
            drawArc(
                color = Color(0x228090A8),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            val total = stats.totalBytes.toFloat()
            if (total > 0f) {
                val segments = listOf(
                    stats.imagesBytes to FileCategory.IMAGES.primaryColor,
                    stats.videosBytes to FileCategory.VIDEOS.primaryColor,
                    stats.audioBytes to FileCategory.AUDIO.primaryColor,
                    stats.documentsBytes to FileCategory.DOCUMENTS.primaryColor,
                    stats.appsBytes to FileCategory.APPS.primaryColor,
                    stats.vaultBytes to FileCategory.VAULT.primaryColor,
                    stats.otherBytes to Color(0xFF90A4AE)
                )

                var currentStartAngle = -90f
                val spacing = 2.5f // Gap degrees between segments

                for ((bytes, color) in segments) {
                    if (bytes > 0) {
                        val fraction = (bytes / total).coerceIn(0f, 1f)
                        val sweep = ((fraction * 360f) - spacing).coerceAtLeast(0.5f) * animationProgress.value

                        drawArc(
                            color = color,
                            startAngle = currentStartAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                        currentStartAngle += (fraction * 360f) * animationProgress.value
                    }
                }
            }
        }

        // Center Text Summary
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(stats.usedPercent * animationProgress.value).toInt()}%",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrismCyan
                )
            )
            Text(
                text = "Used Storage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${stats.formattedUsed} / ${stats.formattedTotal}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
