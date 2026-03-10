package com.bodik.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimerService : Service() {
    private val CHANNEL_ID = "TimerChannel"

    companion object {
        const val PHASE_WORK = "WORK"
        const val PHASE_REST = "REST"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timeLeft = intent?.getStringExtra("TIME_LEFT") ?: "0:00"
        val phase = when (intent?.getStringExtra("PHASE")) {
            "WORK" -> getString(R.string.work)
            "REST" -> getString(R.string.rest)
            else -> getString(R.string.work)
        }

        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(phase)
            .setContentText(
                getString(
                    R.string.time_left,
                    timeLeft
                )
            ) // ← вместо "Осталось: $timeLeft"
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .build()

        startForeground(1, notification)
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}