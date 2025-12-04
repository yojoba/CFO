package com.example.protfoliomanagerv1.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Fade in animation for content
@OptIn(ExperimentalAnimationApi::class)
fun fadeInAnimation() = fadeIn(
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
)

// Fade out animation
@OptIn(ExperimentalAnimationApi::class)
fun fadeOutAnimation() = fadeOut(
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
)

// Slide in from bottom animation
@OptIn(ExperimentalAnimationApi::class)
fun slideInFromBottomAnimation() = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
)

// Slide out to bottom animation
@OptIn(ExperimentalAnimationApi::class)
fun slideOutToBottomAnimation() = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
)

// Scale animation for buttons
fun Modifier.scaleOnPress(pressed: Boolean) = composed {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    this.then(Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    })
}

// Shimmer effect for loading states
@Composable
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width,
        targetValue = 2 * size.width,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1A1E3D),
                Color(0xFF252A4A),
                Color(0xFF1A1E3D)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width, size.height)
        )
    )
}

// Animated number counter
@Composable
fun animateNumberAsState(
    targetValue: Double,
    label: String = "number"
): State<Double> {
    return animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = label
    ).let { remember { derivedStateOf { it.value.toDouble() } } }
}

// Pulsing animation for important elements
@Composable
fun Modifier.pulseAnimation(enabled: Boolean = true): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled) 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    this.alpha(alpha)
}

// Gradient animation for cards
@Composable
fun animatedGradientBrush(
    colors: List<Color>,
    isAnimated: Boolean = true
): Brush {
    val transition = rememberInfiniteTransition(label = "gradient")
    
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimated) 1000f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )
    
    return Brush.linearGradient(
        colors = colors,
        start = Offset(offset, offset),
        end = Offset(offset + 1000f, offset + 1000f)
    )
}

// Loading shimmer placeholder
@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .shimmerEffect()
    )
}

