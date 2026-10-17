package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.receiver.AzanAlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAzan(name: String, timeString: String, tomorrowTimeString: String? = null) { // timeString is "HH:mm"
        val intent = Intent(context, AzanAlarmReceiver::class.java).apply {
            putExtra("AZAN_NAME", name)
        }
        
        // request code based on name hash to be unique per azan
        val requestCode = name.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parts = timeString.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        val nowMillis = System.currentTimeMillis()

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If time has already passed today, schedule for tomorrow using tomorrow's appropriate prayer time
        if (calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val effectiveTomTime = tomorrowTimeString ?: timeString
            val tomParts = effectiveTomTime.split(":")
            if (tomParts.size == 2) {
                val tomHour = tomParts[0].toIntOrNull() ?: hour
                val tomMin = tomParts[1].toIntOrNull() ?: minute
                calendar.set(Calendar.HOUR_OF_DAY, tomHour)
                calendar.set(Calendar.MINUTE, tomMin)
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // For demo environment fallback to standard alarm if exact missing
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun cancelAzan(name: String) {
        try {
            val intent = Intent(context, AzanAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
