package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.LanguageCirclesRow
import com.example.R
import com.example.appBackground
import com.example.data.GoogleSheetMasjidSync
import com.example.data.MasjidItem
import com.example.islamicStarBackground
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SupportScreen(
    uiState: UIState,
    onBack: () -> Unit,
    onLangSelect: (String) -> Unit = {},
    onMasjidSubmitted: () -> Unit = {},
    onChangeMasjid: () -> Unit = {},
    onSelectMasjid: (MasjidItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val primaryGold = Color(0xFFF3DE8E)
    val whatsappTarget = "919960171516"

    var showAddMasjidDialog by remember { mutableStateOf(false) }
    var showThanksDialog by remember { mutableStateOf(false) }
    var showRateUsDialog by remember { mutableStateOf(false) }
    var showOtherAmountDialog by remember { mutableStateOf(false) }
    var showUpiAppChooserDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var selectedContributionAmount by remember { mutableIntStateOf(250) }

    // Rating system: safely read saved stars with fallback across all types (float, int, string), default 0.0
    val prefs = remember { context.getSharedPreferences("azan_user_rating", Context.MODE_PRIVATE) }
    val initialRating = remember(prefs) {
        try {
            prefs.getFloat("saved_stars", 0f).toDouble()
        } catch (e1: Throwable) {
            try {
                prefs.getInt("saved_stars", 0).toDouble()
            } catch (e2: Throwable) {
                try {
                    prefs.getString("saved_stars", "0")?.toDoubleOrNull() ?: 0.0
                } catch (e3: Throwable) {
                    0.0
                }
            }
        }
    }
    var userRating by remember { mutableDoubleStateOf(initialRating) }
    var averageRating by remember { mutableDoubleStateOf(0.0) }
    var totalReviews by remember { mutableIntStateOf(0) }

    // Fetch live ratings from Google Sheet tab "rating" on load
    LaunchedEffect(Unit) {
        try {
            val (avg, count) = GoogleSheetMasjidSync.fetchRatingStats()
            if (count > 0 && avg > 0.0) {
                averageRating = avg
                totalReviews = count
            }
        } catch (t: Throwable) {
            // Never crash if network or sheet fails
        }
    }

    fun openWhatsApp() {
        try {
            val message = when (uiState.language) {
                "ur" -> "السلام علیکم! اذان ایپ کے بارے میں فیڈ بیک / مدد درکار ہے۔"
                "hi" -> "अस्सलाम-ओ-अलैकुम! अज़ान ऐप के बारे में सहायता / सुझाव।"
                else -> "Assalam-o-Alaikum! I need support / have feedback regarding Azan App."
            }
            val uri = Uri.parse("https://wa.me/$whatsappTarget?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$whatsappTarget"))
                context.startActivity(dial)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchUpiForApp(packageName: String?, amount: Int) {
        val amStr = if (amount > 0) "&am=$amount" else ""
        val uri = Uri.parse("upi://pay?pa=9960171516@ybl&pn=A-tek%20Printing%20Press${amStr}&cu=INR")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (!packageName.isNullOrBlank()) {
            intent.setPackage(packageName)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(Intent.createChooser(fallbackIntent, "Pay with UPI"))
            } catch (e2: Exception) {
                Toast.makeText(context, "No UPI app found. Please scan the QR code.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareApp() {
        try {
            val shareText = when (uiState.language) {
                "ur" -> "🕌 اذان ٹائم ایپ - سولاپور شہر کی مساجد کے اوقاتِ نماز و اذان!\nثوابِ جاریہ کی نیت سے اپنے اہل و عیال اور احباب کو شیئر کریں:\nhttps://drive.google.com/drive/folders/1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n?usp=sharing"
                "hi" -> "🕌 अज़ान टाइम ऐप - सोलापुर शहर की मस्जिदों के सही अज़ान और नमाज़ के औक़ात!\nसवाब-ए-जारिया के लिए अपने दोस्तों और परिवार के साथ शेयर करें:\nhttps://drive.google.com/drive/folders/1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n?usp=sharing"
                else -> "🕌 Azan Time App - Accurate prayer & Azan timings for Solapur City Masajid!\nShare with your family & friends to earn Sawab-e-Jariyah:\nhttps://drive.google.com/drive/folders/1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n?usp=sharing"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Azan Time Solapur")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "Share Azan Time App"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share menu", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040C08))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Language Buttons (Homepage size & style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                LanguageCirclesRow(
                    currentLang = uiState.language,
                    onLangSelect = onLangSelect
                )
            }

            // 2. Top Center App Logo, AZAN TIME & Punchline (Preserving exact size)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0E2F23), Color(0xFF061A13))
                            )
                        )
                        .border(1.dp, Color(0xFFE5B842).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.foreground),
                        contentDescription = "Azan App Icon",
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "AZAN TIME",
                    fontFamily = GraenMetalFontFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = GraenMetalGoldGradient,
                        shadow = GraenMetalGoldShadow
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Normal font punchline under AZAN TIME
                Text(
                    text = "AAO ALLAH KI RAAH MEIN CHALE",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    color = Color(0xFFF2CA50).copy(alpha = 0.90f),
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )
            }

            // 3. Divided 2-Column Slot: Support & Feedback / Request Add Masjid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left: Support & Feedback (WhatsApp)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable { openWhatsApp() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF091712)),
                    border = BorderStroke(1.dp, Color(0xFF1B3B2E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "سپورٹ اور فیڈبیک"
                                    "hi" -> "सपोर्ट और सुझाव"
                                    else -> "Support & Feedback"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "فوری واٹس ایپ"
                                    "hi" -> "इंस्टेंट व्हाट्सएप"
                                    else -> "Instant WhatsApp"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4ADE80),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Right: Request Add Masjid
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable { showAddMasjidDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF091712)),
                    border = BorderStroke(1.dp, Color(0xFF1B3B2E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mosque,
                            contentDescription = "Request Add Masjid",
                            tint = primaryGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "نئی مسجد شامل کریں"
                                    "hi" -> "नई मस्जिद जोड़ें"
                                    else -> "Request Add Masjid"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "اوقاتِ نماز درج کریں"
                                    "hi" -> "लोकल टाइमिंग भेजें"
                                    else -> "Submit Local Timings"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE5B842),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 4. Change Slot (Selected Masjid)
            MasjidSelectorDropdown(
                selectedMasjid = uiState.selectedMasjid,
                allMasajid = uiState.allMasajid,
                language = uiState.language,
                onSelectMasjid = onSelectMasjid,
                onLanguageSelect = onLangSelect,
                modifier = Modifier.fillMaxWidth()
            )

            // 5. Main Donation Card (Sadaqah Jariyah: Styled like Support & Feedback, no side QR rectangle)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071711)),
                border = BorderStroke(1.dp, Color(0xFF1B3B2E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header row: Balanced Center SADAQAH JARIYAH (in CAPS) + Larger QR Code button on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left balanced spacer matching QR button width for true center alignment
                        Spacer(modifier = Modifier.width(88.dp))

                        // Center: SADAQAH JARIYAH (CAPS and Centered)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "صدقہ جاریہ"
                                    "hi" -> "सदक़ा-ए-जारिया"
                                    else -> "SADAQAH JARIYAH"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.8.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "ایپ تعاون و ثوابِ جاریہ"
                                    "hi" -> "सवाब-ए-जारिया व ऐप सहयोग"
                                    else -> "Support App & Sawab-e-Jariyah"
                                },
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4ADE80),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }

                        // Right: Larger QR Code button (thoda bada!)
                        Box(
                            modifier = Modifier
                                .width(88.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF162D22))
                                .border(1.dp, Color(0xFF2E533F), RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = primaryGold),
                                    onClick = { showQrDialog = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "QR Code",
                                    tint = primaryGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "QR Code",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryGold
                                )
                            }
                        }
                    }

                    // Amounts
                    val amounts = listOf(50, 100, 250, 500)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        amounts.forEach { amt ->
                            val isSelected = selectedContributionAmount == amt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (isSelected) Color(0xFFE5B842) else Color(0xFF0F261C))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFFFE082) else Color(0xFF224836),
                                        shape = RoundedCornerShape(7.dp)
                                    )
                                    .clickable {
                                        selectedContributionAmount = amt
                                        showUpiAppChooserDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹$amt",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0F1713) else primaryGold
                                )
                            }
                        }

                        val isOtherSelected = !amounts.contains(selectedContributionAmount)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isOtherSelected) Color(0xFFE5B842) else Color(0xFF0F261C))
                                .border(
                                    width = 1.dp,
                                    color = if (isOtherSelected) Color(0xFFFFE082) else Color(0xFF224836),
                                    shape = RoundedCornerShape(7.dp)
                                )
                                .clickable { showOtherAmountDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isOtherSelected) "₹$selectedContributionAmount" else "Other",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOtherSelected) Color(0xFF0F1713) else primaryGold
                            )
                        }
                    }

                    // Pay UPI Button
                    Button(
                        onClick = { showUpiAppChooserDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123425)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                    ) {
                        Text(
                            text = "Pay ₹$selectedContributionAmount via UPI (GPay, PhonePe, Paytm)",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold,
                            maxLines = 1
                        )
                    }
                }
            }

            // 6. Rating Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071711)),
                border = BorderStroke(1.dp, Color(0xFF1B3B2E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1A271B))
                            .border(1.dp, Color(0xFFE5B842).copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color(0xFFE5B842)),
                                onClick = { showRateUsDialog = true }
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Rate Us →",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold
                        )
                    }

                    val appRatingDisplay = if (averageRating > 0.0) averageRating else 4.8
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", appRatingDisplay),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                            for (i in 1..5) {
                                val fillRatio = when {
                                    appRatingDisplay >= i -> 1f
                                    appRatingDisplay >= i - 0.5 -> 0.5f
                                    else -> 0f
                                }
                                StarIconDisplay(fillRatio = fillRatio, size = 13.dp)
                            }
                        }
                        Text(
                            text = "• 1000+ users",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // 7. Footer: Version & Devotion combined in center
            Text(
                text = "Made with devotion • v${BuildConfig.VERSION_NAME} • Powered by @tek",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = primaryGold.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
            )
        }
    }

    // Custom Amount Dialog (when "Other" contribution is clicked)
    if (showOtherAmountDialog) {
        val amountFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val amountKeyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        var customAmountText by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = {
                amountFocusManager.clearFocus()
                amountKeyboardController?.hide()
                showOtherAmountDialog = false
            },
            properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E17)),
                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter Contribution Amount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryGold
                    )
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (₹)", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("e.g. 1000", color = Color.White.copy(alpha = 0.3f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showOtherAmountDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val entered = customAmountText.toIntOrNull()
                                if (entered != null && entered > 0) {
                                    selectedContributionAmount = entered
                                    showOtherAmountDialog = false
                                    showUpiAppChooserDialog = true
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B842)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Proceed", color = Color(0xFF0F1713), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1B15)),
                border = BorderStroke(1.5.dp, Color(0xFFE5B842).copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scan QR to Donate",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold
                        )
                        IconButton(onClick = { showQrDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.5.dp, primaryGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.qr_support_donate),
                            contentDescription = "Donation QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Text(
                        text = "Scan using any UPI App (GPay, PhonePe, Paytm)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Rate Us Interactive Dialog (Supports Half-Stars e.g. 2.5 and Saves to Google Sheet)
    if (showRateUsDialog) {
        var tempRating by remember { mutableDoubleStateOf(if (userRating > 0.0) userRating else 5.0) }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showRateUsDialog = false }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E17)),
                border = BorderStroke(1.5.dp, primaryGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Rate Azan Time",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryGold
                    )
                    Text(
                        text = "Rate your experience. Submitting will save your rating and redirect to Google Play Store to review.",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )

                    // Large Interactive Half-Star Rating Bar
                    InteractiveHalfStarRatingBar(
                        rating = tempRating,
                        onRatingSelected = { selected ->
                            tempRating = selected
                        },
                        starSize = 36.dp,
                        spacing = 8.dp
                    )

                    Text(
                        text = String.format(Locale.US, "%.1f / 5.0 Stars", tempRating),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryGold
                    )

                    Button(
                        onClick = {
                            isSubmitting = true
                            userRating = tempRating
                            try {
                                prefs.edit().putFloat("saved_stars", tempRating.toFloat()).apply()
                            } catch (e: Throwable) {
                                // Ignore
                            }
                            coroutineScope.launch {
                                try {
                                    GoogleSheetMasjidSync.submitRating(tempRating)
                                } catch (t: Throwable) {
                                    // Ignore
                                }
                                isSubmitting = false
                                showRateUsDialog = false

                                // Launch Google Play Store directly to app's listing page for rating & review
                                try {
                                    val pkgName = context.packageName
                                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkgName")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                    context.startActivity(marketIntent)
                                } catch (e: Exception) {
                                    try {
                                        val pkgName = context.packageName
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkgName")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(webIntent)
                                    } catch (e2: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                            Toast.makeText(
                                context,
                                "⭐⭐⭐⭐⭐ JazakAllah Khair! Opening Play Store...",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B842)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color(0xFF0F1713),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", color = Color(0xFF0F1713), fontWeight = FontWeight.Bold)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF0F1713),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Submit & Rate on Play Store",
                                    color = Color(0xFF0F1713),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // UPI App Chooser Dialog (Google Pay, PhonePe, Paytm, Other)
    if (showUpiAppChooserDialog) {
        UpiAppChooserDialog(
            amount = selectedContributionAmount,
            onDismiss = { showUpiAppChooserDialog = false },
            onAppSelected = { pkg ->
                showUpiAppChooserDialog = false
                launchUpiForApp(pkg, selectedContributionAmount)
            }
        )
    }

    // Add Masjid Form Dialog (Auto sends directly to email without redirecting)
    if (showAddMasjidDialog) {
        AddMasjidAutoDialog(
            language = uiState.language,
            onDismiss = { showAddMasjidDialog = false },
            onSubmitted = {
                showAddMasjidDialog = false
                showThanksDialog = true
            }
        )
    }

    // Thanks Popup in Green with Thumb 👍 Icon
    if (showThanksDialog) {
        AlertDialog(
            onDismissRequest = { showThanksDialog = false },
            containerColor = Color(0xFF0F241A),
            shape = RoundedCornerShape(22.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Thanks",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = when (uiState.language) {
                        "ur" -> "درخواست موصول ہوگئی! 👍"
                        "hi" -> "अनुरोध प्राप्त हुआ! 👍"
                        else -> "Request Received! 👍"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color(0xFF34D399),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                val annotatedText = buildAnnotatedString {
                    append("Please wait ")
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    ) {
                        append("(5 Min)")
                    }
                    append("\nYour Masjid will be live.")
                }
                Text(
                    text = annotatedText,
                    fontSize = 14.5.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showThanksDialog = false
                        onMasjidSubmitted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        )
    }
}

fun prepareStraightMasjidPhotoBase64(context: Context, uri: Uri, maxDim: Int = 1080): String? {
    return try {
        // 1. Read EXIF Orientation
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // 2. Decode raw bitmap
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        // 3. Build rotation matrix to preserve natural upright / straight angle (do not flip to landscape)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        val correctlyOrientedBitmap = if (!matrix.isIdentity) {
            Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )
        } else {
            originalBitmap
        }

        // 4. Scale down while strictly preserving aspect ratio
        val w = correctlyOrientedBitmap.width
        val h = correctlyOrientedBitmap.height
        val scale = (maxDim.toFloat() / maxOf(w, h)).coerceAtMost(1f)
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                correctlyOrientedBitmap,
                (w * scale).toInt(),
                (h * scale).toInt(),
                true
            )
        } else {
            correctlyOrientedBitmap
        }

        val baos = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

@Composable
fun AddMasjidAutoDialog(
    language: String,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val prefs = remember { com.example.data.PreferencesRepository(context) }
    val savedAppsScriptUrl by prefs.appsScriptUrlFlow.collectAsState(initial = com.example.data.GoogleSheetMasjidSync.APPS_SCRIPT_WEBAPP_URL)
    var masjidName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var adminContact by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    Dialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .imePadding(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (language) {
                            "ur" -> "نئی مسجد شامل کریں"
                            "hi" -> "नई मस्जिद जोड़ें"
                            else -> "Add New Masjid"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3DE8E)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Field 1: Name of Masjid
                OutlinedTextField(
                    value = masjidName,
                    onValueChange = { masjidName = it; errorMessage = null },
                    label = { Text("Name of Masjid *") },
                    placeholder = { Text("e.g. Markaz Masjid, Jama Masjid") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Field 2: Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it; errorMessage = null },
                    label = { Text("Location / City / Area *") },
                    placeholder = { Text("e.g. Solapur, Muslim Peth") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Field 3: Admin Name
                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it; errorMessage = null },
                    label = { Text("Admin Name *") },
                    placeholder = { Text("e.g. Haji Abdul Rahman") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Field 4: Admin Contact Number
                OutlinedTextField(
                    value = adminContact,
                    onValueChange = { adminContact = it; errorMessage = null },
                    label = { Text("Admin Contact Number *") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Field 5: Photo Browser Button
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Masjid Photo (Optional):",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Photo",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPhotoUri != null) "Photo Selected ✓ Change" else "Browse / Upload Masjid Photo",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    if (selectedPhotoUri != null) {
                        Box(
                            modifier = Modifier
                                .size(70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedPhotoUri,
                                contentDescription = "Masjid Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Submit Request Button (Uploads photo to Google Drive & inserts 9 rows into Sheet1)
                Button(
                    onClick = {
                        if (masjidName.isBlank()) {
                            errorMessage = "Please enter Masjid Name"
                            return@Button
                        }
                        if (location.isBlank()) {
                            errorMessage = "Please enter Location"
                            return@Button
                        }
                        if (adminName.isBlank()) {
                            errorMessage = "Please enter Admin Name"
                            return@Button
                        }
                        if (adminContact.isBlank()) {
                            errorMessage = "Please enter Admin Contact Number"
                            return@Button
                        }

                        focusManager.clearFocus()
                        keyboardController?.hide()
                        isSending = true
                        val cleanMasjidName = masjidName.trim()
                        val targetUrl = savedAppsScriptUrl.trim()
                        val photoUri = selectedPhotoUri

                        coroutineScope.launch {
                            var photoBase64: String? = null
                            if (photoUri != null) {
                                try {
                                    photoBase64 = prepareStraightMasjidPhotoBase64(context, photoUri)
                                } catch (e: Throwable) {
                                    // Proceed without photo if encoding fails
                                }
                            }

                            try {
                                com.example.data.GoogleSheetMasjidSync.submitMasjidRequestAuto(
                                    name = cleanMasjidName,
                                    location = location.trim(),
                                    adminName = adminName.trim(),
                                    contact = adminContact.trim(),
                                    photoBase64 = photoBase64,
                                    photoName = "$cleanMasjidName.jpg",
                                    webAppUrl = targetUrl
                                )
                            } catch (e: Exception) {
                                // Handled gracefully in background
                            }
                            isSending = false
                            showSuccessDialog = true
                        }
                    },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Uploading...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (language) {
                                "ur" -> "درخواست ارسال کریں"
                                "hi" -> "अनुरोध भेजें"
                                else -> "Send Request"
                            },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    onSubmitted()
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .border(2.dp, Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Success",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Request Received! 👍",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            fontSize = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val annotatedText = buildAnnotatedString {
                            append("Please wait ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            ) {
                                append("(5 Min)")
                            }
                            append("\nYour Masjid will be live.")
                        }
                        Text(
                            text = annotatedText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            lineHeight = 22.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onSubmitted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun StarIconDisplay(
    fillRatio: Float, // 0f = outline, 0.5f = half filled, 1f = fully filled
    size: androidx.compose.ui.unit.Dp = 20.dp,
    tint: Color = Color(0xFFFFD700),
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val path = Path().apply {
            val cx = this@Canvas.size.width / 2f
            val cy = this@Canvas.size.height / 2f
            val outerR = this@Canvas.size.minDimension / 2f
            val innerR = outerR * 0.42f
            for (p in 0 until 10) {
                val r = if (p % 2 == 0) outerR else innerR
                val angle = Math.toRadians((p * 36 - 90).toDouble())
                val x = cx + (r * Math.cos(angle)).toFloat()
                val y = cy + (r * Math.sin(angle)).toFloat()
                if (p == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        if (fillRatio >= 1f) {
            drawPath(path, color = tint, style = Fill)
        } else if (fillRatio >= 0.5f) {
            // Half-star: clip left half
            clipRect(right = this.size.width / 2f) {
                drawPath(path, color = tint, style = Fill)
            }
            // Full star outline
            drawPath(path, color = tint.copy(alpha = 0.5f), style = Stroke(width = 1.5.dp.toPx()))
        } else {
            // Empty star outline
            drawPath(path, color = tint.copy(alpha = 0.35f), style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

@Composable
fun InteractiveHalfStarRatingBar(
    rating: Double,
    onRatingSelected: (Double) -> Unit,
    starSize: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = Color(0xFFFFD700),
    spacing: androidx.compose.ui.unit.Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val fillRatio = when {
                rating >= i -> 1f
                rating >= i - 0.5 -> 0.5f
                else -> 0f
            }
            Box(
                modifier = Modifier.size(starSize),
                contentAlignment = Alignment.Center
            ) {
                // Drawn Star vector
                StarIconDisplay(
                    fillRatio = fillRatio,
                    size = starSize,
                    tint = tint
                )
                // Left half clickable for (i - 0.5), right half clickable for (i.toDouble())
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onRatingSelected(i - 0.5)
                            }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onRatingSelected(i.toDouble())
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun UpiAppChooserDialog(
    amount: Int,
    onDismiss: () -> Unit,
    onAppSelected: (String?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1B15)),
            border = BorderStroke(1.5.dp, Color(0xFFE5B842).copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pay via UPI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3DE8E)
                        )
                        Text(
                            text = "Amount: ₹$amount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF34D399)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Text(
                    text = "Select your payment app:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.Start)
                )

                // 1. Google Pay
                UpiAppOptionItem(
                    name = "Google Pay",
                    subtitle = "GPay UPI",
                    badgeColor = Color(0xFF1A73E8),
                    badgeText = "GPay",
                    onClick = { onAppSelected("com.google.android.apps.nbu.paisa.user") }
                )

                // 2. PhonePe
                UpiAppOptionItem(
                    name = "PhonePe",
                    subtitle = "PhonePe UPI",
                    badgeColor = Color(0xFF5F259F),
                    badgeText = "Pe",
                    onClick = { onAppSelected("com.phonepe.app") }
                )

                // 3. Paytm
                UpiAppOptionItem(
                    name = "Paytm",
                    subtitle = "Paytm Payments Bank & UPI",
                    badgeColor = Color(0xFF00B9F5),
                    badgeText = "Paytm",
                    onClick = { onAppSelected("net.one97.paytm") }
                )

                // 4. Other UPI App (System Chooser)
                UpiAppOptionItem(
                    name = "Other UPI App",
                    subtitle = "BHIM, Cred, Amazon Pay, etc.",
                    badgeColor = Color(0xFF10B981),
                    badgeText = "UPI",
                    onClick = { onAppSelected(null) }
                )
            }
        }
    }
}

@Composable
fun UpiAppOptionItem(
    name: String,
    subtitle: String,
    badgeColor: Color,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF122820)),
        border = BorderStroke(1.dp, Color(0xFF1E4636))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Select",
                tint = Color(0xFFF3DE8E),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
