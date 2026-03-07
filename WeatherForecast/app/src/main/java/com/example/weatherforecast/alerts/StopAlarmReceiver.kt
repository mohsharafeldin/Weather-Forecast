package com.example.weatherforecast.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val stopIntent = Intent(context, AlarmSoundService::class.java)
        context.stopService(stopIntent)
    }
}
