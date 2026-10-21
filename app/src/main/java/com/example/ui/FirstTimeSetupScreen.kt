package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.MasjidItem
import com.example.data.getLocalizedArea
import com.example.data.getLocalizedName
import java.util.Locale


@Composable
fun FirstTimeSetupScreen(
    uiState: UIState,
    onSelectMasjid: (MasjidItem) -> Unit,
    onFinishSetup: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isChangeMode = onBack != null

    // Auto-refresh masajid list from Google Sheet whenever Search/Setup screen opens
    LaunchedEffect(Unit) {
        onRefresh()
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    // Starts as null so FINISH button is disabled until a slot is selected from search results
    var userSelectedMasjid by remember {
        mutableStateOf<MasjidItem?>(null)
    }

    val hasQuery = searchQuery.trim().isNotBlank()

    // System Back handling
    BackHandler {
        if (hasQuery) {
            searchQuery = ""
        } else if (onBack != null) {
            onBack()
        }
    }

    // Speech to text activity launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()?.trim()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                isSearchFocused = true
            }
        }
    }

    // Audio permission launcher for speech recognition
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchVoiceSearch(context, speechLauncher)
        } else {
            Toast.makeText(
                context,
                "Microphone permission is needed for voice search",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startVoiceInput() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchVoiceSearch(context, speechLauncher)
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Matching masajid list (only shown when user types search text)
    val matchedList = remember(searchQuery, uiState.allMasajid, uiState.language, hasQuery) {
        if (!hasQuery) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase()
            uiState.allMasajid.filter { m ->
                m.name.lowercase().contains(q) ||
                m.area.lowercase().contains(q) ||
                m.city.lowercase().contains(q) ||
                m.state.lowercase().contains(q) ||
                m.id.lowercase().contains(q) ||
                m.getLocalizedName(uiState.language).lowercase().contains(q) ||
                m.getLocalizedArea(uiState.language).lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F14))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            val photoHeight = (screenHeight * 0.48f).coerceIn(250.dp, 380.dp)

            val activeMasjid = userSelectedMasjid
            val hasSelectionChanged = activeMasjid != null && activeMasjid.id != uiState.selectedMasjid.id

            val cardHeight = if (activeMasjid != null) photoHeight else 320.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // TOP CARD: Main Logo on 1st time before selection, or Masjid Photo when selected
                Card(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color(0xFFF2CA50).copy(alpha = 0.35f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F151C)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // ALWAYS SHOW: Main App Logo & AZAN TIME Graen Metal Title!
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF14241C),
                                            Color(0xFF0F1A15),
                                            Color(0xFF0A0F14)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Golden Medallion with Main App Logo
                                Box(
                                    modifier = Modifier.size(156.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Subtle Golden Aura Glow
                                    Box(
                                        modifier = Modifier
                                            .size(180.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color(0xFFF2CA50).copy(alpha = 0.30f),
                                                        Color(0xFFC9A741).copy(alpha = 0.12f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )

                                    // Circular Golden Medallion
                                    Box(
                                        modifier = Modifier
                                            .size(148.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF1E3328), Color(0xFF0D1712))
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(
                                                BorderStroke(
                                                    2.5.dp,
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color(0xFFF3DE8E),
                                                            Color(0xFFF2CA50),
                                                            Color(0xFFB37C3C)
                                                        )
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.foreground),
                                            contentDescription = "Azan Time Logo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // AZAN TIME typography
                                val goldBrush = remember {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFF3DE8E),
                                            Color(0xFFF3D66A),
                                            Color(0xFFB37C3C)
                                        ),
                                        startY = 0f,
                                        endY = 120f
                                    )
                                }
                                Text(
                                    text = "AZAN TIME",
                                    fontFamily = GraenMetalFontFamily,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        brush = goldBrush,
                                        shadow = GraenMetalGoldShadow
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                )
                                // PUNCHLINE
                                val punchline = when (uiState.language) {
                                    "ur" -> "آپ کی نمازوں کا بااعتماد ساتھی"
                                    "hi" -> "आपकी नमाज़ों का सच्चा साथी"
                                    else -> "Your Faithful Prayer Companion"
                                }
                                Text(
                                    text = punchline,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFFF2CA50).copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SEARCH BAR
                SearchBarContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        isSearchFocused = true
                    },
                    onSearchFocused = { isSearchFocused = true },
                    onVoiceInput = {
                        isSearchFocused = true
                        startVoiceInput()
                    }
                )

                // SEARCH RESULTS (If query entered)
                if (hasQuery) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SearchResultsList(
                        matchedList = matchedList,
                        searchQuery = searchQuery,
                        selectedMasjidId = activeMasjid?.id,
                        language = uiState.language,
                        onSelect = { item -> userSelectedMasjid = item }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ACTION BUTTON: FINISH
                val isFinishEnabled = activeMasjid != null
                Button(
                    onClick = {
                        val chosen = activeMasjid ?: uiState.selectedMasjid
                        onSelectMasjid(chosen)
                        onFinishSetup(chosen.id)
                    },
                    enabled = isFinishEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFinishEnabled) Color(0xFFF2CA50) else Color(0xFF1E2836).copy(alpha = 0.50f),
                        contentColor = if (isFinishEnabled) Color(0xFF241A00) else Color(0xFF64748B),
                        disabledContainerColor = Color(0xFF1E2836).copy(alpha = 0.50f),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = if (isFinishEnabled) ButtonDefaults.buttonElevation(defaultElevation = 4.dp) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "FINISH",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Reusable Search Bar Component
 */
@Composable
private fun SearchBarContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchFocused: () -> Unit,
    onVoiceInput: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(
                Color(0xFF0F172A).copy(alpha = 0.90f),
                RoundedCornerShape(18.dp)
            )
            .border(
                1.dp,
                Color(0xFFF2CA50).copy(alpha = 0.45f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onSearchFocused() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFFF2CA50),
                modifier = Modifier
                    .padding(start = 8.dp, end = 10.dp)
                    .size(22.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search Masjid Name, Area or ID...",
                        color = Color(0xFF94A3B8).copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFFF2CA50)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                onSearchFocused()
                            }
                        }
                )
            }

            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFFD0C5AF).copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Color(0xFFF2CA50).copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        Color(0xFFF2CA50).copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onVoiceInput() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice search",
                    tint = Color(0xFFF2CA50),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Reusable Search Results List
 */
