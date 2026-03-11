package com.bodik.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val CHANNEL_ID = "TimerChannel"
    private val NOTIFICATION_ID = 1

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = LocalBinder()

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null

    private var phaseStartTime = 0L
    private var phaseDuration = 0L

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_WORK_SECONDS = "WORK_SECONDS"
        const val EXTRA_REST_SECONDS = "REST_SECONDS"
        const val EXTRA_REPEATS = "REPEATS"
    }

    fun start(workSeconds: Int, restSeconds: Int, repeats: Int) {

        _state.value = TimerState(
            workSeconds = workSeconds,
            restSeconds = restSeconds,
            totalRepeats = repeats,
            timeLeft = workSeconds,
            currentRepeat = 1,
            isWorkPhase = true,
            isRunning = true
        )

        startPhase(workSeconds)
    }

    private fun startPhase(seconds: Int) {

        phaseDuration = seconds * 1000L
        phaseStartTime = SystemClock.elapsedRealtime()

        startTicking()
    }

    fun pause() {
        timerJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
        updateNotification()
    }

    fun resume() {
        if (!_state.value.isRunning) {
            _state.value = _state.value.copy(isRunning = true)
            phaseStartTime =
                SystemClock.elapsedRealtime() - (phaseDuration - _state.value.timeLeft * 1000L)
            startTicking()
        }
    }

    fun stop() {
        timerJob?.cancel()
        releaseWakeLock()
        _state.value = TimerState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicking() {

        timerJob?.cancel()
        acquireWakeLock()

        timerJob = serviceScope.launch {

            while (_state.value.isRunning) {

                val elapsed = SystemClock.elapsedRealtime() - phaseStartTime
                val remaining = phaseDuration - elapsed

                if (remaining <= 0) {
                    nextPhase()
                    continue
                }

                val seconds = (remaining / 1000).toInt()

                _state.value = _state.value.copy(timeLeft = seconds)

                updateNotification()

                delay(200)
            }
        }
    }

    private fun nextPhase() {

        val s = _state.value

        if (s.isWorkPhase) {

            if (s.restSeconds == 0) {
                nextRound()
            } else {
                _state.value = s.copy(
                    isWorkPhase = false,
                    timeLeft = s.restSeconds
                )
                startPhase(s.restSeconds)
            }

        } else {
            nextRound()
        }
    }

    private fun nextRound() {

        val s = _state.value

        if (s.currentRepeat >= s.totalRepeats) {

            timerJob?.cancel()

            _state.value = s.copy(
                isRunning = false,
                isFinished = true,
                timeLeft = 0
            )

            releaseWakeLock()

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

        } else {

            val next = s.currentRepeat + 1

            _state.value = s.copy(
                isWorkPhase = true,
                timeLeft = s.workSeconds,
                currentRepeat = next
            )

            startPhase(s.workSeconds)
        }
    }

    private fun acquireWakeLock() {

        if (wakeLock?.isHeld == true) return

        val pm = getSystemService(POWER_SERVICE) as PowerManager

        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Timer::WakeLock"
        )

        wakeLock?.acquire(3 * 60 * 60 * 1000L)
    }

    private fun releaseWakeLock() {

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        wakeLock = null
    }

    private fun updateNotification() {

        val s = _state.value

        val phase = if (s.isWorkPhase) getString(R.string.work) else getString(R.string.rest)

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(phase)
            .setContentText(getString(R.string.time_left, formatTime(s.timeLeft)))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .build()

        val manager = getSystemService(NotificationManager::class.java)

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        createNotificationChannel()

        when (intent?.action) {

            ACTION_START -> {

                val work = intent.getIntExtra(EXTRA_WORK_SECONDS, 120)
                val rest = intent.getIntExtra(EXTRA_REST_SECONDS, 30)
                val repeats = intent.getIntExtra(EXTRA_REPEATS, 10)

                val notificationIntent = Intent(this, MainActivity::class.java)

                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.work))
                    .setContentText(getString(R.string.time_left, formatTime(work)))
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentIntent(pendingIntent)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .build()

                startForeground(NOTIFICATION_ID, notification)

                start(work, rest, repeats)
            }

            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stop()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {

        releaseWakeLock()
        serviceScope.cancel()

        super.onDestroy()
    }
}

data class TimerState(
    val workSeconds: Int = 120,
    val restSeconds: Int = 30,
    val totalRepeats: Int = 10,
    val timeLeft: Int = 0,
    val currentRepeat: Int = 0,
    val isWorkPhase: Boolean = true,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)