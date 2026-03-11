package com.bodik.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class TimerReceiver : BroadcastReceiver() {

    // FIX: убрана аннотация @RequiresApi(O) — startForegroundService доступен
    // с API 26+, а ресивер вызывается только через AlarmManager на тех же версиях.
    // Аннотация была избыточной и вводила в заблуждение.
    override fun onReceive(context: Context, intent: Intent) {

        val serviceIntent = Intent(context, TimerService::class.java)

        serviceIntent.action = TimerService.ACTION_STOP

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}