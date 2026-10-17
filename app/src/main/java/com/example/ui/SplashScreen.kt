package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    
    // Animation states
    val medallionAlpha = remember { Animatable(0f) }
    val medallionScale = remember { Animatable(0.25f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(30f) }
    val masterScale = remember { Animatable(1f) }
    val masterAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Bounce in Medallion
        launch {
            medallionAlpha.animateTo(1f, tween(400))
            medallionScale.animateTo(1f, keyframes {
                durationMillis = 1100
                0.25f at 0
                1.1f at 550 using CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                1.0f at 1100
            })
        }
        
        // Slide up Text
        delay(450)
        launch {
            titleAlpha.animateTo(1f, tween(750))
            titleOffsetY.animateTo(0f, tween(850, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        }

        // Wait then Zoom out
        delay(2500)
        masterScale.animateTo(18f, tween(1000, easing = CubicBezierEasing(0.68f, -0.05f, 0.85f, 0.05f)))
        masterAlpha.animateTo(0f, tween(800))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00221A))
            .graphicsLayer {
                scaleX = masterScale.value
                scaleY = masterScale.value
                alpha = masterAlpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        /*
        Image(
            painter = painterResource(id = R.drawable.azan_logo_medallion_1792232502522),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    alpha = medallionAlpha.value
                    scaleX = medallionScale.value
                    scaleY = medallionScale.value
                }
        )
        */
        Box(modifier = Modifier.size(200.dp).background(Color(0xFFF2CA50), shape = androidx.compose.foundation.shape.CircleShape))

        // Title placeholder
        Text(
            text = "AZAN TIME",
            color = Color(0xFFF2CA50),
            fontSize = 36.sp,
            modifier = Modifier
                .padding(top = 20.dp)
                .graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffsetY.value
                }
        )
    }
}
