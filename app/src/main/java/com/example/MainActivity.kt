package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.SplashScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.FirstTimeSetupScreen
import com.example.data.GoogleSheetMasjidSync
import com.example.util.AppUpdateManager
import com.example.util.UpdateInfo
import com.example.ui.UpdateDialog
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AzanDatabase
import com.example.data.AzanRepository
import com.example.data.PreferencesRepository
import com.example.service.AlarmScheduler
import com.example.ui.AzanViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.core.rememberInfiniteTransition
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloat
import com.example.data.MasjidItem
import com.example.data.getLocalizedName
import com.example.data.getLocalizedArea
import com.example.data.getLocalizedCity
import com.example.ui.MasjidSelectorDropdown

class MainActivity : ComponentActivity() {

    private var mainViewModel: AzanViewModel? = null

    override fun onResume() {
        super.onResume()
        mainViewModel?.refreshDate()
    }

    private fun wakeAndShowOverLock() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                km?.requestDismissKeyguard(this, null)
            }
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeAndShowOverLock()
        
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            com.example.service.AzanForegroundService.initFromPrefs(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            com.example.worker.PrayerWorkScheduler.scheduleDailySync(applicationContext)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Handle permission grant
                }
                
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val db = remember { AzanDatabase.getDatabase(context) }
            val repo = remember { AzanRepository(context, db.azanDao(), db.prayerLogDao()) }
            val prefs = remember { PreferencesRepository(context) }
            val scheduler = remember { AlarmScheduler(context) }

            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AzanViewModel(context.applicationContext, repo, prefs, scheduler) as T
                }
            }
            val viewModel: AzanViewModel = viewModel(factory = factory)
            mainViewModel = viewModel

            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                
                val strings = when (uiState.language) {
                    "hi" -> HindiStrings
                    "ur" -> UrduStrings
                    else -> EnglishStrings
                }

                val isAlarmIntent = intent.getBooleanExtra("FROM_ALARM", false)
                var showSplashScreen by remember(uiState.splashShown) { mutableStateOf(!isAlarmIntent && !uiState.splashShown) }

                // Lock fontScale to 1.0f to guarantee exact, identical typography rendering across all mobile devices
                val currentDensity = LocalDensity.current
                val fixedDensity = remember(currentDensity.density) {
                    Density(
                        density = currentDensity.density,
                        fontScale = 1.0f
                    )
                }

                CompositionLocalProvider(
                    LocalDensity provides fixedDensity,
                    LocalAppStrings provides strings
                ) {
                    AnimatedVisibility(
                        visible = showSplashScreen,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(400))
                    ) {
                        SplashScreen(
                            onSplashFinished = {
                                viewModel.setSplashShown(true)
                                showSplashScreen = false
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = !showSplashScreen,
                        enter = fadeIn(tween(400)),
                        exit = fadeOut(tween(200))
                    ) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            AzanScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        wakeAndShowOverLock()
    }

}

@Composable
fun AzanScreen(viewModel: AzanViewModel, uiState: com.example.ui.UIState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isRamazanActive = remember {
        val current = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(2027, java.util.Calendar.FEBRUARY, 5, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        current.timeInMillis >= target.timeInMillis
    }

    var selectedTab by remember { mutableStateOf("home") } // "support", "home", or "ramazan"
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var showMasjidSelectorScreen by remember { mutableStateOf(false) }
    var ramazanTapCount by remember { mutableIntStateOf(0) }
    var forcedRamazanActive by remember { mutableStateOf(false) }
    val effectiveRamazanActive = isRamazanActive || forcedRamazanActive

    var showUpdateDialog by remember { mutableStateOf(false) }
    var availableUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val update = AppUpdateManager.checkForUpdate()
        if (update != null && update.hasUpdate) {
            availableUpdateInfo = update
            showUpdateDialog = true
        }
    }

    LaunchedEffect(effectiveRamazanActive) {
        if (!effectiveRamazanActive && selectedTab == "ramazan") {
            selectedTab = "home"
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.syncGoogleSheet()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val homeLabel = when (uiState.language) {
        "ur" -> "ہوم"
        "hi" -> "होम"
        else -> "Home"
    }
    val supportLabel = when (uiState.language) {
        "ur" -> "سپورٹ"
        "hi" -> "सपोर्ट"
        else -> "Support"
    }
    val ramazanLabel = when (uiState.language) {
        "ur" -> "رمضان"
        "hi" -> "रमज़ान"
        else -> "Ramazan"
    }

    if (!uiState.isSetupCompleted || showMasjidSelectorScreen) {
        FirstTimeSetupScreen(
            uiState = uiState,
            onSelectMasjid = { masjid -> viewModel.selectMasjid(masjid.id) },
            onFinishSetup = { masjidId ->
                viewModel.completeSetup(masjidId)
                showMasjidSelectorScreen = false
            },
            onBack = if (uiState.isSetupCompleted) { { showMasjidSelectorScreen = false } } else null,
            onRefresh = { viewModel.syncGoogleSheet() }
        )
    } else if (showAdminPanel) {
        BackHandler { showAdminPanel = false }
        AdminPanelScreen(
            viewModel = viewModel,
            uiState = uiState,
            onBack = { showAdminPanel = false }
        )
    } else {
        if (showUpdateDialog && availableUpdateInfo != null) {
            UpdateDialog(
                updateInfo = availableUpdateInfo!!,
                language = uiState.language,
                onDismiss = { showUpdateDialog = false }
            )
        }
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Black,
            bottomBar = {
            Surface(
                color = Color(0xFF0A1610).copy(alpha = 0.96f),
                contentColor = Color(0xFFF3DE8E),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(0.5.dp, Color(0xFFF3DE8E).copy(alpha = 0.2f)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Support Button (Circle shape)
                    NavCircleItem(
                        selected = selectedTab == "support",
                        onClick = { selectedTab = "support" },
                        icon = if (selectedTab == "support") Icons.Filled.HeadsetMic else Icons.Outlined.HeadsetMic,
                        label = supportLabel,
                        testTag = "nav_support_button"
                    )

                    // Center: Home Button (Circle shape)
                    NavCircleItem(
                        selected = selectedTab == "home",
                        onClick = {
                            selectedTab = "home"
                            viewModel.selectToday()
                        },
                        icon = if (selectedTab == "home") Icons.Filled.Home else Icons.Outlined.Home,
                        label = homeLabel,
                        testTag = "nav_home_button"
                    )

                    // Right: Ramazan Section Button (Circle shape) with 5-tap activation
                    NavCircleItem(
                        selected = selectedTab == "ramazan",
                        onClick = {
                            ramazanTapCount++
                            if (!effectiveRamazanActive && ramazanTapCount >= 5) {
                                forcedRamazanActive = true
                                android.widget.Toast.makeText(context, "Ramazan section activated!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            if (effectiveRamazanActive) {
                                selectedTab = "ramazan"
                            } else {
                                val msg = when (uiState.language) {
                                    "ur" -> "رمضان سیکشن 5 فروری 2027 سے دستیاب ہوگا"
                                    "hi" -> "रमज़ान सेक्शन 5 फ़रवरी 2027 से शुरू होगा"
                                    else -> "Ramazan section will be available from 5 Feb 2027"
                                }
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = if (selectedTab == "ramazan") Icons.Filled.NightsStay else Icons.Outlined.NightsStay,
                        label = ramazanLabel,
                        testTag = "nav_ramazan_button",
                        enabled = effectiveRamazanActive
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .appBackground()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "screenTransition"
            ) { tab ->
                when (tab) {
                    "support" -> {
                        com.example.ui.SupportScreen(
                            uiState = uiState,
                            onBack = { selectedTab = "home" },
                            onLangSelect = { viewModel.setLanguage(it) },
                            onMasjidSubmitted = { viewModel.syncGoogleSheet() },
                            onChangeMasjid = { showMasjidSelectorScreen = true },
                            onSelectMasjid = { masjid -> viewModel.selectMasjid(masjid.id) }
                        )
                    }
                    "ramazan" -> {
                        com.example.ui.RamazanScreenContent(
                            uiState = uiState,
                            onLanguageSelect = { viewModel.setLanguage(it) }
                        )
                    }
                    else -> {
                        AzanHomeContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            onOpenAdminLogin = { showAdminLoginDialog = true },
                            onChangeMasjid = { showMasjidSelectorScreen = true }
                        )
                    }
                }
            }
        }
    }
    }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            selectedMasjid = uiState.selectedMasjid,
            allMasajid = uiState.allMasajid,
            onDismiss = { showAdminLoginDialog = false },
            onLoginSuccess = {
                showAdminLoginDialog = false
                showAdminPanel = true
            }
        )
    }
}

data class HighlightedSlotInfo(
    val index: Int,
    val systemName: String,
    val displayName: String,
    val azanTime: String,
    val jammatTime: String,
    val isOngoing: Boolean,
    val icon: ImageVector
)

fun getHighlightedSlotInfo(
    uiState: com.example.ui.UIState,
    strings: com.example.ui.theme.AppStrings
): HighlightedSlotInfo {
    val s = uiState.selectedMasjid
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val isToday = uiState.selectedDate.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            uiState.selectedDate.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    val isFri = if (isToday) (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) else (uiState.selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
    val timings = uiState.todayTimings

    val fAzan = s.fajrAzanFixed ?: timings?.fajr ?: "05:40"
    val dAzan = if (isFri) (uiState.customJumahAzan ?: s.jumahAzanTime.ifBlank { "12:30" }) else (s.zoharAzanFixed ?: "13:15")
    val aAzan = s.asrAzanFixed ?: timings?.asr ?: "17:17"
    val mAzan = s.maghribAzanFixed ?: timings?.maghrib ?: "18:10"
    val iAzan = s.ishaAzanFixed ?: timings?.isha ?: "19:50"

    val fJammat = getEffectiveJammatTime("Fajr", fAzan, uiState.customJammatTimes, isFri, s)
    val dJammat = getEffectiveJammatTime("Dhuhr", dAzan, uiState.customJammatTimes, isFri, s)
    val aJammat = getEffectiveJammatTime("Asr", aAzan, uiState.customJammatTimes, isFri, s)
    val mJammat = getEffectiveJammatTime("Maghrib", mAzan, uiState.customJammatTimes, isFri, s)
    val iJammat = getEffectiveJammatTime("Isha", iAzan, uiState.customJammatTimes, isFri, s)

    fun toMin(t: String): Int {
        val p = t.split(":")
        return if (p.size >= 2) (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0) else 0
    }

    val fJammatCutoff = toMin(fJammat) + 15
    val dJammatCutoff = toMin(dJammat) + 15
    val aJammatCutoff = toMin(aJammat) + 15
    val mJammatCutoff = toMin(mJammat) + 15
    val iJammatCutoff = toMin(iJammat) + 15

    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    val activeIdx = when {
        currentMinutes <= fJammatCutoff -> 0
        currentMinutes <= dJammatCutoff -> 1
        currentMinutes <= aJammatCutoff -> 2
        currentMinutes <= mJammatCutoff -> 3
        currentMinutes <= iJammatCutoff -> 4
        else -> 0
    }

    val dhuhrLabel = if (isFri) strings.jumah.uppercase() else strings.dhuhr.uppercase()
    val names = listOf(
        Pair("Fajr", strings.fajr.uppercase()),
        Pair("Dhuhr", dhuhrLabel),
        Pair("Asr", strings.asr.uppercase()),
        Pair("Maghrib", strings.maghrib.uppercase()),
        Pair("Isha", strings.isha.uppercase())
    )
    val azanTimes = listOf(fAzan, dAzan, aAzan, mAzan, iAzan)
    val jammatTimes = listOf(fJammat, dJammat, aJammat, mJammat, iJammat)
    val cutoffs = listOf(fJammatCutoff, dJammatCutoff, aJammatCutoff, mJammatCutoff, iJammatCutoff)
    val icons = listOf(
        Icons.Outlined.WbTwilight,
        Icons.Outlined.WbSunny,
        Icons.Outlined.WbCloudy,
        Icons.Outlined.NightsStay,
        Icons.Outlined.Nightlight
    )

    val activeAzanMin = toMin(azanTimes[activeIdx])
    val activeCutoff = cutoffs[activeIdx]
    val isOngoing = isToday && (currentMinutes in activeAzanMin..activeCutoff)

    return HighlightedSlotInfo(
        index = activeIdx,
        systemName = names[activeIdx].first,
        displayName = names[activeIdx].second,
        azanTime = azanTimes[activeIdx],
        jammatTime = jammatTimes[activeIdx],
        isOngoing = isOngoing,
        icon = icons[activeIdx]
    )
}

@Composable
fun SlidingArrowHandle(
    isPulledDown: Boolean,
    onClick: () -> Unit,
    scale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrowFloat")
    val animatedOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPulledDown) -2.5f else 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowOffsetY"
    )

    Box(
        modifier = modifier
            .size((34f * scale).dp)
            .clip(CircleShape)
            .background(Color(0xFFF3DE8E)) // Home button background color
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPulledDown) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (isPulledDown) "Slide Up" else "Slide Down",
            tint = Color(0xFF0F2618), // Dark contrast icon tint
            modifier = Modifier
                .size((18f * scale).dp)
                .offset(y = (animatedOffsetY * scale).dp)
        )
    }
}