@Composable
private fun SearchResultsList(
    matchedList: List<MasjidItem>,
    searchQuery: String,
    selectedMasjidId: String?,
    language: String,
    onSelect: (MasjidItem) -> Unit
) {
    if (matchedList.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A).copy(alpha = 0.90f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFF2CA50).copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mosque,
                    contentDescription = null,
                    tint = Color(0xFFF2CA50).copy(alpha = 0.65f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "No masjid found for \"${searchQuery.trim()}\"",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Try searching by Area name or 9-digit Masjid ID",
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MATCHING MASAJID (${matchedList.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFF2CA50),
                letterSpacing = 0.8.sp
            )
            Text(
                text = "Tap to select",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        matchedList.forEach { item ->
            val isSel = item.id == selectedMasjidId
            Card(
                onClick = { onSelect(item) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSel) {
                        Color(0xFF14291E).copy(alpha = 0.95f)
                    } else {
                        Color(0xFF0F151C).copy(alpha = 0.85f)
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSel) Color(0xFF22C55E) else Color(0xFFF2CA50).copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isSel) Color(0xFF166534) else Color(0xFF1E2836).copy(alpha = 0.6f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mosque,
                            contentDescription = null,
                            tint = if (isSel) Color(0xFF86EFAC) else Color(0xFFF2CA50),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.getLocalizedName(language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isSel) Color(0xFF86EFAC) else Color.White
                        )
                        val itemLoc = listOf(
                            item.getLocalizedArea(language),
                            item.city,
                            item.state
                        ).filter { it.isNotBlank() }.joinToString(", ")
                        if (itemLoc.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = itemLoc,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFD0C5AF).copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF1E2836).copy(alpha = 0.8f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                Color(0xFFF2CA50).copy(alpha = 0.35f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${item.id}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFF2CA50)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper to launch speech recognition intent
 */
private fun launchVoiceSearch(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak Masjid Name, Area or ID..."
            )
        }
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Speech recognition is not available on this device",
            Toast.LENGTH_SHORT
        ).show()
    }
}
