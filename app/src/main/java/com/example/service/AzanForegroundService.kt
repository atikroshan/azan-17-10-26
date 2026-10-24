package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow

class AzanForegroundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: TelephonyCallback? = null

    private var isPowerReceiverRegistered = false
    private var lastPowerPressTime = 0L
    private var powerPressCount = 0

    private val powerButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON) {
                if (isPlayingAzan.value) {
                    val now = System.currentTimeMillis()
                    if (now - lastPowerPressTime <= 2500L) {
                        powerPressCount++
                    } else {
                        powerPressCount = 1
                    }
                    lastPowerPressTime = now
                    android.util.Log.d("AzanForegroundService", "Power button toggle: action=$action, count=$powerPressCount")
                    if (powerPressCount >= 2) {
                        android.util.Log.d("AzanForegroundService", "Double power button press detected while locked! Stopping audio.")
                        powerPressCount = 0
                        lastPowerPressTime = 0L
                        stopAzanAudio()
                    }
                }
            }
        }
    }

    companion object {
        var serviceInstance: AzanForegroundService? = null
        val isPlayingAzan = MutableStateFlow(false)
        val isMuted = MutableStateFlow(false)
        val currentPlayingPrayerName = MutableStateFlow<String?>(null)
        val lastAudioFinishedTime = MutableStateFlow(0L)
        val lastAudioPrayerIndex = MutableStateFlow(-1)

        fun toggleMuteService(context: Context) {
            try {
                serviceInstance?.toggleMuteAudio()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        fun getPrayerKey(prayerName: String?): String {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            val day = cal.get(java.util.Calendar.DAY_OF_YEAR)
            val year = cal.get(java.util.Calendar.YEAR)
            val raw = (prayerName ?: "unknown").lowercase().trim()
            val normalized = when (raw) {
                "jumah", "juma" -> "dhuhr"
                else -> raw
            }
            return "$year-$day-$normalized"
        }

        fun isPrayerStoppedOrHandled(context: Context, prayerName: String?): Boolean {
            try {
                val key = getPrayerKey(prayerName)
                val prefs = context.getSharedPreferences("azan_prefs", Context.MODE_PRIVATE)
                val handledKeys = prefs.getStringSet("handled_prayers_set", emptySet()) ?: emptySet()
                return handledKeys.contains(key)
            } catch (e: Exception) {
                return false
            }
        }

        fun markPrayerHandled(context: Context, prayerName: String?) {
            try {
                val key = getPrayerKey(prayerName)
                val prefs = context.getSharedPreferences("azan_prefs", Context.MODE_PRIVATE)
                val currentSet = prefs.getStringSet("handled_prayers_set", emptySet()) ?: emptySet()
                val newSet = HashSet(currentSet)
                newSet.add(key)
                if (newSet.size > 20) {
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
                    val todayDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
                    val todayYear = cal.get(java.util.Calendar.YEAR)
                    newSet.retainAll { it.startsWith("$todayYear-$todayDay-") }
                    newSet.add(key)
                }
                prefs.edit().putStringSet("handled_prayers_set", newSet).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            val playingName = currentPlayingPrayerName.value
            if (playingName != null) {
                markPrayerHandled(context, playingName)
            }
            try {
                serviceInstance?.stopAzanAudio()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            try {
                val stopIntent = Intent(context, AzanForegroundService::class.java).apply {
                    action = "STOP_AZAN"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(stopIntent)
                } else {
                    context.startService(stopIntent)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        fun prayerNameToIndex(name: String?): Int {
            return when (name?.lowercase()?.trim()) {
                "fajr" -> 0
                "dhuhr" -> 1
                "jumah", "juma" -> 1
                "asr" -> 2
                "maghrib" -> 3
                "isha" -> 4
                "tahajjud" -> 5
                else -> -1
            }
        }

        fun initFromPrefs(context: Context) {
            try {
                val prefs = context.getSharedPreferences("azan_prefs", Context.MODE_PRIVATE)
                val savedTime = prefs.getLong("last_audio_finished_time", 0L)
                val savedIdx = prefs.getInt("last_audio_prayer_index", -1)
                if (savedTime > lastAudioFinishedTime.value) {
                    lastAudioFinishedTime.value = savedTime
                    lastAudioPrayerIndex.value = savedIdx
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun recordAudioFinished(context: Context, timeMs: Long, index: Int) {
            lastAudioFinishedTime.value = timeMs
            try {
                val prefs = context.getSharedPreferences("azan_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong("last_audio_finished_time", timeMs)
                    .putInt("last_audio_prayer_index", index)
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupCallListener()
        setupPowerButtonListener()
    }

    private fun setupPowerButtonListener() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(powerButtonReceiver, filter)
            isPowerReceiverRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupCallListener() {
        try {
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                phoneStateListener = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        if (state != TelephonyManager.CALL_STATE_IDLE) {
                            // Call active, pause or stop
                            stopAzanAudio()
                        }
                    }
                }
                telephonyManager?.registerTelephonyCallback(mainExecutor, phoneStateListener as TelephonyCallback)
            } else {
                @Suppress("DEPRECATION")
                telephonyManager?.listen(object : android.telephony.PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        if (state != TelephonyManager.CALL_STATE_IDLE) {
                            stopAzanAudio()
                        }
                    }
                }, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleMuteAudio() {
        try {
            val nextMuted = !isMuted.value
            isMuted.value = nextMuted
            if (nextMuted) {
                mediaPlayer?.setVolume(0f, 0f)
            } else {
                mediaPlayer?.setVolume(1f, 1f)
            }
            updateNotification()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun updateNotification() {
        try {
            val azanName = currentPlayingPrayerName.value ?: "Azan"
            val stopIntent = Intent(this, AzanForegroundService::class.java).apply {
                action = "STOP_AZAN"
            }
            val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val muteIntent = Intent(this, AzanForegroundService::class.java).apply {
                action = "MUTE_TOGGLE"
            }
            val mutePendingIntent = PendingIntent.getService(this, 1, muteIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val muteLabel = if (isMuted.value) "Unmute" else "Mute"

            val mainIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("FROM_ALARM", true)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val notification = NotificationCompat.Builder(this, "azan_channel")
                .setContentTitle("It's time for $azanName")
                .setContentText(if (isMuted.value) "Azan is muted" else "Azan is playing")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(mainPendingIntent)
                .addAction(R.mipmap.ic_launcher, "Stop", stopPendingIntent)
                .addAction(R.mipmap.ic_launcher, muteLabel, mutePendingIntent)
                .setOngoing(true)
                .build()

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(1, notification)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceInstance = this
        initFromPrefs(this)
        val azanName = intent?.getStringExtra("AZAN_NAME") ?: "Azan"
        val pIdx = prayerNameToIndex(azanName)
        if (pIdx != -1) {
            lastAudioPrayerIndex.value = pIdx
        }

        if (intent?.action == "STOP_AZAN") {
            stopAzanAudio()
            return START_NOT_STICKY
        }
        if (intent?.action == "MUTE_TOGGLE") {
            toggleMuteAudio()
            return START_NOT_STICKY
        }

        // GUARD: If this prayer was already stopped or played today, do not play again!
        if (isPrayerStoppedOrHandled(this, azanName)) {
            android.util.Log.d("AzanForegroundService", "Prayer $azanName has already been completed or stopped today. Skipping.")
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                // Ignore
            }
            stopSelf()
            return START_NOT_STICKY
        }

        isMuted.value = false
        currentPlayingPrayerName.value = azanName
        
        createNotificationChannel()
        
        val stopIntent = Intent(this, AzanForegroundService::class.java).apply {
            action = "STOP_AZAN"
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val muteIntent = Intent(this, AzanForegroundService::class.java).apply {
            action = "MUTE_TOGGLE"
        }
        val mutePendingIntent = PendingIntent.getService(this, 1, muteIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val currentMuted = isMuted.value
        val muteLabel = if (currentMuted) "Unmute" else "Mute"

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("FROM_ALARM", true)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, "azan_channel")
            .setContentTitle("It's time for $azanName")
            .setContentText("Azan is playing")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(mainPendingIntent)
            .setFullScreenIntent(mainPendingIntent, true)
            .addAction(R.mipmap.ic_launcher, "Stop", stopPendingIntent)
            .addAction(R.mipmap.ic_launcher, muteLabel, mutePendingIntent)
            .setOngoing(true)
            .build()
            
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Requirement: Keep CPU running to ensure audio plays completely even in lock mode
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            if (wakeLock == null || wakeLock?.isHeld != true) {
                wakeLock = pm?.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,
                    "offlineazan:service_audio_wake"
                )
                wakeLock?.acquire(15 * 60 * 1000L) // 15 mins max safety timeout
            }
            startActivity(mainIntent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        if (!isPlayingAzan.value) {
            playAzan()
        }

        return START_NOT_STICKY
    }

    fun stopAzanAudio() {
        val playingName = currentPlayingPrayerName.value
        if (playingName != null) {
            markPrayerHandled(this, playingName)
        }
        isPlayingAzan.value = false
        isMuted.value = false
        currentPlayingPrayerName.value = null
        recordAudioFinished(this, System.currentTimeMillis(), lastAudioPrayerIndex.value)
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAzan() {
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        mediaPlayer = null

        // Removed volume override to keep original system volume
        
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            android.util.Log.d("AzanForegroundService", "Device is in silent or vibrate mode, skipping audio.")
            stopAzanAudio()
            return
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()

            var player: MediaPlayer? = null

            // Tier 1: Try MediaPlayer.create with USAGE_ALARM
            try {
                player = MediaPlayer.create(applicationContext, R.raw.azan, audioAttributes, 0)
            } catch (e: Throwable) {
                android.util.Log.w("AzanForegroundService", "Tier 1 MediaPlayer.create failed: ${e.message}")
            }

            // Tier 2: Open raw resource file descriptor and prepare properly before closing FD
            if (player == null) {
                try {
                    val afd = resources.openRawResourceFd(R.raw.azan)
                    if (afd != null) {
                        val p = MediaPlayer()
                        p.setAudioAttributes(audioAttributes)
                        p.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        p.prepare()
                        afd.close()
                        player = p
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("AzanForegroundService", "Tier 2 setDataSource failed: ${e.message}")
                }
            }

            // Tier 3: Standard MediaPlayer.create fallback
            if (player == null) {
                try {
                    player = MediaPlayer.create(applicationContext, R.raw.azan)
                } catch (e: Throwable) {
                    android.util.Log.w("AzanForegroundService", "Tier 3 default create failed: ${e.message}")
                }
            }

            mediaPlayer = player
        } catch (e: Exception) {
            android.util.Log.e("AzanForegroundService", "Error initializing MediaPlayer for Azan", e)
            mediaPlayer = null
        }

        try {
            val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxAlarmVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)
            }
        } catch (e: Throwable) {
            android.util.Log.w("AzanForegroundService", "Could not set alarm stream volume: ${e.message}")
        }

        mediaPlayer?.let { player ->
            try {
                player.setWakeMode(applicationContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                player.setVolume(1.0f, 1.0f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            player.isLooping = false
            isPlayingAzan.value = true

            player.setOnCompletionListener {
                android.util.Log.d("AzanForegroundService", "Azan audio playback completed")
                stopAzanAudio()
            }

            player.setOnErrorListener { _, what, extra ->
                android.util.Log.e("AzanForegroundService", "MediaPlayer error: what=$what, extra=$extra")
                stopAzanAudio()
                true
            }

            try {
                player.start()
                val currentPrayer = currentPlayingPrayerName.value
                markPrayerHandled(this, currentPrayer)
                android.util.Log.d("AzanForegroundService", "Azan audio playback started successfully")
            } catch (e: Exception) {
                android.util.Log.e("AzanForegroundService", "Failed to start MediaPlayer", e)
                stopAzanAudio()
            }
        } ?: run {
            android.util.Log.e("AzanForegroundService", "MediaPlayer is null, cannot play Azan audio")
            stopAzanAudio()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceInstance == this) {
            serviceInstance = null
        }
        
        if (isPowerReceiverRegistered) {
            try {
                unregisterReceiver(powerButtonReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isPowerReceiverRegistered = false
        }
        
        // Release wake lock if held
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null

        val playingName = currentPlayingPrayerName.value
        if (playingName != null) {
            markPrayerHandled(this, playingName)
        }
        if (isPlayingAzan.value) {
            recordAudioFinished(this, System.currentTimeMillis(), lastAudioPrayerIndex.value)
        }
        isPlayingAzan.value = false
        currentPlayingPrayerName.value = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "azan_channel",
                "Azan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Azan audio playback service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
