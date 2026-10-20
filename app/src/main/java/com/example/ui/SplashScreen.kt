package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import kotlinx.coroutines.delay

val GraenMetalFont = FontFamily(Font(R.font.graen_metal))

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scaleAnim"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(3500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0F382E), Color(0xFF07241E), Color(0xFF00221A)),
                    radius = 1200f
                )
            )
            .clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .scale(scaleAnim)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color(0xFF0F382E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F382E), Color(0xFF061C16))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDzv4lkY-VFUwM4ArCsKdDYhYrOEkR0yJRLn90dBsI4QVDCmJfxrhTUN_ifTBx5Sny465t4O7nfAl1ledCCfRnazsPALOQH5KieopDQ63TlhSZ5lvlj-VduoZagDuoyE1F8Mg5jYoXhllceFatdiNcmzk9LgQKpfsKkeWAk_fzt0BX_MgGEBdo3msqbUW4SoujaKzTfdGc3JVLuWIjarh0cyjYcwJ_vBQNY8dvcnFo7hzgUeIhmLUBJGo8zRYLqLJgBAGE",
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(90.dp)
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AZAN TIME",
                fontFamily = GraenMetalFont,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFF3DE8E), Color(0xFFF3D66A), Color(0xFFB37C3C))
                    ),
                    shadow = Shadow(
                        color = Color(0xFFFFE08B).copy(alpha = 0.5f),
                        offset = Offset(0f, 0f),
                        blurRadius = 16f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"AAO ALLAH KI RAAH MEIN CHALE\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