@Composable
fun AzanHomeContent(
    viewModel: AzanViewModel,
    uiState: com.example.ui.UIState,
    onOpenAdminLogin: () -> Unit = {},
    onChangeMasjid: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photoUrl = uiState.selectedMasjid.photoUrl

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        val baseTopPadding = 205.dp
        val navGap = 16.dp
        val collapsedSheetHeight = 265.dp
        val collapsedSheetHeightPx = with(density) { collapsedSheetHeight.toPx() }

        val screenHeightPx = with(density) { screenHeight.toPx() }
        val baseTopPaddingPx = with(density) { baseTopPadding.toPx() }
        val navGapPx = with(density) { navGap.toPx() }

        // When slid down: highlighted slot and content section collapse down leaving clean 16dp gap above navigation bar
        val targetCollapsedTopPx = (screenHeightPx - navGapPx - collapsedSheetHeightPx).coerceAtLeast(baseTopPaddingPx)
        val maxPullDownPx = (targetCollapsedTopPx - baseTopPaddingPx).coerceAtLeast(0f)
        val maxPullDownDistance = with(density) { maxPullDownPx.toDp() }
        val targetCollapsedTopDp = with(density) { targetCollapsedTopPx.toDp() }

        var isPulledDown by rememberSaveable { mutableStateOf(true) }
        val sheetOffsetY = remember { Animatable(if (isPulledDown) maxPullDownPx else 0f) }

        LaunchedEffect(isPulledDown, maxPullDownPx) {
            val target = if (isPulledDown) maxPullDownPx else 0f
            if (sheetOffsetY.value != target) {
                if (kotlin.math.abs(sheetOffsetY.value - target) > 40f && !sheetOffsetY.isRunning) {
                    sheetOffsetY.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                } else {
                    sheetOffsetY.snapTo(target)
                }
            }
        }

        var currentMinutesTick by remember {
            mutableIntStateOf(
                Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).let {
                    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
                }
            )
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000L)
                val m = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).let {
                    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
                }
                if (m != currentMinutesTick) {
                    currentMinutesTick = m
                }
            }
        }
        val strings = LocalAppStrings.current
        val highlightedInfo = remember(uiState, strings, currentMinutesTick) {
            getHighlightedSlotInfo(uiState, strings)
        }

        val dragGestureModifier = Modifier.pointerInput(maxPullDownPx) {
            detectVerticalDragGestures(
                onDragStart = { },
                onDragEnd = {
                    coroutineScope.launch {
                        val current = sheetOffsetY.value
                        val target = if (current > maxPullDownPx * 0.35f) {
                            isPulledDown = true
                            maxPullDownPx
                        } else {
                            isPulledDown = false
                            0f
                        }
                        sheetOffsetY.animateTo(
                            targetValue = target,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        sheetOffsetY.animateTo(if (isPulledDown) maxPullDownPx else 0f)
                    }
                },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        val newOffset = (sheetOffsetY.value + dragAmount).coerceIn(0f, maxPullDownPx)
                        sheetOffsetY.snapTo(newOffset)
                        isPulledDown = (newOffset > maxPullDownPx * 0.4f)
                    }
                }
            )
        }

        val nestedScrollConnection = remember(maxPullDownPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    if (delta < 0 && sheetOffsetY.value > 0f) {
                        val newOffset = (sheetOffsetY.value + delta).coerceIn(0f, maxPullDownPx)
                        val consumed = sheetOffsetY.value - newOffset
                        coroutineScope.launch {
                            sheetOffsetY.snapTo(newOffset)
                            isPulledDown = (newOffset > maxPullDownPx * 0.4f)
                        }
                        return Offset(0f, -consumed)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    if (delta > 0 && source == NestedScrollSource.UserInput) {
                        val newOffset = (sheetOffsetY.value + delta).coerceIn(0f, maxPullDownPx)
                        val consumedY = newOffset - sheetOffsetY.value
                        coroutineScope.launch {
                            sheetOffsetY.snapTo(newOffset)
                            isPulledDown = (newOffset > maxPullDownPx * 0.4f)
                        }
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val target = if (sheetOffsetY.value > maxPullDownPx * 0.35f || available.y > 500f) {
                        isPulledDown = true
                        maxPullDownPx
                    } else {
                        isPulledDown = false
                        0f
                    }
                    sheetOffsetY.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    return Velocity.Zero
                }
            }
        }

        // --- BACK SCREEN ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            val homePhoto = if (photoUrl.isNotBlank()) photoUrl else R.drawable.img_makkah_light
            var langRowHeightPx by remember { mutableIntStateOf(0) }
            val langRowHeightDp = with(density) { langRowHeightPx.toDp() }
            val photoTopPadding = if (langRowHeightDp > 0.dp) langRowHeightDp + 20.dp else 65.dp

            LanguageCirclesRow(
                currentLang = uiState.language,
                onLangSelect = { viewModel.setLanguage(it) },
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 5.dp, bottom = 0.dp)
                    .onSizeChanged { langRowHeightPx = it.height }
            )

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(homePhoto)
                    .error(R.drawable.img_makkah_light)
                    .placeholder(R.drawable.img_makkah_light)
                    .crossfade(false)
                    .build(),
                contentDescription = "Selected Masjid",
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = photoTopPadding)
                    .wrapContentHeight()
                    .heightIn(max = targetCollapsedTopDp + 10.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()

                        // Ultra-smooth transparent fade effect at bottom of photo (no harsh cut, natural feathering)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.00f to Color.White,
                                0.45f to Color.White,
                                0.60f to Color.White.copy(alpha = 0.95f),
                                0.72f to Color.White.copy(alpha = 0.80f),
                                0.82f to Color.White.copy(alpha = 0.55f),
                                0.90f to Color.White.copy(alpha = 0.30f),
                                0.96f to Color.White.copy(alpha = 0.10f),
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        }

        // --- FRONT SCREEN (screen over screen: part from time till the bottom) ---
        val currentOffsetDp = with(density) { sheetOffsetY.value.toDp() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isPulledDown) it.wrapContentHeight() else it.fillMaxHeight() }
                .padding(
                    top = (baseTopPadding + currentOffsetDp - 20.dp).coerceAtLeast(0.dp),
                    bottom = if (isPulledDown) 0.dp else 10.dp
                )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (isPulledDown) it.wrapContentHeight() else it.fillMaxHeight() }
                    .padding(top = 20.dp)
                    .nestedScroll(nestedScrollConnection),
                shape = if (isPulledDown) RoundedCornerShape(24.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color(0xFF06140D),
                border = null,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (isPulledDown) it.wrapContentHeight() else it.fillMaxSize() }
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            top = 28.dp,
                            bottom = if (isPulledDown) 14.dp else 12.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                // Top header: Part from time till the bottom of the clock header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(dragGestureModifier)
                ) {
                    ClockDisplay(
                        selectedDate = uiState.selectedDate,
                        language = uiState.language,
                        todayTimings = uiState.todayTimings,
                        onPrevDay = { viewModel.previousDay() },
                        onNextDay = { viewModel.nextDay() },
                        onToday = { viewModel.selectToday() },
                        onOpenAdminLogin = onOpenAdminLogin,
                        scale = 1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // When home page slides down: show only the highlighted prayer card (AZAN & JAMMAT) matching attached screenshot
                if (isPulledDown) {
                    Spacer(modifier = Modifier.height(6.dp))

                    val infiniteTransition = rememberInfiniteTransition(label = "pullDownTrimTransition")
                    val flareProgress by infiniteTransition.animateFloat(
                        initialValue = -1f,
                        targetValue = 2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2500, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pullDownFlare"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pullDownPulse"
                    )
                    val islamicGlowColors = listOf(
                        Color(0xFFF59E0B),
                        Color(0xFF10B981),
                        Color(0xFFFBBF24),
                        Color(0xFF34D399),
                        Color(0xFFF59E0B)
                    )
                    
                    // Header for Prayer Name & Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = highlightedInfo.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = highlightedInfo.displayName,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (highlightedInfo.isOngoing) strings.ongoing else strings.next,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            style = androidx.compose.ui.text.TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeight = 11.sp
                            ),
                            modifier = Modifier
                                .background(Color(0xFFFFD700), RoundedCornerShape(3.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    // Card with same animation as AzanSlot when slid up (shining border + glowing background)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.05f * pulseAlpha),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .drawWithContent {
                                drawContent()
                                val strokeWidth = 1.5.dp.toPx()
                                val halfStroke = strokeWidth / 2f
                                val stroke = Stroke(width = strokeWidth)
                                val corner = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx() - halfStroke)
                                val topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke)
                                val borderSize = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
                                
                                val center = flareProgress * size.width
                                val span = size.width * 0.7f
                                
                                // Sharp shining animated border (Same as slide up animation)
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            islamicGlowColors[0],
                                            islamicGlowColors[1],
                                            islamicGlowColors[2],
                                            islamicGlowColors[3],
                                            Color.Transparent
                                        ),
                                        startX = center - span,
                                        endX = center + span
                                    ),
                                    topLeft = topLeft,
                                    size = borderSize,
                                    style = stroke,
                                    cornerRadius = corner
                                )
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF07140E))
                    ) {
                        HighlightedAzanJammatDisplay(
                            azanTime24 = highlightedInfo.azanTime,
                            jammatTime24 = highlightedInfo.jammatTime,
                            compact = false,
                            textScale = 1.0f,
                            timeFontSize = 30.sp,
                            timeSuffixSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                } else {
                    // When pulled up (default): full accordion will show
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AzanList(
                            viewModel = viewModel,
                            uiState = uiState,
                            singleSlotMode = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "v${BuildConfig.VERSION_NAME} • Powered by @tek",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Absolute positioning fix for the drag handle: drawn on top of the sheet's top boundary
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 3.dp)
                .then(dragGestureModifier),
            contentAlignment = Alignment.Center
        ) {
            SlidingArrowHandle(
                isPulledDown = isPulledDown,
                scale = 1.0f,
                onClick = {
                    coroutineScope.launch {
                        val target = if (sheetOffsetY.value > 10f) 0f else maxPullDownPx
                        isPulledDown = (target > 0f)
                        sheetOffsetY.animateTo(
                            targetValue = target,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
            )
        }
    }
}
}


@Composable
fun LanguageCirclesRow(
    currentLang: String,
    onLangSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnglishActive = currentLang == "en"

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // English: morphs between circle (34x34) and rectangle (70x34, corner 8dp) with animation
        LanguageAnimatedButton(
            shortLabel = "E",
            activeLabel = "ENGLISH",
            langCode = "en",
            currentLang = currentLang,
            isOtherLarger = false,
            onLangSelect = onLangSelect
        )

        // Hindi: slightly larger (40x40 circle) when English is active
        LanguageAnimatedButton(
            shortLabel = "ह",
            activeLabel = "हिंदी",
            langCode = "hi",
            currentLang = currentLang,
            isOtherLarger = isEnglishActive,
            onLangSelect = onLangSelect
        )

        // Urdu: slightly larger (40x40 circle) when English is active
        LanguageAnimatedButton(
            shortLabel = "ر",
            activeLabel = "اردو",
            langCode = "ur",
            currentLang = currentLang,
            isOtherLarger = isEnglishActive,
            onLangSelect = onLangSelect
        )
    }
}

