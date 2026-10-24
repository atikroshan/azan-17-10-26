package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated Curved Bottom Navigation Bar
 * Inspired by Shmelt Studios Figma Menu Animation:
 * - Fluid curved cutout/notch along the top edge of the navigation bar
 * - Floating elevated circular active indicator with generous padding and space for cutout shape
 */
@Composable
fun AnimatedCurvedNavBar(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    supportLabel: String,
    homeLabel: String,
    ramazanLabel: String,
    isRamazanEnabled: Boolean,
    onRamazanTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = when (selectedTab) {
        "support" -> 0
        "home" -> 1
        "ramazan" -> 2
        else -> 1
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val totalWidth = maxWidth
        val tabWidth = totalWidth / 3

        // Animate the horizontal center position of the active circular indicator
        val animatedIndicatorCenterX by animateDpAsState(
            targetValue = tabWidth * (selectedIndex + 0.5f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "indicatorPos"
        )

        val density = LocalDensity.current
        val barHeight = 72.dp
        val barColor = Color(0xFF0A1610)
        val borderColor = Color(0xFFF3DE8E).copy(alpha = 0.28f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + 22.dp)
        ) {
            // 1. Curved Background with Cut Shape Notch
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.BottomCenter)
            ) {
                val w = size.width
                val h = size.height
                val cx = with(density) { animatedIndicatorCenterX.toPx() }
                val r = 32.dp.toPx() // generous padding radius for cutout shape
                val dipDepth = 18.dp.toPx() // cut shape depth

                // Path for the background fill
                val fillPath = Path().apply {
                    moveTo(0f, 0f)
                    val curveStartX = (cx - r * 1.5f).coerceAtLeast(0f)
                    lineTo(curveStartX, 0f)

                    // Smooth Bezier dip down and up for the cut shape
                    cubicTo(
                        cx - r * 0.95f, 0f,
                        cx - r * 0.7f, dipDepth,
                        cx, dipDepth
                    )
                    cubicTo(
                        cx + r * 0.7f, dipDepth,
                        cx + r * 0.95f, 0f,
                        (cx + r * 1.5f).coerceAtMost(w), 0f
                    )

                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                drawPath(fillPath, color = barColor)

                // Top border stroke along the cut shape
                val borderPath = Path().apply {
                    moveTo(0f, 0f)
                    val curveStartX = (cx - r * 1.5f).coerceAtLeast(0f)
                    lineTo(curveStartX, 0f)
                    cubicTo(
                        cx - r * 0.95f, 0f,
                        cx - r * 0.7f, dipDepth,
                        cx, dipDepth
                    )
                    cubicTo(
                        cx + r * 0.7f, dipDepth,
                        cx + r * 0.95f, 0f,
                        (cx + r * 1.5f).coerceAtMost(w), 0f
                    )
                    lineTo(w, 0f)
                }

                drawPath(
                    borderPath,
                    color = borderColor,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Sliding Elevated Circular Indicator Bubble
            Box(
                modifier = Modifier
                    .offset(x = animatedIndicatorCenterX - 27.dp, y = 2.dp)
                    .size(54.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = Color(0xFFF3DE8E))
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFF0B8),
                                Color(0xFFF3DE8E),
                                Color(0xFFD4A034)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFFFF4C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Active Icon inside the floating bubble
                val activeIcon = when (selectedIndex) {
                    0 -> Icons.Filled.HeadsetMic
                    1 -> Icons.Filled.Home
                    else -> Icons.Filled.NightsStay
                }
                Icon(
                    imageVector = activeIcon,
                    contentDescription = null,
                    tint = Color(0xFF071711),
                    modifier = Modifier.size(26.dp)
                )
            }

            // 3. Tab Items & Labels Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Support
                NavBarItem(
                    selected = selectedIndex == 0,
                    icon = Icons.Outlined.HeadsetMic,
                    label = supportLabel,
                    onClick = { onSelectTab("support") },
                    testTag = "nav_support_button",
                    modifier = Modifier.weight(1f)
                )

                // Tab 1: Home
                NavBarItem(
                    selected = selectedIndex == 1,
                    icon = Icons.Outlined.Home,
                    label = homeLabel,
                    onClick = { onSelectTab("home") },
                    testTag = "nav_home_button",
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Ramazan
                NavBarItem(
                    selected = selectedIndex == 2,
                    icon = Icons.Outlined.NightsStay,
                    label = ramazanLabel,
                    onClick = onRamazanTap,
                    testTag = "nav_ramazan_button",
                    enabled = isRamazanEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconScale"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        label = "labelAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(bottom = 10.dp)
    ) {
        if (!selected) {
            Spacer(modifier = Modifier.height(2.dp))
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) Color(0xFFF3DE8E).copy(alpha = 0.70f) else Color(0xFFF3DE8E).copy(alpha = 0.35f),
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale)
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            // When selected, elevated circle takes the icon position, leaving clean padded space for text label
            Spacer(modifier = Modifier.height(30.dp))
        }

        Text(
            text = label,
            fontSize = if (selected) 11.5.sp else 10.5.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) Color(0xFFF3DE8E) else Color(0xFFF3DE8E).copy(alpha = labelAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
