package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeumorphDarkBase
import com.example.ui.theme.NeumorphDarkDarkShadow
import com.example.ui.theme.NeumorphDarkLightShadow
import com.example.ui.theme.NeumorphLightBase
import com.example.ui.theme.NeumorphLightDarkShadow
import com.example.ui.theme.NeumorphLightLightShadow
import com.example.ui.theme.PrismCyan

/**
 * Custom Modifier for Neumorphic Dual Shadows (Extruded or Inset look).
 */
fun Modifier.neumorphic(
    elevation: Dp = 6.dp,
    isPressed: Boolean = false,
    cornerRadius: Dp = 16.dp,
    lightShadowColor: Color? = null,
    darkShadowColor: Color? = null,
    baseColor: Color? = null
): Modifier = this.drawBehind {
    val light = lightShadowColor ?: Color(0xFF262E40)
    val dark = darkShadowColor ?: Color(0xFF070A0F)
    val base = baseColor ?: Color(0xFF161B26)

    val shadowDist = if (isPressed) 2.dp.toPx() else elevation.toPx()
    val blurRadius = if (isPressed) 4.dp.toPx() else (elevation * 1.5f).toPx()

    drawIntoCanvas { canvas ->
        val lightPaint = Paint().apply {
            asFrameworkPaint().apply {
                this.color = light.toArgb()
                this.isAntiAlias = true
                this.setShadowLayer(blurRadius, -shadowDist, -shadowDist, light.toArgb())
            }
            this.style = PaintingStyle.Fill
        }

        val darkPaint = Paint().apply {
            asFrameworkPaint().apply {
                this.color = dark.toArgb()
                this.isAntiAlias = true
                this.setShadowLayer(blurRadius, shadowDist, shadowDist, dark.toArgb())
            }
            this.style = PaintingStyle.Fill
        }

        val cornerPx = cornerRadius.toPx()
        // Draw bottom-right dark shadow
        canvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, darkPaint)
        // Draw top-left light shadow
        canvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, lightPaint)
    }
}.background(
    color = baseColor ?: Color(0xFF161B26),
    shape = RoundedCornerShape(cornerRadius)
)

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) NeumorphDarkBase else NeumorphLightBase
    val lightShadow = if (isDark) NeumorphDarkLightShadow else NeumorphLightLightShadow
    val darkShadow = if (isDark) NeumorphDarkDarkShadow else NeumorphLightDarkShadow

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .neumorphic(
                elevation = elevation,
                cornerRadius = cornerRadius,
                lightShadowColor = lightShadow,
                darkShadowColor = darkShadow,
                baseColor = baseColor
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun NeumorphicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) NeumorphDarkBase else NeumorphLightBase
    val trackWidth = 56.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp

    val animatedOffset by animateFloatAsState(
        targetValue = if (checked) 24f else 4f,
        animationSpec = tween(durationMillis = 200),
        label = "switchOffset"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Neumorphic switch track
        Box(
            modifier = Modifier
                .size(trackWidth, trackHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (checked) {
                        Brush.horizontalGradient(listOf(PrismCyan, Color(0xFF00B0FF)))
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                if (isDark) Color(0xFF10141C) else Color(0xFFD6DCE5),
                                baseColor
                            )
                        )
                    }
                )
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset.dp)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(
                        if (checked) Color.White else (if (isDark) Color(0xFF2C3547) else Color.White)
                    )
                    .shadow(elevation = 3.dp, shape = CircleShape)
            )
        }
    }
}

@Composable
fun NeumorphicPinKey(
    digit: String,
    subLabel: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()

    val baseColor = if (isDark) NeumorphDarkBase else NeumorphLightBase
    val lightShadow = if (isDark) NeumorphDarkLightShadow else NeumorphLightLightShadow
    val darkShadow = if (isDark) NeumorphDarkDarkShadow else NeumorphLightDarkShadow

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .neumorphic(
                elevation = if (isPressed) 2.dp else 6.dp,
                isPressed = isPressed,
                cornerRadius = 36.dp,
                lightShadowColor = lightShadow,
                darkShadowColor = darkShadow,
                baseColor = baseColor
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .testTag("pin_key_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) PrismCyan else MaterialTheme.colorScheme.onSurface
            )
            if (subLabel.isNotBlank()) {
                Text(
                    text = subLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
