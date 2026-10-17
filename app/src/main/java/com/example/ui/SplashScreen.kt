package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    
    val medallionAlpha = remember { Animatable(0f) }
    val medallionOffsetY = remember { Animatable(40f) }
    val medallionScale = remember { Animatable(0.25f) }
    
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(30f) }
    
    val masterScale = remember { Animatable(1f) }
    val masterAlpha = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    fun startExitSequence(fast: Boolean = false) {
        coroutineScope.launch {
            if (masterScale.value > 1.2f) return@launch
            if (fast) {
                launch { masterScale.animateTo(18f, tween(700, easing = CubicBezierEasing(0.68f, -0.05f, 0.85f, 0.05f))) }
                launch { masterAlpha.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
                delay(650)
                onSplashFinished()
            } else {
                launch { masterScale.animateTo(1.06f, tween(250, easing = FastOutSlowInEasing)) }
                delay(220)
                launch { masterScale.animateTo(18f, tween(1000, easing = CubicBezierEasing(0.68f, -0.05f, 0.85f, 0.05f))) }
                launch { masterAlpha.animateTo(0f, tween(850, easing = FastOutSlowInEasing)) }
                delay(950)
                onSplashFinished()
            }
        }
    }

    LaunchedEffect(Unit) {
        // Step 1: Smooth bounce in Medallion
        launch {
            medallionAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            medallionOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 1100
                    40f at 0
                    -8f at 550 using CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                    3f at 770
                    -1f at 935
                    0f at 1100
                }
            )
        }
        launch {
            medallionScale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 1100
                    0.25f at 0
                    1.1f at 550 using CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                    0.96f at 770
                    1.02f at 935
                    1.0f at 1100
                }
            )
        }

        // Step 2: Slide up Title & Punchline
        delay(450)
        launch {
            textAlpha.animateTo(1f, tween(750, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        }
        launch {
            textOffsetY.animateTo(0f, tween(850, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        }

        // Step 3: Wait until ~3.2s then trigger exit zoom
        delay(3200)
        startExitSequence(fast = false)
    }

    BackHandler {
        startExitSequence(fast = true)
    }

    val radialBackground = Brush.radialGradient(
        colors = listOf(
            Color(0xFF0F382E),
            Color(0xFF07241E),
            Color(0xFF00221A)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(radialBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                startExitSequence(fast = true)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = masterScale.value
                    scaleY = masterScale.value
                    alpha = masterAlpha.value
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Medallion with rotating orbits & aura
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .alpha(medallionAlpha.value)
                        .graphicsLayer {
                            translationY = medallionOffsetY.value * density
                            scaleX = medallionScale.value
                            scaleY = medallionScale.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Golden Aura Glow
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFF2CA50).copy(alpha = pulseGlow * 0.35f),
                                        Color(0xFFC9A741).copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Orbit Ring 1 (rotating)
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .border(1.dp, Color(0xFFF2CA50).copy(alpha = 0.3f), CircleShape)
                            .graphicsLayer { rotationZ = rotationAngle }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .offset(x = 101.dp, y = 101.dp)
                                .background(Color(0xFFFFE088), CircleShape)
                        )
                    }

                    // Central Golden Medallion Disk
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3328), Color(0xFF0D1712))
                                ),
                                shape = CircleShape
                            )
                            .border(
                                2.5.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFF3DE8E),
                                        Color(0xFFF2CA50),
                                        Color(0xFFB37C3C)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.foreground),
                            contentDescription = "Azan Time Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title and Punchline
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(textAlpha.value)
                        .graphicsLayer {
                            translationY = textOffsetY.value * density
                        }
                ) {
                    Text(
                        text = "AZAN TIME",
                        fontFamily = GraenMetalFontFamily,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = GraenMetalGoldGradient,
                            shadow = GraenMetalGoldShadow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "\"AAO ALLAH KI RAAH MEIN CHALE\"",
                        color = Color.White.copy(alpha = 0.96f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            }
        }
    }
}
