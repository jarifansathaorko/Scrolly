package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SleekTextPrimary
import com.example.ui.theme.ScrollyAmber
import com.example.ui.theme.ScrollyCream
import com.example.ui.theme.ScrollyDarkBg
import com.example.ui.theme.ScrollyGold
import com.example.ui.theme.ScrollyMascotPink
import com.example.ui.theme.ScrollyMascotPinkDark
import com.example.ui.theme.ScrollyRed
import kotlin.math.cos
import kotlin.math.sin

enum class ScrollyMascotState(
    val title: String,
    val quote: String,
    val minScrolls: Int,
    val maxScrolls: Int,
    val baseColor: Color,
    val cheekColor: Color
) {
    FRESH(
        title = "Super Fresh",
        quote = "Brain is crystal clear! Ready to conquer the day.",
        minScrolls = 0,
        maxScrolls = 20,
        baseColor = Color(0xFFFF9E8E),
        cheekColor = Color(0xFFFF6350)
    ),
    CHILL(
        title = "Cruising",
        quote = "Cruising along nicely. Keep it intentional!",
        minScrolls = 21,
        maxScrolls = 50,
        baseColor = Color(0xFFFFAE75),
        cheekColor = Color(0xFFFF7A45)
    ),
    DAZED(
        title = "Dazed",
        quote = "You're doing okay. Maybe touch some grass 😌",
        minScrolls = 51,
        maxScrolls = 100,
        baseColor = Color(0xFFFFBC60),
        cheekColor = Color(0xFFEE8A2A)
    ),
    FRIED(
        title = "Brain Fried",
        quote = "Thumb is getting overtime. Your focus is melting! ⚡",
        minScrolls = 101,
        maxScrolls = 200,
        baseColor = Color(0xFFE58867),
        cheekColor = Color(0xFFC75530)
    ),
    COOKED(
        title = "Completely Cooked",
        quote = "Your brain is well-done. Put the phone down! 🍳",
        minScrolls = 201,
        maxScrolls = 500,
        baseColor = Color(0xFFB5705C),
        cheekColor = Color(0xFF8A4030)
    ),
    NUCLEAR(
        title = "Nuclear Brainrot",
        quote = "Emergency! Dopamine overload catastrophic level! 💀",
        minScrolls = 501,
        maxScrolls = Int.MAX_VALUE,
        baseColor = Color(0xFF82544B),
        cheekColor = Color(0xFF5A2E26)
    );

    companion object {
        fun fromScrollCount(scrolls: Int): ScrollyMascotState {
            return entries.firstOrNull { scrolls in it.minScrolls..it.maxScrolls } ?: NUCLEAR
        }
    }
}

@Composable
fun ScrollyCharacter(
    scrollCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    animated: Boolean = true
) {
    val state = ScrollyMascotState.fromScrollCount(scrollCount)

    val bounceY: Float
    val eyeSquint: Float

    if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "mascot_idle")
        val animatedBounce by infiniteTransition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce"
        )
        val animatedSquint by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "squint"
        )
        bounceY = animatedBounce
        eyeSquint = animatedSquint
    } else {
        bounceY = 0f
        eyeSquint = 1f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            withTransform({
                translate(left = 0f, top = bounceY)
            }) {
                // Draw Mascot Brain Body
                drawBrainBody(cx, cy, w, h, state)

                // Draw Facial Expressions based on state
                drawEyesAndFace(cx, cy, w, h, state, eyeSquint)
            }
        }
    }
}

private fun DrawScope.drawBrainBody(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    state: ScrollyMascotState
) {
    val brainPath = Path().apply {
        val r = w * 0.32f
        // Start top center
        moveTo(cx, cy - r * 0.85f)
        // Top right lobe
        cubicTo(cx + r * 0.6f, cy - r * 1.15f, cx + r * 1.25f, cy - r * 0.45f, cx + r * 1.1f, cy)
        // Bottom right lobe
        cubicTo(cx + r * 1.35f, cy + r * 0.65f, cx + r * 0.7f, cy + r * 1.15f, cx + r * 0.25f, cy + r * 0.95f)
        // Bottom center lobe dip
        cubicTo(cx, cy + r * 1.1f, cx - r * 0.25f, cy + r * 0.95f, cx - r * 0.25f, cy + r * 0.95f)
        // Bottom left lobe
        cubicTo(cx - r * 0.7f, cy + r * 1.15f, cx - r * 1.35f, cy + r * 0.65f, cx - r * 1.1f, cy)
        // Top left lobe
        cubicTo(cx - r * 1.25f, cy - r * 0.45f, cx - r * 0.6f, cy - r * 1.15f, cx, cy - r * 0.85f)
        close()
    }

    // Body Fill
    drawPath(path = brainPath, color = state.baseColor)

    // Inner brain fold wrinkle grooves
    val foldColor = state.cheekColor.copy(alpha = 0.55f)
    drawArc(
        color = foldColor,
        startAngle = 180f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - w * 0.22f, cy - h * 0.24f),
        size = Size(w * 0.16f, h * 0.16f),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
    drawArc(
        color = foldColor,
        startAngle = 230f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx + w * 0.06f, cy - h * 0.24f),
        size = Size(w * 0.16f, h * 0.16f),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )

    // Cheeks
    val cheekRadius = w * 0.055f
    drawCircle(
        color = state.cheekColor.copy(alpha = 0.8f),
        radius = cheekRadius,
        center = Offset(cx - w * 0.25f, cy + h * 0.12f)
    )
    drawCircle(
        color = state.cheekColor.copy(alpha = 0.8f),
        radius = cheekRadius,
        center = Offset(cx + w * 0.25f, cy + h * 0.12f)
    )
}

