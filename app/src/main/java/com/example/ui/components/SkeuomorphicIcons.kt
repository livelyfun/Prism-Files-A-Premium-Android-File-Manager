package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.FileCategory

@Composable
fun SkeuomorphicFolderIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    accentColor: Color? = null
) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            val baseWarmYellow = accentColor ?: Color(0xFFF9A825)
            val lightTabColor = Color(0xFFFFD54F)
            val shadowColor = Color(0xFFE65100).copy(alpha = 0.5f)

            // 1. Back Folder Body (Darker warm tone)
            val backFolder = Path().apply {
                moveTo(w * 0.08f, h * 0.22f)
                // Tab shoulder
                lineTo(w * 0.42f, h * 0.22f)
                cubicTo(w * 0.46f, h * 0.22f, w * 0.48f, h * 0.28f, w * 0.52f, h * 0.28f)
                lineTo(w * 0.92f, h * 0.28f)
                cubicTo(w * 0.96f, h * 0.28f, w * 0.96f, h * 0.32f, w * 0.96f, h * 0.36f)
                lineTo(w * 0.96f, h * 0.86f)
                cubicTo(w * 0.96f, h * 0.92f, w * 0.92f, h * 0.92f, w * 0.88f, h * 0.92f)
                lineTo(w * 0.12f, h * 0.92f)
                cubicTo(w * 0.08f, h * 0.92f, w * 0.08f, h * 0.86f, w * 0.08f, h * 0.86f)
                close()
            }
            drawPath(
                path = backFolder,
                brush = Brush.verticalGradient(
                    colors = listOf(lightTabColor, baseWarmYellow)
                )
            )

            // 2. Paper sheets peeking out of folder
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0))
                ),
                topLeft = Offset(w * 0.20f, h * 0.20f),
                size = Size(w * 0.60f, h * 0.40f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Paper text lines
            drawLine(
                color = Color(0xFFBDBDBD),
                start = Offset(w * 0.26f, h * 0.27f),
                end = Offset(w * 0.65f, h * 0.27f),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color(0xFFBDBDBD),
                start = Offset(w * 0.26f, h * 0.33f),
                end = Offset(w * 0.55f, h * 0.33f),
                strokeWidth = 1.5.dp.toPx()
            )

            // 3. Front Folder Flap (Layered 3D envelope with bevel)
            val frontFlap = Path().apply {
                moveTo(w * 0.06f, h * 0.40f)
                lineTo(w * 0.94f, h * 0.40f)
                cubicTo(w * 0.97f, h * 0.40f, w * 0.98f, h * 0.44f, w * 0.96f, h * 0.48f)
                lineTo(w * 0.90f, h * 0.90f)
                cubicTo(w * 0.88f, h * 0.94f, w * 0.84f, h * 0.94f, w * 0.80f, h * 0.94f)
                lineTo(w * 0.20f, h * 0.94f)
                cubicTo(w * 0.16f, h * 0.94f, w * 0.12f, h * 0.94f, w * 0.10f, h * 0.90f)
                lineTo(w * 0.04f, h * 0.48f)
                cubicTo(w * 0.02f, h * 0.44f, w * 0.03f, h * 0.40f, w * 0.06f, h * 0.40f)
                close()
            }

            // Drop shadow behind front flap
            drawPath(
                path = frontFlap,
                color = shadowColor
            )

            // Rich warm gradient on front flap
            drawPath(
                path = frontFlap,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFE082),
                        baseWarmYellow,
                        Color(0xFFF57F17)
                    )
                )
            )

            // Embossed top rim highlight on front flap
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x99FFFFFF), Color(0x33FFFFFF))
                ),
                start = Offset(w * 0.06f, h * 0.41f),
                end = Offset(w * 0.94f, h * 0.41f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
fun SkeuomorphicFileIcon(
    category: FileCategory,
    extension: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            when (category) {
                FileCategory.IMAGES -> {
                    // Polaroid print photo frame
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF0F0F0))),
                        topLeft = Offset(w * 0.10f, h * 0.08f),
                        size = Size(w * 0.80f, h * 0.84f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // Inner photo landscape
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFF26A69A))),
                        topLeft = Offset(w * 0.18f, h * 0.16f),
                        size = Size(w * 0.64f, h * 0.52f),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    // Sun circle
                    drawCircle(
                        color = Color(0xFFFFEE58),
                        radius = w * 0.08f,
                        center = Offset(w * 0.65f, h * 0.28f)
                    )
                    // Mountain triangle
                    val mountain = Path().apply {
                        moveTo(w * 0.22f, h * 0.66f)
                        lineTo(w * 0.45f, h * 0.38f)
                        lineTo(w * 0.60f, h * 0.52f)
                        lineTo(w * 0.72f, h * 0.35f)
                        lineTo(w * 0.82f, h * 0.66f)
                        close()
                    }
                    drawPath(mountain, color = Color(0xFF2E7D32))
                }
                FileCategory.VIDEOS -> {
                    // Movie slate / clapperboard
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFF263238), Color(0xFF101820))),
                        topLeft = Offset(w * 0.10f, h * 0.10f),
                        size = Size(w * 0.80f, h * 0.80f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // Clapper stripes
                    for (i in 0..3) {
                        drawLine(
                            color = Color(0xFFFF5252),
                            start = Offset(w * (0.18f + i * 0.18f), h * 0.12f),
                            end = Offset(w * (0.26f + i * 0.18f), h * 0.26f),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                    // Play triangle badge
                    val playTri = Path().apply {
                        moveTo(w * 0.42f, h * 0.42f)
                        lineTo(w * 0.64f, h * 0.55f)
                        lineTo(w * 0.42f, h * 0.68f)
                        close()
                    }
                    drawPath(playTri, color = Color(0xFFFF1744))
                }
                FileCategory.AUDIO -> {
                    // Vinyl LP record disc
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF37474F), Color(0xFF10171D), Color(0xFF000000))
                        ),
                        radius = w * 0.42f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                    // Grooves
                    drawCircle(
                        color = Color(0x33FFFFFF),
                        radius = w * 0.32f,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0x22FFFFFF),
                        radius = w * 0.22f,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    // Center label
                    drawCircle(
                        color = Color(0xFFFF9100),
                        radius = w * 0.14f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.04f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                }
                FileCategory.VAULT -> {
                    // Titanium safe badge with glowing lock
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF607D8B), Color(0xFF263238), Color(0xFF455A64))
                        ),
                        topLeft = Offset(w * 0.12f, h * 0.12f),
                        size = Size(w * 0.76f, h * 0.76f),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    // Beveled border
                    drawRoundRect(
                        color = Color(0xFF90A4AE),
                        topLeft = Offset(w * 0.12f, h * 0.12f),
                        size = Size(w * 0.76f, h * 0.76f),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Neon keyhole
                    drawCircle(
                        color = Color(0xFFFF2A85),
                        radius = w * 0.10f,
                        center = Offset(w * 0.5f, h * 0.45f)
                    )
                    drawRoundRect(
                        color = Color(0xFFFF2A85),
                        topLeft = Offset(w * 0.44f, h * 0.45f),
                        size = Size(w * 0.12f, h * 0.18f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
                else -> {
                    // Textured paper document with folded dog-ear corner
                    val paper = Path().apply {
                        moveTo(w * 0.15f, h * 0.10f)
                        lineTo(w * 0.65f, h * 0.10f)
                        lineTo(w * 0.85f, h * 0.30f)
                        lineTo(w * 0.85f, h * 0.90f)
                        lineTo(w * 0.15f, h * 0.90f)
                        close()
                    }
                    drawPath(
                        path = paper,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFFFFF), Color(0xFFECEFF1))
                        )
                    )

                    // Folded corner flap
                    val fold = Path().apply {
                        moveTo(w * 0.65f, h * 0.10f)
                        lineTo(w * 0.65f, h * 0.30f)
                        lineTo(w * 0.85f, h * 0.30f)
                        close()
                    }
                    drawPath(fold, color = Color(0xFFCFD8DC))

                    // Text lines
                    val lineColor = category.primaryColor
                    drawLine(
                        color = lineColor,
                        start = Offset(w * 0.25f, h * 0.42f),
                        end = Offset(w * 0.75f, h * 0.42f),
                        strokeWidth = 2.5.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFB0BEC5),
                        start = Offset(w * 0.25f, h * 0.54f),
                        end = Offset(w * 0.65f, h * 0.54f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFB0BEC5),
                        start = Offset(w * 0.25f, h * 0.66f),
                        end = Offset(w * 0.55f, h * 0.66f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}