@Composable
fun LanguageAnimatedButton(
    shortLabel: String,
    activeLabel: String,
    langCode: String,
    currentLang: String,
    isOtherLarger: Boolean,
    onLangSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = currentLang == langCode

    // Morph target dimensions
    val targetWidth = when {
        isSelected -> 70.dp
        isOtherLarger -> 40.dp
        else -> 34.dp
    }
    val targetHeight = when {
        isSelected -> 34.dp
        isOtherLarger -> 40.dp
        else -> 34.dp
    }
    // Corner radius animation: rectangle = 8dp, circle = 20dp (for 40dp) or 17dp (for 34dp)
    val targetCorner = when {
        isSelected -> 17.dp // Smoothly morphs from circle shape
        isOtherLarger -> 20.dp
        else -> 17.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "lang_btn_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "lang_btn_height"
    )
    val animatedCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "lang_btn_corner"
    )

    val shape = RoundedCornerShape(animatedCorner)

    Box(
        modifier = modifier
            .size(width = animatedWidth, height = animatedHeight)
            .clip(shape)
            .background(
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(Color(0xFFF3DE8E), Color(0xFFD49B37))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF131926), Color(0xFF0F1522))
                    )
                }
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFD700) else Color(0xFF26334A),
                shape = shape
            )
            .clickable { onLangSelect(langCode) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isSelected) activeLabel else shortLabel,
            fontSize = when {
                isSelected -> if (langCode == "ur") 13.sp else 12.5.sp
                langCode == "ur" -> if (isOtherLarger) 18.sp else 16.sp
                langCode == "hi" -> if (isOtherLarger) 17.sp else 14.5.sp
                else -> 13.5.sp
            },
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) Color(0xFF0C101B) else Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LanguageCircleButton(
    label: String,
    langCode: String,
    currentLang: String,
    onLangSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LanguageAnimatedButton(
        shortLabel = label,
        activeLabel = label,
        langCode = langCode,
        currentLang = currentLang,
        isOtherLarger = false,
        onLangSelect = onLangSelect,
        modifier = modifier
    )
}

@Composable
fun LanguageToggleRow(currentLang: String, onLangSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1522), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageButton("English", "en", currentLang, onLangSelect, Modifier.weight(1f))
        LanguageButton("हिंदी", "hi", currentLang, onLangSelect, Modifier.weight(1f))
        LanguageButton("اردو", "ur", currentLang, onLangSelect, Modifier.weight(1f))
    }
}

