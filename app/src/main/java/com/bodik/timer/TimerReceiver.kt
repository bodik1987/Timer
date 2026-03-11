package com.bodik.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class TimerReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {

        val serviceIntent = Intent(context, TimerService::class.java)

        serviceIntent.action = TimerService.ACTION_STOP

        context.startForegroundService(serviceIntent)
    }
}