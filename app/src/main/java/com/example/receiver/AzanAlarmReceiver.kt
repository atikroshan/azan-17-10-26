package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.AzanForegroundService
import java.util.Calendar
import java.util.TimeZone

class AzanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val azanName = intent.getStringExtra("AZAN_NAME") ?: return
        Log.d("AzanAlarmReceiver", "Received alarm for $azanName")

        val nowCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val isFriday = nowCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)

        // Safety Guard: On non-Friday days (e.g. Saturday), Dhuhr is Zohar (13:15 / 1:15 PM).
        // If an early Jumah alarm (around 12:30 PM) was scheduled by Android AlarmManager on a Saturday or weekday,
        // do not play azan audio early at 12:30. Reschedule for true Zohar time instead.
        if (azanName.equals("Dhuhr", ignoreCase = true) && !isFriday) {
            if (currentHour < 13) {
                Log.w("AzanAlarmReceiver", "Preventing early Dhuhr alarm at hour $currentHour on non-Friday. Rescheduling for Zohar 13:15.")
                val scheduler = com.example.service.AlarmScheduler(context)
                scheduler.scheduleAzan("Dhuhr", "13:15", "13:15")
                return
            }
        }
        
        val serviceIntent = Intent(context, AzanForegroundService::class.java).apply {
            putExtra("AZAN_NAME", azanName)
        }
        
        // Start Foreground Service safely
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Wake screen and launch MainActivity
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            @Suppress("DEPRECATION")
            val wl = pm?.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                android.os.PowerManager.ON_AFTER_RELEASE,
                "offlineazan:alarm_receiver_wake"
            )
            wl?.acquire(10000L)

            val launchIntent = Intent(context, com.example.MainActivity::class.java).apply {
                putExtra("FROM_ALARM", true)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            context.startActivity(launchIntent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