@Composable
fun LanguageButton(
    label: String,
    langCode: String,
    currentLang: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = currentLang == langCode
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(34.dp)
            .background(
                brush = if (isActive) {
                    Brush.verticalGradient(
                        listOf(Color(0xFFF3DE8E), Color(0xFFB37C3C))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF131926), Color(0xFF131926))
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(langCode) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = if (isActive) 14.sp else 12.sp,
            color = if (isActive) Color(0xFF0C101B) else Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

fun parseTimeToCalendar(timeStr: String, baseCal: Calendar): Calendar {
    val parts = timeStr.split(":")
    val targetCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata")).apply {
        timeInMillis = baseCal.timeInMillis
        if (parts.size == 2) {
            set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
            set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    return targetCal
}

@Composable
fun ClockDisplay(
    selectedDate: Calendar,
    language: String,
    todayTimings: com.example.data.AzanTiming?,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onOpenAdminLogin: () -> Unit = {},
    scale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        }
    }

    val strings = LocalAppStrings.current

    val locale = remember(language) {
        when (language) {
            "hi" -> Locale("hi", "IN")
            "ur" -> Locale("ur")
            else -> Locale.ENGLISH
        }
    }

    val timeFormat = remember(locale) { 
        SimpleDateFormat("hh:mm", locale).apply { 
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") 
        } 
    }
    val amPmFormat = remember(locale) { 
        SimpleDateFormat("a", locale).apply { 
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") 
        } 
    }
    val dateFormat = remember(locale) { 
        SimpleDateFormat("EEEE, dd MMM yyyy", locale).apply { 
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") 
        } 
    }
    
    val secondsFormat = remember(locale) { 
        SimpleDateFormat("ss", locale).apply { 
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") 
        } 
    }
    val seconds = secondsFormat.format(currentTime.time)

    val today = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
    val isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    val isAfterMaghrib = if (isToday && todayTimings != null) {
        val mToday = parseTimeToCalendar(todayTimings.maghrib, currentTime)
        currentTime.after(mToday)
    } else {
        false
    }

    val islamicDateText = remember(selectedDate, isAfterMaghrib, language) {
        try {
            val gYear = selectedDate.get(Calendar.YEAR)
            val gMonth = selectedDate.get(Calendar.MONTH) + 1
            val gDay = selectedDate.get(Calendar.DAY_OF_MONTH)
            
            val baseLocalDate = java.time.LocalDate.of(gYear, gMonth, gDay)
            val localDate = if (isAfterMaghrib) baseLocalDate else baseLocalDate.minusDays(1)
            val hijrahDate = java.time.chrono.HijrahDate.from(localDate)
            val hYear = hijrahDate.get(java.time.temporal.ChronoField.YEAR)
            val hMonth = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            val hDay = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            
            val urduDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
            val hDayUrdu = hDay.toString().map { if (it in '0'..'9') urduDigits[it - '0'] else it }.joinToString("")
            val hYearUrdu = hYear.toString().map { if (it in '0'..'9') urduDigits[it - '0'] else it }.joinToString("")

            val monthNameUrdu = when (hMonth) {
                1 -> "محرم"
                2 -> "صفر"
                3 -> "ربیع الاول"
                4 -> "ربیع الثانی"
                5 -> "جمادی الاول"
                6 -> "جمادی الثانی"
                7 -> "رجب"
                8 -> "شعبان"
                9 -> "رمضان"
                10 -> "شوال"
                11 -> "ذوالقعدہ"
                12 -> "ذوالحجہ"
                else -> ""
            }

            val urduDateFormatted = "$hDayUrdu $monthNameUrdu $hYearUrdu ہجری"

            when (language) {
                "ur" -> "اسلامی تاریخ: $urduDateFormatted"
                "hi" -> {
                    val monthNameHi = when (hMonth) {
                        1 -> "मुहर्रम"
                        2 -> "सफ़र"
                        3 -> "रबी अल-अव्वल"
                        4 -> "रबी अउ-थानी"
                        5 -> "जमाद अल-अव्वल"
                        6 -> "जमाद अउ-थानी"
                        7 -> "रजब"
                        8 -> "शबान"
                        9 -> "रमज़ान"
                        10 -> "शव्वाल"
                        11 -> "ज़ु अल-क़ादा"
                        12 -> "ज़ु अल-हिज्जह"
                        else -> ""
                    }
                    "इस्लामिक तारीख: $hDay $monthNameHi $hYear हिजरी"
                }
                else -> {
                    val monthNameEn = when (hMonth) {
                        1 -> "Muharram"
                        2 -> "Safar"
                        3 -> "Rabi-ul-Awwal"
                        4 -> "Rabi-us-Sani"
                        5 -> "Jamadi-ul-Awwal"
                        6 -> "Jamadi-us-Sani"
                        7 -> "Rajab"
                        8 -> "Shaban"
                        9 -> "Ramadan"
                        10 -> "Shawaal"
                        11 -> "Zul-Qadah"
                        12 -> "Zul-Hijjah"
                        else -> ""
                    }
                    "Islamic Date: $hDay $monthNameEn $hYear AH"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 0.dp, bottom = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val timeString = timeFormat.format(currentTime.time)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    timeString.forEachIndexed { index, char ->
                        AnimatedContent(
                            targetState = char,
                            transitionSpec = {
                                (slideInVertically { height -> height } + fadeIn()) togetherWith
                                        (slideOutVertically { height -> -height } + fadeOut())
                            },
                            label = "TimeCharAnimation$index"
                        ) { c ->
                            Text(
                                text = c.toString(),
                                fontSize = (52f * scale).sp,
                                fontWeight = FontWeight.Black,
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFF3DE8E), Color(0xFFB37C3C))
                                    )
                                ),
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width((6f * scale).dp))
                Column(
                    modifier = Modifier.widthIn(min = (36f * scale).dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row {
                        seconds.forEachIndexed { index, char ->
                            AnimatedContent(
                                targetState = char,
                                transitionSpec = {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> -height } + fadeOut())
                                },
                                label = "SecondCharAnimation$index"
                            ) { c ->
                                Text(
                                    text = c.toString(),
                                    fontSize = (20f * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    Text(
                        text = amPmFormat.format(currentTime.time).uppercase(),
                        fontSize = (14f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                }
                Spacer(modifier = Modifier.width((14f * scale).dp))
                Box(
                    modifier = Modifier
                        .offset(y = (-3).dp)
                        .clip(RoundedCornerShape((6f * scale).dp))
                        .clickable { onOpenAdminLogin() }
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = RoundedCornerShape((6f * scale).dp)
                        )
                        .padding(horizontal = (8f * scale).dp, vertical = (4f * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "IST",
                        fontSize = (11f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height((4f * scale).dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isFriday = selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                val dateColor = if (isFriday) Color(0xFF25D366) else Color.White
                val dayName = strings.getDayOfWeekFull(selectedDate.get(Calendar.DAY_OF_WEEK)).uppercase()
                val dayNum = selectedDate.get(Calendar.DAY_OF_MONTH)
                val monthShort = strings.getMonthShortName(selectedDate.get(Calendar.MONTH) + 1).uppercase()
                val yearNum = selectedDate.get(Calendar.YEAR)
                val formattedDate = "$dayName, $dayNum $monthShort $yearNum"
                Text(
                    text = formattedDate,
                    fontSize = (13f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = dateColor,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            if (islamicDateText.isNotEmpty()) {
                Spacer(modifier = Modifier.height((4f * scale).dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = islamicDateText,
                        fontSize = ((if (language == "ur") 14f else 12f) * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = if (language == "ur") 0.sp else 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (!isToday) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clickable { onToday() }
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = strings.goToToday,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AzanList(
    viewModel: AzanViewModel,
    uiState: com.example.ui.UIState,
    isAccordionClosed: Boolean = false,
    singleSlotMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currentUiState by rememberUpdatedState(uiState)
    val context = androidx.compose.ui.platform.LocalContext.current

    val curMasjid = uiState.selectedMasjid
    val todayCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val isTodayDate = uiState.selectedDate.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            uiState.selectedDate.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    val isFridayToday = if (isTodayDate) {
        todayCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    } else {
        uiState.selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    }
    val dhuhrLabel = if (isFridayToday) strings.jumah else strings.dhuhr.uppercase()

    val fajrAzan = curMasjid.fajrAzanFixed ?: uiState.todayTimings?.fajr ?: "05:40"
    val zoharAzan = if (isFridayToday) {
        uiState.customJumahAzan ?: curMasjid.jumahAzanTime.ifBlank { "12:30" }
    } else {
        curMasjid.zoharAzanFixed ?: "13:15"
    }
    val asrAzan = curMasjid.asrAzanFixed ?: uiState.todayTimings?.asr ?: "17:17"
    val maghribAzan = curMasjid.maghribAzanFixed ?: uiState.todayTimings?.maghrib ?: "18:10"
    val ishaAzan = curMasjid.ishaAzanFixed ?: uiState.todayTimings?.isha ?: "19:50"

    fun computeCurrentNext(): Int {
        val isAudioPlaying = com.example.service.AzanForegroundService.isPlayingAzan.value
        val lastAudioIdx = com.example.service.AzanForegroundService.lastAudioPrayerIndex.value

        if (isAudioPlaying && lastAudioIdx in 0..5) {
            return lastAudioIdx
        }

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val s = currentUiState.selectedMasjid
        val isFri = if (isTodayDate) (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) else (currentUiState.selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
        val f = s.fajrAzanFixed ?: currentUiState.todayTimings?.fajr ?: "05:40"
        val d = if (isFri) (currentUiState.customJumahAzan ?: s.jumahAzanTime.ifBlank { "12:30" }) else (s.zoharAzanFixed ?: "13:15")
        val a = s.asrAzanFixed ?: currentUiState.todayTimings?.asr ?: "17:17"
        val m = s.maghribAzanFixed ?: currentUiState.todayTimings?.maghrib ?: "18:10"
        val i = s.ishaAzanFixed ?: currentUiState.todayTimings?.isha ?: "19:50"

        val fJammat = getEffectiveJammatTime("Fajr", f, currentUiState.customJammatTimes, isFri, s)
        val dJammat = getEffectiveJammatTime("Dhuhr", d, currentUiState.customJammatTimes, isFri, s)
        val aJammat = getEffectiveJammatTime("Asr", a, currentUiState.customJammatTimes, isFri, s)
        val mJammat = getEffectiveJammatTime("Maghrib", m, currentUiState.customJammatTimes, isFri, s)
        val iJammat = getEffectiveJammatTime("Isha", i, currentUiState.customJammatTimes, isFri, s)

        fun toMin(t: String): Int {
            val p = t.split(":")
            return if (p.size >= 2) (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0) else 0
        }

        val fJammatCutoff = toMin(fJammat) + 15
        val dJammatCutoff = toMin(dJammat) + 15
        val aJammatCutoff = toMin(aJammat) + 15
        val mJammatCutoff = toMin(mJammat) + 15
        val iJammatCutoff = toMin(iJammat) + 15

        return when {
            currentMinutes <= fJammatCutoff -> 0 // Through Fajr Jamaat + 15 min -> Fajr (Index 0)
            currentMinutes <= dJammatCutoff -> 1 // Through Zohr Jamaat + 15 min -> Zohr (Index 1)
            currentMinutes <= aJammatCutoff -> 2 // Through Asr Jamaat + 15 min -> Asr (Index 2)
            currentMinutes <= mJammatCutoff -> 3 // Through Maghrib Jamaat + 15 min -> Maghrib (Index 3)
            currentMinutes <= iJammatCutoff -> 4 // Through Isha Jamaat + 15 min -> Isha (Index 4)
            else -> 0 // After Isha Jamaat + 15 min at night -> Loops to tomorrow's Fajr (Index 0)
        }
    }

    var currentNextIndex by remember(fajrAzan, zoharAzan, asrAzan, maghribAzan, ishaAzan, currentUiState.customJammatTimes, currentUiState.selectedMasjid) {
        mutableIntStateOf(computeCurrentNext())
    }
    val initialMinuteKey = remember {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val day = cal.get(Calendar.DAY_OF_YEAR)
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        String.format(java.util.Locale.US, "INIT-%02d:%02d-%d", h, m, day)
    }
    var lastTriggeredMinute by remember { mutableStateOf(initialMinuteKey) }
    var lastObservedDayOfYear by remember { mutableIntStateOf(Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).get(Calendar.DAY_OF_YEAR)) }
    
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            if (todayDayOfYear != lastObservedDayOfYear) {
                lastObservedDayOfYear = todayDayOfYear
                viewModel.refreshDate()
            }

            currentNextIndex = computeCurrentNext()

            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMin = cal.get(Calendar.MINUTE)
            val currentTimeStr = String.format(java.util.Locale.US, "%02d:%02d", currentHour, currentMin)

            // In-app prayer time audio playback trigger (plays user audio once and closes)
            val isNowFriday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val liveZoharAzan = if (isNowFriday) {
                currentUiState.customJumahAzan ?: curMasjid.jumahAzanTime.ifBlank { "12:30" }
            } else {
                curMasjid.zoharAzanFixed ?: "13:15"
            }
            val prayerPairs = listOf(
                "Fajr" to (fajrAzan to currentUiState.fajrEnabled),
                "Dhuhr" to (liveZoharAzan to currentUiState.dhuhrEnabled),
                "Asr" to (asrAzan to currentUiState.asrEnabled),
                "Maghrib" to (maghribAzan to currentUiState.maghribEnabled),
                "Isha" to (ishaAzan to currentUiState.ishaEnabled)
            )
            for ((name, pair) in prayerPairs) {
                val (timeStr, isEnabled) = pair
                if (isEnabled && timeStr == currentTimeStr) {
                    val triggerKey = "$name-$currentTimeStr-$todayDayOfYear"
                    if (lastTriggeredMinute != triggerKey) {
                        lastTriggeredMinute = triggerKey
                        try {
                            val serviceIntent = Intent(context, com.example.service.AzanForegroundService::class.java).apply {
                                putExtra("AZAN_NAME", name)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            delay(1000L)
        }
    }

    val slots = listOf(
        Triple("Fajr", strings.fajr.uppercase(), fajrAzan),
        Triple("Dhuhr", dhuhrLabel, zoharAzan),
        Triple("Asr", strings.asr.uppercase(), asrAzan),
        Triple("Maghrib", strings.maghrib.uppercase(), maghribAzan),
        Triple("Isha", strings.isha.uppercase(), ishaAzan),
        Triple("Tahajjud", strings.tahajjud.uppercase(), "01:30")
    )
    val toggles = listOf(
        uiState.fajrEnabled, uiState.dhuhrEnabled, uiState.asrEnabled, uiState.maghribEnabled, uiState.ishaEnabled, false // no audio for tahajjud
    )

    // Icons assigned representing the visual path of the day/night
    val icons = listOf(
        Icons.Outlined.WbTwilight, // Fajr
        Icons.Outlined.WbSunny,    // Dhuhr
        Icons.Outlined.WbCloudy,   // Asr
        Icons.Outlined.NightsStay, // Maghrib (moon with cloud)
        Icons.Outlined.Nightlight, // Isha
        Icons.Outlined.StarRate    // Tahajjud
    )

    val today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val isToday = uiState.selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            uiState.selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    val prayedList = listOf(
        uiState.fajrPrayed,
        uiState.dhuhrPrayed,
        uiState.asrPrayed,
        uiState.maghribPrayed,
        uiState.ishaPrayed,
        uiState.tahajjudPrayed
    )

    var expandedIndex by remember(currentNextIndex, isAccordionClosed) {
        mutableIntStateOf(if (isAccordionClosed) -1 else currentNextIndex)
    }

    val isPlayingAzan by com.example.service.AzanForegroundService.isPlayingAzan.collectAsState()

    LaunchedEffect(currentNextIndex, isToday, isAccordionClosed) {
        if (isAccordionClosed) {
            expandedIndex = -1
        } else if (isToday && currentNextIndex != -1) {
            expandedIndex = currentNextIndex
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isPlayingAzan) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEAB308))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Azan Playing",
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Azan Playing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Playing once • Auto closing",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            com.example.service.AzanForegroundService.stopService(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val displayedIndices = if (singleSlotMode) {
            val safeIdx = if (currentNextIndex in slots.indices) currentNextIndex else 0
            listOf(safeIdx)
        } else {
            slots.indices.toList()
        }

        displayedIndices.forEach { index ->
            val triple = slots[index]
            val rawEffectiveJammat = if (triple.first != "Tahajjud") {
                getEffectiveJammatTime(triple.first, triple.third, uiState.customJammatTimes, isFridayToday, uiState.selectedMasjid)
            } else {
                ""
            }
            val jammatTimeStr = if (rawEffectiveJammat.isNotBlank() && rawEffectiveJammat.contains(":")) {
                rawEffectiveJammat
            } else if (triple.first != "Tahajjud") {
                val p = triple.third.split(":")
                val h = p.getOrNull(0)?.toIntOrNull() ?: 5
                val m = p.getOrNull(1)?.toIntOrNull() ?: 0
                val off = when (triple.first.lowercase()) {
                    "fajr" -> 35
                    "maghrib" -> 15
                    "isha" -> 20
                    else -> 25
                }
                val tot = h * 60 + m + off
                String.format(java.util.Locale.US, "%02d:%02d", (tot / 60) % 24, tot % 60)
            } else {
                ""
            }
            
            // Check if prayer is unlocked: active ONLY 10 minutes after Jammat time!
            val (isPrayerUnlocked, unlockTimeFormatted) = if (isToday && triple.first != "Tahajjud") {
                checkPrayerUnlocked(jammatTimeStr, triple.third, now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")), systemName = triple.first)
            } else {
                Pair(true, "")
            }

            // Ongoing if current time is between Azan and Jamaat + 15 min
            val currentMinutes = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
            val azanMin = triple.third.split(":").let { if (it.size >= 2) (it[0].toIntOrNull() ?: 0) * 60 + (it[1].toIntOrNull() ?: 0) else 0 }
            val jammatMin = jammatTimeStr.split(":").let { if (it.size >= 2) (it[0].toIntOrNull() ?: 0) * 60 + (it[1].toIntOrNull() ?: 0) else 0 }
            val jammatCutoff = (if (jammatMin > 0) jammatMin else azanMin + 20) + 15

            val isSlotActive = if (singleSlotMode) true else (isToday && (index == currentNextIndex))
            val isOngoing = isSlotActive && (currentMinutes in azanMin..jammatCutoff)
            val isNext = isSlotActive && !isOngoing

            val isExpanded = if (singleSlotMode) false else (index == expandedIndex)
            
            AzanSlot(
                systemName = triple.first,
                displayName = triple.second,
                time = triple.third,
                jammatTime = jammatTimeStr,
                enabled = toggles[index],
                isNext = isNext,
                isOngoing = isOngoing,
                icon = icons[index],
                prayed = prayedList[index],
                isPrayerUnlocked = isPrayerUnlocked,
                expanded = isExpanded,
                showAudioToggle = if (singleSlotMode) false else (triple.first != "Tahajjud"),
                showExpandIcon = !singleSlotMode,
                onHeaderClick = {
                    if (!singleSlotMode) {
                        expandedIndex = if (isExpanded) -1 else index
                    }
                },
                modifier = Modifier.animateContentSize(),
                onToggle = { enabled -> viewModel.toggleAzan(triple.first, enabled) },
                onPrayedToggle = {
                    if (isToday && !prayedList[index]) {
                        if (!isPrayerUnlocked) {
                            val jammatDisplay = formatTo12Hour(if (jammatTimeStr.isNotBlank()) jammatTimeStr else triple.third)
                            val msg = when (uiState.language) {
                                "ur" -> "${triple.second} کی جماعت $jammatDisplay پر ہے۔ حاضری کا نشان جماعت کے 10 منٹ بعد ($unlockTimeFormatted پر) فعال ہوگا۔"
                                "hi" -> "${triple.second} जमात $jammatDisplay बजे है। टिक मार्क जमात के 10 मिनट बाद ($unlockTimeFormatted पर) चालू होगा。"
                                else -> "${triple.second} jammat is at $jammatDisplay. Tick mark will unlock at $unlockTimeFormatted (10 min after jammat)."
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            return@AzanSlot
                        }
                    }
                    viewModel.togglePrayerPrayed(triple.first)
                },
                showPrayerToggle = false
            )
        }
    }
}

fun getRakatSegments(prayerName: String, strings: com.example.ui.theme.AppStrings, isJumuah: Boolean = false): List<Pair<String, Int>> {
    return when (prayerName) {
        "Fajr" -> listOf(
            Pair(strings.sunnat, 2),
            Pair(strings.farz, 2)
        )
        "Dhuhr" -> if (isJumuah) {
            listOf(
                Pair(strings.sunnat, 4),
                Pair(strings.farz, 2),
                Pair(strings.sunnat, 4),
                Pair(strings.sunnat, 2),
                Pair(strings.nafil, 2)
            )
        } else {
            listOf(
                Pair(strings.sunnat, 4),
                Pair(strings.farz, 4),
                Pair(strings.sunnat, 2),
                Pair(strings.nafil, 2)
            )
        }
        "Asr" -> listOf(
            Pair(strings.sunnat, 4),
            Pair(strings.farz, 4)
        )
        "Maghrib" -> listOf(
            Pair(strings.farz, 3),
            Pair(strings.sunnat, 2),
            Pair(strings.nafil, 2)
        )
        "Isha" -> listOf(
            Pair(strings.sunnat, 4),
            Pair(strings.farz, 4),
            Pair(strings.sunnat, 2),
            Pair(strings.nafil, 2),
            Pair(strings.witr, 3),
            Pair(strings.nafil, 2)
        )
        "Tahajjud" -> listOf(
            Pair(strings.nafil, 2)
        )
        else -> emptyList()
    }
}

@Composable
fun AzanSlot(
    systemName: String,
    displayName: String,
    time: String,
    jammatTime: String = "",
    enabled: Boolean,
    isNext: Boolean,
    isOngoing: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    prayed: Boolean,
    isPrayerUnlocked: Boolean = true,
    expanded: Boolean,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAudioToggle: Boolean = true,
    showExpandIcon: Boolean = true,
    onToggle: (Boolean) -> Unit,
    onPrayedToggle: () -> Unit,
    showPrayerToggle: Boolean = true
) {
    // ...
    val isHighlighted = isNext || isOngoing
    // ...
    // Text("NEXT") logic inside AzanSlot changed to use strings.ongoing if isOngoing is true
    // And AzanJammatDisplay(..., isNext = isNext, isOngoing = isOngoing)
    // ...

    val infiniteTransition = rememberInfiniteTransition(label = "trimTransition")
    
    val flareProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flare"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val tintColor = MaterialTheme.colorScheme.primary
    val strings = LocalAppStrings.current

    val islamicGlowColors = listOf(
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFFFBBF24),
        Color(0xFF34D399),
        Color(0xFFF59E0B)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onHeaderClick() }
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isHighlighted) listOf(
                        Color.White.copy(alpha = 0.05f * pulseAlpha),
                        Color.White.copy(alpha = 0.02f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color(0xFF0E131F).copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .drawWithContent {
                drawContent()
                if (isHighlighted) {
                    val strokeWidth = 1.5.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    val stroke = Stroke(width = strokeWidth)
                    val corner = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx() - halfStroke)
                    val topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke)
                    val borderSize = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
                    
                    val center = flareProgress * size.width
                    val span = size.width * 0.7f
                    
                    // Sharp shining animated border (Celestial Islamic theme)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                islamicGlowColors[0],
                                islamicGlowColors[1],
                                islamicGlowColors[2],
                                islamicGlowColors[3],
                                Color.Transparent
                            ),
                            startX = center - span,
                            endX = center + span
                        ),
                        topLeft = topLeft,
                        size = borderSize,
                        style = stroke,
                        cornerRadius = corner
                    )
                } else {
                    // Glass border for inactive slots
                    val strokeWidth = 1.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f),
                        topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx() - halfStroke)
                    )
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (isHighlighted && !showExpandIcon) 0.dp else 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = if (isHighlighted) (if (!showExpandIcon) 8.dp else 10.dp) else 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = if (!showExpandIcon) Modifier else Modifier.weight(1f).padding(end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Glow icon wrapper
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isHighlighted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color(0xFF1A2234),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = displayName,
                            tint = if (isHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = displayName,
                            fontSize = if (isHighlighted) {
                                if (systemName == "Maghrib") 18.sp else if (!showExpandIcon) 20.sp else 22.sp
                            } else 16.sp,
                            fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Bold,
                            color = if (isHighlighted) MaterialTheme.colorScheme.secondary else TextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isHighlighted) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isOngoing) strings.ongoing else strings.next,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                style = androidx.compose.ui.text.TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    ),
                                    lineHeight = 10.sp
                                ),
                                modifier = Modifier
                                    .background(Color(0xFFFFD700), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isHighlighted && !showExpandIcon && systemName != "Tahajjud") {
                        // Image 2 exact single-row layout
                        HighlightedAzanJammatDisplay(
                            azanTime24 = time,
                            jammatTime24 = jammatTime,
                            compact = true
                        )
                    } else if (!isHighlighted) {
                        if (systemName == "Tahajjud") {
                            PrayerTimeDisplay(time24 = time, isNext = false)
                        } else {
                            AzanJammatDisplay(
                                azanTime24 = time,
                                jammatTime24 = jammatTime,
                                isNext = false,
                                isOngoing = false
                            )
                        }
                    } else if (systemName == "Tahajjud") {
                        PrayerTimeDisplay(time24 = time, isNext = true)
                    }
                    
                    if (showPrayerToggle) {
                        IconButton(onClick = onPrayedToggle, modifier = Modifier.size(32.dp)) {
                            if (prayed) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Marked as Prayed",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else if (!isPrayerUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = "Locked until 10 min after jammat",
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Mark as Prayed",
                                    tint = TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    
                    if (showAudioToggle) {
                        IconButton(onClick = { onToggle(!enabled) }, modifier = Modifier.size(32.dp)) {
                            if (enabled) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "Toggle Audio",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .drawWithContent {
                                             drawCircle(
                                                 color = Color.Red,
                                                 radius = size.minDimension / 2,
                                                 style = Stroke(width = 2.dp.toPx())
                                             )
                                             drawLine(
                                                 color = Color.Red,
                                                 start = Offset(4.dp.toPx(), 4.dp.toPx()),
                                                 end = Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                                                 strokeWidth = 2.dp.toPx()
                                             )
                                             drawContent()
                                         },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Toggle Audio",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    if (showExpandIcon) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle Rak'at Accordion",
                            tint = if (isHighlighted) MaterialTheme.colorScheme.secondary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            if (isHighlighted && showExpandIcon && systemName != "Tahajjud") {
                HighlightedAzanJammatDisplay(
                    azanTime24 = time,
                    jammatTime24 = jammatTime,
                    compact = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 14.dp)
                )
            }
            
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                val isJumuah = systemName == "Dhuhr" && (displayName.equals("JUMAH", ignoreCase = true) || displayName.equals(strings.jumah, ignoreCase = true))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .background(Color(0xFF0F1522), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    val segments = getRakatSegments(systemName, strings, isJumuah)
                    val totalRakat = segments.sumOf { it.second }
                    
                    val typeHeader = strings.prayerType
                    val countHeader = strings.rakats

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Table Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF162035), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = typeHeader,
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = countHeader,
                                modifier = Modifier.width(80.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.End,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Data rows below
                        segments.forEachIndexed { idx, segment ->
                            val color = when {
                                segment.first.contains(strings.sunnat) -> Color(0xFF00C853)
                                segment.first == strings.farz -> Color(0xFFFFD600)
                                segment.first == strings.nafil -> Color(0xFF00B0FF)
                                segment.first == strings.witr -> Color(0xFFAA00FF)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) Color(0xFF0C1220) else Color(0xFF0F1522))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(color, RoundedCornerShape(2.dp))
                                    )
                                    Text(
                                        text = segment.first,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextColor
                                    )
                                }
                                Text(
                                    text = "${segment.second}",
                                    modifier = Modifier.width(80.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextColor,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        // Total Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF162035), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.total,
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "$totalRakat",
                                modifier = Modifier.width(80.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.appBackground(): Modifier {
    return this.background(Color.Black)
}

// Clean solid background without decorative overlays as requested
fun Modifier.islamicStarBackground(primaryColor: Color): Modifier = this



fun formatTo12Hour(time24: String): String {
    if (time24 == "--:--" || !time24.contains(":")) return time24
    val parts = time24.split(":")
    if (parts.size < 2) return time24
    val hour = parts[0].toIntOrNull() ?: return time24
    val minute = parts[1].toIntOrNull() ?: return time24
    
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(java.util.Locale.US, "%02d:%02d %s", displayHour, minute, suffix)
}

fun calculateJammatTime(systemName: String, azanTime24: String, isFriday: Boolean = false): String {
    if (azanTime24 == "--:--" || !azanTime24.contains(":")) return "--:--"
    val parts = azanTime24.split(":")
    if (parts.size != 2) return azanTime24
    val h = parts[0].toIntOrNull() ?: return azanTime24
    val m = parts[1].toIntOrNull() ?: return azanTime24

    if (systemName == "Dhuhr" && isFriday) {
        return "13:30" // 01:30 PM Jum'ah Jamaat
    }

    val offsetMinutes = when (systemName) {
        "Fajr" -> 25
        "Dhuhr" -> 20
        "Asr" -> 20
        "Maghrib" -> 10
        "Isha" -> 20
        else -> 0
    }
    val totalMins = (h * 60 + m + offsetMinutes) % (24 * 60)
    val jammatH = totalMins / 60
    val jammatM = totalMins % 60
    return String.format(java.util.Locale.US, "%02d:%02d", jammatH, jammatM)
}

fun checkPrayerUnlocked(
    jammatTime24: String,
    azanTime24: String,
    now: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")),
    systemName: String = ""
): Pair<Boolean, String> {
    val timeToUse = if (jammatTime24.isNotBlank() && jammatTime24.contains(":")) {
        jammatTime24.trim()
    } else {
        azanTime24.trim()
    }
    if (!timeToUse.contains(":")) return Pair(true, "")
    val parts = timeToUse.split(":")
    val rawH = parts.getOrNull(0)?.toIntOrNull() ?: return Pair(true, "")
    val rawM = parts.getOrNull(1)?.toIntOrNull() ?: return Pair(true, "")
    
    val h = when (systemName.lowercase()) {
        "fajr" -> rawH // Fajr is ALWAYS in the morning (AM), never add 12
        "dhuhr", "zohar", "jumah", "jum'ah" -> if (rawH in 1..11) rawH + 12 else rawH
        "asr" -> if (rawH in 1..11) rawH + 12 else rawH
        "maghrib" -> if (rawH in 1..11) rawH + 12 else rawH
        "isha" -> if (rawH in 1..11) rawH + 12 else rawH
        else -> {
            if (rawH in 1..3) rawH + 12 else rawH
        }
    }
    val jammatTotalMinutes = h * 60 + rawM
    val unlockTotalMinutes = jammatTotalMinutes + 10 // Active ONLY 10 min after jammat!

    val currentTotalMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    val unlockH = (unlockTotalMinutes / 60) % 24
    val unlockM = unlockTotalMinutes % 60
    val unlockFormatted = formatTo12Hour(String.format(java.util.Locale.US, "%02d:%02d", unlockH, unlockM))

    val isUnlocked = currentTotalMinutes >= unlockTotalMinutes
    return Pair(isUnlocked, unlockFormatted)
}

fun getEffectiveJammatTime(
    systemName: String,
    azanTime24: String,
    customJammatMap: Map<String, String>,
    isFriday: Boolean = false,
    masjid: MasjidItem? = null
): String {
    // 1. Check masjid's fixed Jammat time FIRST (Primary Source of Truth)
    if (masjid != null) {
        if (systemName.equals("Jumah", ignoreCase = true) || systemName.equals("Jum'ah", ignoreCase = true) || (systemName.equals("Dhuhr", ignoreCase = true) && isFriday)) {
            if (masjid.jumahJammatTime.isNotBlank()) {
                return masjid.jumahJammatTime
            }
        }
        val fixed = when (systemName.lowercase()) {
            "fajr" -> masjid.fajrJammatFixed
            "dhuhr", "zohar" -> masjid.zoharJammatFixed
            "asr" -> masjid.asrJammatFixed
            "maghrib" -> masjid.maghribJammatFixed
            "isha" -> masjid.ishaJammatFixed
            else -> null
        }
        if (!fixed.isNullOrBlank()) {
            return fixed
        }
    }

    // 2. Custom local override if set
    if (systemName.equals("Jumah", ignoreCase = true) || systemName.equals("Jum'ah", ignoreCase = true) || (systemName.equals("Dhuhr", ignoreCase = true) && isFriday)) {
        val customJumah = customJammatMap["jumah"]
        if (!customJumah.isNullOrEmpty()) {
            return customJumah
        }
        return masjid?.jumahJammatTime ?: "13:30"
    }
    val custom = customJammatMap[systemName.lowercase()]
    if (!custom.isNullOrEmpty()) {
        return custom
    }
    if (masjid != null && azanTime24.contains(":")) {
        val offset = when (systemName.lowercase()) {
            "fajr" -> masjid.fajrJammatOffset
            "dhuhr" -> masjid.dhuhrJammatOffset
            "asr" -> masjid.asrJammatOffset
            "maghrib" -> masjid.maghribJammatOffset
            "isha" -> masjid.ishaJammatOffset
            else -> 20
        }
        val parts = azanTime24.split(":")
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        val totalMins = (h * 60 + m + offset) % (24 * 60)
        return String.format(java.util.Locale.US, "%02d:%02d", totalMins / 60, totalMins % 60)
    }
    return calculateJammatTime(systemName, azanTime24, isFriday)
}

@Composable
fun AzanJammatDisplay(
    azanTime24: String,
    jammatTime24: String,
    isNext: Boolean,
    isOngoing: Boolean = false
) {
    val strings = LocalAppStrings.current
    val rawAzan = if (azanTime24.isNotBlank() && azanTime24 != "--:--" && azanTime24.contains(":")) azanTime24 else "05:40"
    val rawJammat = if (jammatTime24.isNotBlank() && jammatTime24 != "--:--" && jammatTime24.contains(":")) {
        jammatTime24
    } else {
        val parts = rawAzan.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 5
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 40
        val tot = h * 60 + m + 25
        String.format(java.util.Locale.US, "%02d:%02d", (tot / 60) % 24, tot % 60)
    }

    val azanFormatted = formatTo12Hour(rawAzan)
    val jammatFormatted = formatTo12Hour(rawJammat)

    val labelColor = if (isNext || isOngoing) Color(0xFFF3DE8E) else TextMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        // Azan column: Name on top (ALL CAPS), time below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.azanLabel.uppercase(),
                fontSize = if (isNext) 10.sp else 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = azanFormatted,
                fontSize = if (isNext) 12.sp else 10.5.sp,
                fontWeight = if (isNext) FontWeight.Black else FontWeight.Bold,
                color = if (isNext || isOngoing) Color(0xFFFFD700) else Color.White
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .width(1.dp)
                .height(if (isNext) 24.dp else 18.dp)
                .background(Color.White.copy(alpha = if (isNext) 0.35f else 0.15f))
        )

        // Jammat column: Name on top (ALL CAPS), time below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.jammatLabel.uppercase(),
                fontSize = if (isNext) 10.sp else 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNext) Color(0xFF86EFAC) else TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = jammatFormatted,
                fontSize = if (isNext) 12.sp else 10.5.sp,
                fontWeight = if (isNext) FontWeight.Black else FontWeight.Bold,
                color = if (isNext || isOngoing) Color(0xFF4ADE80) else Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun HighlightedAzanJammatDisplay(
    azanTime24: String,
    jammatTime24: String,
    compact: Boolean = false,
    textScale: Float = 1.0f,
    timeFontSize: androidx.compose.ui.unit.TextUnit? = null,
    timeSuffixSize: androidx.compose.ui.unit.TextUnit? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val rawAzan = if (azanTime24.isNotBlank() && azanTime24 != "--:--" && azanTime24.contains(":")) azanTime24 else "05:40"
    val rawJammat = if (jammatTime24.isNotBlank() && jammatTime24 != "--:--" && jammatTime24.contains(":")) {
        jammatTime24
    } else {
        val parts = rawAzan.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 5
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 40
        val tot = h * 60 + m + 25
        String.format(java.util.Locale.US, "%02d:%02d", (tot / 60) % 24, tot % 60)
    }

    val azanFormatted = formatTo12Hour(rawAzan)
    val jammatFormatted = formatTo12Hour(rawJammat)

    val azanParts = azanFormatted.split(" ")
    val azanTimeStr = azanParts.getOrNull(0) ?: azanFormatted
    val azanSuffix = azanParts.getOrNull(1) ?: ""

    val jammatParts = jammatFormatted.split(" ")
    val jammatTimeStr = jammatParts.getOrNull(0) ?: jammatFormatted
    val jammatSuffix = jammatParts.getOrNull(1) ?: ""

    val effectiveTimeSize = timeFontSize ?: (if (compact) 16.5.sp else (20f * textScale).sp)
    val effectiveSuffixSize = timeSuffixSize ?: (if (compact) 9.5.sp else (12f * textScale).sp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.SpaceEvenly
    ) {
        // Azan column: Label on top (ALL CAPS), prominent time below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (compact) Modifier else Modifier.weight(1f)
        ) {
            Text(
                text = strings.azanLabel.uppercase(),
                fontSize = if (compact) 9.5.sp else (11f * textScale).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF3DE8E),
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height((2f * textScale).dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = azanTimeStr,
                    fontSize = effectiveTimeSize,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700),
                    maxLines = 1,
                    softWrap = false
                )
                if (azanSuffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = azanSuffix,
                        fontSize = effectiveSuffixSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700).copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = if (timeFontSize != null) 3.dp else (1.5f * textScale).dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = if (compact) 8.dp else (12f * textScale).dp)
                .width(if (compact) 1.dp else (1.6f * textScale).dp)
                .height(if (compact) 24.dp else (if (timeFontSize != null) 38.dp else (32f * textScale).dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF10B981).copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Jammat column: Label on top (ALL CAPS), prominent time below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (compact) Modifier else Modifier.weight(1f)
        ) {
            Text(
                text = strings.jammatLabel.uppercase(),
                fontSize = if (compact) 9.5.sp else (11f * textScale).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF86EFAC),
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height((2f * textScale).dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = jammatTimeStr,
                    fontSize = effectiveTimeSize,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4ADE80),
                    maxLines = 1,
                    softWrap = false
                )
                if (jammatSuffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = jammatSuffix,
                        fontSize = effectiveSuffixSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80).copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = if (timeFontSize != null) 3.dp else (1.5f * textScale).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerTimeDisplay(time24: String, isNext: Boolean) {
    val formatted = formatTo12Hour(time24)
    val parts = formatted.split(" ")
    val timeStr = parts.getOrNull(0) ?: time24
    val suffixStr = parts.getOrNull(1) ?: ""
    
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = timeStr,
            fontSize = if (isNext) 22.sp else 16.sp,
            fontWeight = if (isNext) FontWeight.Black else FontWeight.Medium,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF3DE8E), Color(0xFFB37C3C))
                )
            )
        )
        if (suffixStr.isNotEmpty()) {
            Text(
                text = suffixStr,
                fontSize = if (isNext) 14.sp else 10.sp,
                fontWeight = if (isNext) FontWeight.Black else FontWeight.Medium,
                color = if (isNext) MaterialTheme.colorScheme.secondary else TextMuted
            )
        }
    }
}

fun Modifier.animatedWavingLines(primaryColor: Color) = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    this.drawBehind {
        val width = size.width
        val height = size.height
        val centerY = height * 0.70f
        
        for (i in 0 until 3) {
            val amplitude = 25f + (i * 20f)
            val frequency = 1.5f
            
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, centerY)
            
            val steps = 50
            for (step in 0..steps) {
                val x = (step.toFloat() / steps) * width
                val angle = (x / width) * (2f * Math.PI.toFloat() * frequency) + phase + (i * 1.5f)
                val y = centerY + kotlin.math.sin(angle).toFloat() * amplitude
                path.lineTo(x, y)
            }
            
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.03f + (i * 0.015f)),
                style = Stroke(width = 3f)
            )
        }
    }
}

data class AdminPrayerItem(
    val systemName: String,
    val displayName: String,
    val icon: ImageVector,
    val azanTime: String,
    val jammatTime: String
)

@Composable
fun AdminLoginDialog(
    selectedMasjid: com.example.data.MasjidItem? = null,
    allMasajid: List<com.example.data.MasjidItem> = emptyList(),
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var adminId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ADMIN LOGIN",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (selectedMasjid != null) "Enter ID & Password for ${selectedMasjid.name}" else "Enter ID & Password to edit prayer timings",
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = adminId,
                    onValueChange = {
                        adminId = it
                        isError = false
                    },
                    label = { Text("Admin ID") },
                    placeholder = { Text("admin") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = { Text("Password") },
                    placeholder = { Text("••••") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedMasjid != null) "Incorrect ID or Password for ${selectedMasjid.name}" else "Incorrect ID or Password",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val id = adminId.trim()
                            val pass = password.trim()

                            val curMasjid = selectedMasjid ?: allMasajid.firstOrNull()
                            val targetAdminId = curMasjid?.adminId?.trim().takeUnless { it.isNullOrBlank() } ?: "admin"
                            val targetPassword = curMasjid?.adminPassword?.trim() ?: ""

                            // Condition: Must match Google Sheet Admin ID and Password for the currently selected masjid
                            val isMatch = id.equals(targetAdminId, ignoreCase = true) &&
                                (pass == targetPassword || (targetPassword.isBlank() && (pass == "admin" || pass == "1234")))

                            if (isMatch) {
                                isError = false
                                onLoginSuccess()
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Text("Login", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPanelScreen(
    viewModel: AzanViewModel,
    uiState: com.example.ui.UIState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val curMasjid = uiState.selectedMasjid
    val isFridayToday = uiState.selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    val dhuhrLabel = if (isFridayToday) strings.jumah else strings.dhuhr.uppercase()

    val timings = uiState.todayTimings
    val fajrAzan = curMasjid.fajrAzanFixed ?: timings?.fajr ?: "05:40"
    val zoharAzan = curMasjid.zoharAzanFixed ?: "13:15"
    val jumahAzan = curMasjid.jumahAzanTime.ifBlank { "12:30" }
    val asrAzan = curMasjid.asrAzanFixed ?: timings?.asr ?: "17:17"
    val maghribAzan = curMasjid.maghribAzanFixed ?: timings?.maghrib ?: "18:10"
    val ishaAzan = curMasjid.ishaAzanFixed ?: timings?.isha ?: "19:50"

    val fajrJammat = curMasjid.fajrJammatFixed ?: getEffectiveJammatTime("Fajr", fajrAzan, uiState.customJammatTimes, false, curMasjid)
    val zoharJammat = curMasjid.zoharJammatFixed ?: getEffectiveJammatTime("Dhuhr", zoharAzan, uiState.customJammatTimes, false, curMasjid)
    val jumahJammat = curMasjid.jumahJammatTime.ifBlank { "13:00" }
    val asrJammat = curMasjid.asrJammatFixed ?: getEffectiveJammatTime("Asr", asrAzan, uiState.customJammatTimes, false, curMasjid)
    val maghribJammat = curMasjid.maghribJammatFixed ?: getEffectiveJammatTime("Maghrib", maghribAzan, uiState.customJammatTimes, false, curMasjid)
    val ishaJammat = curMasjid.ishaJammatFixed ?: getEffectiveJammatTime("Isha", ishaAzan, uiState.customJammatTimes, false, curMasjid)

    val adminPrayers = listOf(
        AdminPrayerItem("Fajr", strings.fajr.uppercase(), Icons.Outlined.WbTwilight, fajrAzan, fajrJammat),
        AdminPrayerItem("Zohar", strings.dhuhr.uppercase(), Icons.Outlined.WbSunny, zoharAzan, zoharJammat),
        AdminPrayerItem("Jumah", strings.jumah, Icons.Outlined.WbSunny, jumahAzan, jumahJammat),
        AdminPrayerItem("Asr", strings.asr.uppercase(), Icons.Outlined.WbCloudy, asrAzan, asrJammat),
        AdminPrayerItem("Maghrib", strings.maghrib.uppercase(), Icons.Outlined.NightsStay, maghribAzan, maghribJammat),
        AdminPrayerItem("Isha", strings.isha.uppercase(), Icons.Outlined.Nightlight, ishaAzan, ishaJammat)
    )

    var editingPrayer by remember { mutableStateOf<AdminPrayerItem?>(null) }
    var showEditMasjidDialog by remember { mutableStateOf(false) }
    var showAddMasjidDialog by remember { mutableStateOf(false) }
    var showAppsScriptSettingsDialog by remember { mutableStateOf(false) }
    var showScriptCodeDialog by remember { mutableStateOf(false) }
    var isSyncingNow by remember { mutableStateOf(false) }
    var syncStatusErrorDialog by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .appBackground()
            .islamicStarBackground(primaryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ADMIN PANEL",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Edit Namaz Azan & Jammat Timings",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subtle Sheet Sync Status Badge (click to configure)
                    Surface(
                        onClick = { showAppsScriptSettingsDialog = true },
                        color = if (uiState.appsScriptUrl.isNotBlank()) Color(0xFF14532D).copy(alpha = 0.65f) else Color(0xFF78350F).copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (uiState.appsScriptUrl.isNotBlank()) Color(0xFF86EFAC).copy(alpha = 0.8f) else Color(0xFFFCD34D).copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.appsScriptUrl.isNotBlank()) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (uiState.appsScriptUrl.isNotBlank()) Color(0xFF86EFAC) else Color(0xFFFCD34D),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (uiState.appsScriptUrl.isNotBlank()) strings.saved else strings.connect,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.appsScriptUrl.isNotBlank()) Color(0xFF86EFAC) else Color(0xFFFCD34D)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val configuredUrl = uiState.appsScriptUrl.trim()
                            if (configuredUrl.isBlank()) {
                                showAppsScriptSettingsDialog = true
                                android.widget.Toast.makeText(
                                    context,
                                    strings.connect,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                isSyncingNow = true
                                scope.launch {
                                    val (success, msg) = viewModel.syncMasjidToSheet(uiState.selectedMasjid)
                                    isSyncingNow = false
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        if (success) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "✅ ${strings.timeUpdated}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            onBack()
                                        } else {
                                            syncStatusErrorDialog = msg
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSyncingNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    ) {
                        if (isSyncingNow) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Language Switcher Row (Same as Home Page)
            LanguageCirclesRow(
                currentLang = uiState.language,
                onLangSelect = { viewModel.setLanguage(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Content: Active Masjid Card + 6 Prayer Slots
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Masjid Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .border(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mosque,
                                    contentDescription = null,
                                    tint = Color(0xFFF3DE8E),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = curMasjid.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF3DE8E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${curMasjid.area} • ID: #${curMasjid.id}",
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                adminPrayers.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522).copy(alpha = 0.88f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Icon + Prayer Name
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFF1A2234), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.displayName,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Text(
                                    text = item.displayName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextColor
                                )
                            }

                            // Center & Right: Azan & Jammat time + Pencil Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AzanJammatDisplay(
                                    azanTime24 = item.azanTime,
                                    jammatTime24 = item.jammatTime,
                                    isNext = false
                                )

                                IconButton(
                                    onClick = { editingPrayer = item },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit ${item.displayName}",
                                        tint = Color(0xFFF3DE8E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Info footer card filling bottom space elegantly
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF86EFAC),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Timings saved here reflect immediately on the Home screen clock, Namaz cards, and Azan alarms.",
                            fontSize = 11.5.sp,
                            color = TextColor.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Edit Dialog
        editingPrayer?.let { prayer ->
            EditPrayerTimingDialog(
                prayer = prayer,
                onDismiss = { editingPrayer = null },
                onSave = { newAzan, newJammat ->
                    Toast.makeText(context, "Updating timings for all mobiles...", Toast.LENGTH_SHORT).show()
                    viewModel.updatePrayerLocally(prayer.systemName, newAzan, newJammat) { success, _ ->
                        if (success) {
                            Toast.makeText(context, "Time update ho gaya! Sabhi mobile par time badal chuka hai.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Time save ho gaya.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    editingPrayer = null
                }
            )
        }

        // Edit Masjid Dialog
        if (showEditMasjidDialog) {
            MasjidInfoDialog(
                initialMasjid = curMasjid,
                isNew = false,
                onDismiss = { showEditMasjidDialog = false },
                onSave = { id, name, address, photoUrl ->
                    viewModel.saveOrUpdateMasjid(id, name, address, photoUrl)
                    android.widget.Toast.makeText(context, "$name updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    showEditMasjidDialog = false
                }
            )
        }

        // Add Masjid Dialog
        if (showAddMasjidDialog) {
            MasjidInfoDialog(
                initialMasjid = null,
                isNew = true,
                onDismiss = { showAddMasjidDialog = false },
                onSave = { id, name, address, photoUrl ->
                    viewModel.saveOrUpdateMasjid(id, name, address, photoUrl)
                    android.widget.Toast.makeText(context, "$name added successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    showAddMasjidDialog = false
                }
            )
        }

        // Apps Script Settings Dialog
        if (showAppsScriptSettingsDialog) {
            AppsScriptSettingsDialog(
                currentUrl = uiState.appsScriptUrl,
                onDismiss = { showAppsScriptSettingsDialog = false },
                onSave = { newUrl ->
                    viewModel.setAppsScriptUrl(newUrl)
                    android.widget.Toast.makeText(context, "Apps Script URL saved!", android.widget.Toast.LENGTH_SHORT).show()
                },
                onTest = { testUrl ->
                    viewModel.testAppsScriptConnection(testUrl)
                },
                onShowScriptCode = {
                    showScriptCodeDialog = true
                }
            )
        }

        // Apps Script Code Guide Dialog
        if (showScriptCodeDialog) {
            AppsScriptCodeDialog(
                onDismiss = { showScriptCodeDialog = false }
            )
        }

        // Sync Error Dialog with Direct Action
        syncStatusErrorDialog?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { syncStatusErrorDialog = null },
                containerColor = Color(0xFF1E293B),
                title = {
                    Text(
                        text = "Google Sheet Sync Error",
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (errorMsg == "APPS_SCRIPT_NOT_SET") {
                                "Google Sheet par live save hone ke liye WebApp URL configure hona zaroori hai."
                            } else {
                                "Google Sheet se connect nahi ho paya:\n\n$errorMsg"
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Aap 'Setup WebApp URL' par click karke apna URL check ya set kar sakte hain.",
                            color = Color(0xFFF3DE8E),
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            syncStatusErrorDialog = null
                            showAppsScriptSettingsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Setup WebApp URL", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { syncStatusErrorDialog = null }) {
                        Text("Dismiss", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }
    }
}

@Composable
fun AppsScriptSettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTest: suspend (String) -> Pair<Boolean, String>,
    onShowScriptCode: () -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, Color(0xFFF3DE8E).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GOOGLE SHEET APPS SCRIPT URL",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF3DE8E),
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Apne Google Sheet ka deployed Web App URL yahan paste karein taaki Namaz timings Google Sheet par save ho sakein.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { 
                        urlText = it
                        testResult = null
                    },
                    label = { Text("Apps Script WebApp URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                urlText = clipText.trim()
                                testResult = null
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFFF3DE8E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste URL", color = Color(0xFFF3DE8E), fontSize = 12.sp)
                    }

                    TextButton(onClick = onShowScriptCode) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF86EFAC), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get Script Code", color = Color(0xFF86EFAC), fontSize = 12.sp)
                    }
                }

                testResult?.let { (success, msg) ->
                    Text(
                        text = if (success) "🟢 $msg" else "🔴 $msg",
                        fontSize = 11.5.sp,
                        color = if (success) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            coroutineScope.launch {
                                testResult = onTest(urlText)
                                isTesting = false
                            }
                        },
                        enabled = !isTesting && urlText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.6f))
                    ) {
                        Text(if (isTesting) "Testing..." else "Test URL", color = Color(0xFFF3DE8E), fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onSave(urlText)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3DE8E))
                    ) {
                        Text("Save URL", color = Color(0xFF0C101B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AppsScriptCodeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val code = GoogleSheetMasjidSync.APPS_SCRIPT_SAMPLE_CODE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, Color(0xFF86EFAC).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "APPS SCRIPT SETUP GUIDE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF86EFAC),
                    letterSpacing = 0.8.sp
                )

                Text(
                    text = "Follow these 4 simple steps to connect your Google Sheet:\n" +
                            "1. Open your Google Sheet in browser.\n" +
                            "2. Click Extensions > Apps Script.\n" +
                            "3. Replace all code with the script below and Save 💾.\n" +
                            "4. Click Deploy > New deployment > Web App > Execute as: Me, Who has access: Anyone > Deploy.\n" +
                            "5. Copy the Web App URL and paste it into 'Set WebApp URL' in this app.",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF070B12), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = code,
                        fontSize = 10.5.sp,
                        color = Color(0xFF86EFAC),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            android.widget.Toast.makeText(context, "Apps Script code copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF86EFAC))
                    ) {
                        Text("Copy Code", color = Color(0xFF0C101B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text("Close", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MasjidInfoDialog(
    initialMasjid: MasjidItem?,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (id: String, name: String, address: String, photoUrl: String) -> Unit
) {
    var id by remember { mutableStateOf(initialMasjid?.id ?: "") }
    var name by remember { mutableStateOf(initialMasjid?.name ?: "") }
    var address by remember { mutableStateOf(initialMasjid?.area ?: "") }
    var photoUrl by remember { mutableStateOf(initialMasjid?.photoUrl ?: "") }
    var errorText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isNew) "ADD NEW MASJID" else "EDIT MASJID INFO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Saved masajid display on Home page and sync with Google Sheet & Drive",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = "" },
                    label = { Text("Masjid Name") },
                    placeholder = { Text("e.g. Mohammadiya Masjid") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; errorText = "" },
                    label = { Text("Address / Area") },
                    placeholder = { Text("e.g. Swagat Nagar") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it; errorText = "" },
                    label = { Text("Masjid ID") },
                    placeholder = { Text("e.g. 100111111") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF86EFAC),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF86EFAC),
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it; errorText = "" },
                    label = { Text("Google Drive Photo Link") },
                    placeholder = { Text("https://drive.google.com/file/d/...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF86EFAC),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF86EFAC),
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorText = "Please enter Masjid Name"
                                return@Button
                            }
                            if (address.isBlank()) {
                                errorText = "Please enter Address"
                                return@Button
                            }
                            if (id.isBlank()) {
                                errorText = "Please enter Masjid ID"
                                return@Button
                            }
                            onSave(id, name, address, photoUrl)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Text("Save Masjid", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPrayerTimingDialog(
    prayer: AdminPrayerItem,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val isFajr = prayer.systemName.equals("Fajr", ignoreCase = true)
    val defaultIsPm = !isFajr

    fun parseTimeParts(rawTime: String, fallbackPm: Boolean): Triple<String, String, Boolean> {
        val clean = rawTime.trim()
        val parts = clean.split(":")
        if (parts.size != 2) return Triple("08", "00", fallbackPm)
        val rawH = parts[0].toIntOrNull() ?: 8
        val m = parts[1].toIntOrNull() ?: 0
        val isPm = when {
            rawH in 13..23 -> true
            rawH == 12 -> true
            rawH == 0 -> false
            else -> fallbackPm
        }
        val h12 = when {
            rawH == 0 -> 12
            rawH > 12 -> rawH - 12
            else -> rawH
        }
        return Triple(
            String.format(Locale.US, "%02d", h12),
            String.format(Locale.US, "%02d", m),
            isPm
        )
    }

    val (initAzanH, initAzanM, initAzanPm) = remember(prayer) {
        parseTimeParts(prayer.azanTime, defaultIsPm)
    }
    val (initJammatH, initJammatM, initJammatPm) = remember(prayer) {
        parseTimeParts(prayer.jammatTime, defaultIsPm)
    }

    var azanHours by remember { mutableStateOf(initAzanH) }
    var azanMinutes by remember { mutableStateOf(initAzanM) }
    var azanIsPm by remember { mutableStateOf(initAzanPm) }

    var jammatHours by remember { mutableStateOf(initJammatH) }
    var jammatMinutes by remember { mutableStateOf(initJammatM) }
    var jammatIsPm by remember { mutableStateOf(initJammatPm) }

    var errorText by remember { mutableStateOf("") }

    fun to24Hour(hStr: String, mStr: String, isPm: Boolean): String? {
        val h = hStr.trim().toIntOrNull() ?: return null
        val m = mStr.trim().toIntOrNull() ?: return null
        if (m !in 0..59) return null
        val finalH = if (h in 13..23) {
            h
        } else if (h in 1..12) {
            if (isPm) {
                if (h == 12) 12 else h + 12
            } else {
                if (h == 12) 0 else h
            }
        } else if (h == 0) {
            0
        } else {
            return null
        }
        return String.format(Locale.US, "%02d:%02d", finalH, m)
    }

    val validAzan = to24Hour(azanHours, azanMinutes, azanIsPm)
    val validJammat = to24Hour(jammatHours, jammatMinutes, jammatIsPm)
    val previewAzan = if (validAzan != null) formatTo12Hour(validAzan) else "--:--"
    val previewJammat = if (validJammat != null) formatTo12Hour(validJammat) else "--:--"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Mosque Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = prayer.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "EDIT ${prayer.displayName.uppercase()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Hours & Minutes separate for Azan and Jammat",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. AZAN TIME SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFF3DE8E), CircleShape)
                                )
                                Text(
                                    text = "AZAN TIME",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3DE8E),
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = previewAzan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3DE8E)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Azan Hours and Minutes TextFields + AM/PM
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1st Text Field: Hours (e.g. 08)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = azanHours,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                            azanHours = it
                                            errorText = ""
                                        }
                                    },
                                    placeholder = { Text("08", fontSize = 18.sp, color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF3DE8E),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(76.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "1st: HRS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3DE8E)
                                )
                            }

                            Text(
                                text = ":",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF3DE8E),
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                            )

                            // 2nd Text Field: Minutes (e.g. 55)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = azanMinutes,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                            azanMinutes = it
                                            errorText = ""
                                        }
                                    },
                                    placeholder = { Text("55", fontSize = 18.sp, color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF3DE8E),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(76.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "2nd: MIN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3DE8E)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // AM / PM Switcher
                            Column(
                                modifier = Modifier.padding(bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = if (!azanIsPm) Color(0xFFF3DE8E) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable { azanIsPm = false }
                                ) {
                                    Text(
                                        text = "AM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!azanIsPm) Color(0xFF0F172A) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = if (azanIsPm) Color(0xFFF3DE8E) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable { azanIsPm = true }
                                ) {
                                    Text(
                                        text = "PM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (azanIsPm) Color(0xFF0F172A) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. JAMMAT TIME SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF86EFAC), CircleShape)
                                )
                                Text(
                                    text = "JAMMAT TIME",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC),
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = previewJammat,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Jammat Hours and Minutes TextFields + AM/PM
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1st Text Field: Hours (e.g. 08)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = jammatHours,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                            jammatHours = it
                                            errorText = ""
                                        }
                                    },
                                    placeholder = { Text("08", fontSize = 18.sp, color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF86EFAC),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(76.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "1st: HRS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC)
                                )
                            }

                            Text(
                                text = ":",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF86EFAC),
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                            )

                            // 2nd Text Field: Minutes (e.g. 55)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = jammatMinutes,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                            jammatMinutes = it
                                            errorText = ""
                                        }
                                    },
                                    placeholder = { Text("55", fontSize = 18.sp, color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF86EFAC),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(76.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "2nd: MIN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // AM / PM Switcher
                            Column(
                                modifier = Modifier.padding(bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = if (!jammatIsPm) Color(0xFF86EFAC) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable { jammatIsPm = false }
                                ) {
                                    Text(
                                        text = "AM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!jammatIsPm) Color(0xFF0F172A) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = if (jammatIsPm) Color(0xFF86EFAC) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable { jammatIsPm = true }
                                ) {
                                    Text(
                                        text = "PM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (jammatIsPm) Color(0xFF0F172A) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorText,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "OK / DONE dabate hi sabhi mobile mein ek sath naya time change ho jayega",
                    fontSize = 11.5.sp,
                    color = Color(0xFF86EFAC),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (validAzan == null) {
                                errorText = "Please enter valid Azan hours (01-12) and minutes (00-59)"
                                return@Button
                            }
                            if (validJammat == null) {
                                errorText = "Please enter valid Jammat hours (01-12) and minutes (00-59)"
                                return@Button
                            }
                            onSave(validAzan, validJammat)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Text("OK / DONE", fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun NavCircleItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    testTag: String,
    enabled: Boolean = true
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "navScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    when {
                        selected -> Color(0xFFF3DE8E)
                        !enabled -> Color(0xFF14261B).copy(alpha = 0.5f)
                        else -> Color(0xFF14261B)
                    }
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = when {
                        selected -> Color(0xFFFFD700)
                        !enabled -> Color(0xFFF3DE8E).copy(alpha = 0.15f)
                        else -> Color(0xFFF3DE8E).copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    selected -> Color(0xFF0F2618)
                    !enabled -> Color(0xFFF3DE8E).copy(alpha = 0.4f)
                    else -> Color(0xFFF3DE8E).copy(alpha = 0.85f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = when {
                selected -> Color(0xFFF3DE8E)
                !enabled -> Color.White.copy(alpha = 0.35f)
                else -> Color.White.copy(alpha = 0.65f)
            },
            textAlign = TextAlign.Center
        )
    }
}