private fun DrawScope.drawEyesAndFace(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    state: ScrollyMascotState,
    squintFactor: Float
) {
    val eyeSpacing = w * 0.16f
    val eyeY = cy + h * 0.01f
    val eyeRadius = w * 0.075f

    when (state) {
        ScrollyMascotState.FRESH, ScrollyMascotState.CHILL -> {
            // Big sparkling cute anime eyes
            listOf(cx - eyeSpacing, cx + eyeSpacing).forEach { ex ->
                drawCircle(color = SleekTextPrimary, radius = eyeRadius, center = Offset(ex, eyeY))
                // Big light reflection highlight
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius * 0.42f,
                    center = Offset(ex - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f)
                )
                // Small secondary highlight
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius * 0.2f,
                    center = Offset(ex + eyeRadius * 0.35f, eyeY + eyeRadius * 0.35f)
                )
            }
            // Sweet smile
            val smilePath = Path().apply {
                moveTo(cx - w * 0.06f, cy + h * 0.14f)
                quadraticBezierTo(cx, cy + h * 0.20f, cx + w * 0.06f, cy + h * 0.14f)
            }
            drawPath(path = smilePath, color = SleekTextPrimary, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
        ScrollyMascotState.DAZED -> {
            // Wide-eyed slightly dazed look
            listOf(cx - eyeSpacing, cx + eyeSpacing).forEach { ex ->
                drawCircle(color = SleekTextPrimary, radius = eyeRadius * 1.1f, center = Offset(ex, eyeY))
                drawCircle(color = Color.White, radius = eyeRadius * 0.3f, center = Offset(ex, eyeY - eyeRadius * 0.2f))
            }
            // Flat tiny 'o' mouth
            drawCircle(
                color = SleekTextPrimary,
                radius = w * 0.035f,
                center = Offset(cx, cy + h * 0.15f)
            )
        }
        ScrollyMascotState.FRIED -> {
            // Dizzy spiral eyes
            listOf(cx - eyeSpacing, cx + eyeSpacing).forEach { ex ->
                drawCircle(color = SleekTextPrimary, radius = eyeRadius, center = Offset(ex, eyeY), style = Stroke(width = 3.5f))
                drawCircle(color = SleekTextPrimary, radius = eyeRadius * 0.5f, center = Offset(ex, eyeY), style = Stroke(width = 3.5f))
            }
            // Wobbly mouth
            val wobblyPath = Path().apply {
                moveTo(cx - w * 0.07f, cy + h * 0.15f)
                quadraticBezierTo(cx - w * 0.03f, cy + h * 0.12f, cx, cy + h * 0.15f)
                quadraticBezierTo(cx + w * 0.03f, cy + h * 0.18f, cx + w * 0.07f, cy + h * 0.15f)
            }
            drawPath(path = wobblyPath, color = SleekTextPrimary, style = Stroke(width = 4f, cap = StrokeCap.Round))
            // Sweat drop on temple
            drawCircle(color = Color(0xFF6EC6FF), radius = w * 0.03f, center = Offset(cx + w * 0.32f, cy - h * 0.1f))
        }
        ScrollyMascotState.COOKED, ScrollyMascotState.NUCLEAR -> {
            // "X" eyes for cooked/nuclear state
            listOf(cx - eyeSpacing, cx + eyeSpacing).forEach { ex ->
                val r = eyeRadius * 0.8f
                drawLine(
                    color = SleekTextPrimary,
                    start = Offset(ex - r, eyeY - r),
                    end = Offset(ex + r, eyeY + r),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = SleekTextPrimary,
                    start = Offset(ex + r, eyeY - r),
                    end = Offset(ex - r, eyeY + r),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
            // Stressed open tongue mouth
            val openMouth = Path().apply {
                moveTo(cx - w * 0.06f, cy + h * 0.15f)
                quadraticBezierTo(cx, cy + h * 0.24f, cx + w * 0.06f, cy + h * 0.15f)
                close()
            }
            drawPath(path = openMouth, color = SleekTextPrimary)
        }
    }
}
