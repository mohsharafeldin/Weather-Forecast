package com.example.weatherforecast.alerts

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.weatherforecast.alerts.AlertScheduler
import com.example.weatherforecast.db.WeatherDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.weatherforecast.settings.SettingsDataStore

class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationId = intent.getIntExtra("notification_id", -1)
        val alertId = intent.getIntExtra("alert_id", -1)

        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }

        if (alertId != -1 && (action == "STOP" || action == "DISMISS")) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = WeatherDatabase.getInstance(context)
                    val dao = db.weatherAlertDao()
                    val alert = dao.getAlertById(alertId)
                    if (alert != null) {
                        if (alert.alertType == "ALARM") {
                            val stopIntentService = Intent(context, AlarmSoundService::class.java)
                            context.stopService(stopIntentService)
                        }

                        AlertScheduler.cancel(context, alert)
                        
                        val newStartTime = if (action == "STOP") {
                            var nextTime = alert.startTime
                            val now = System.currentTimeMillis()
                            while (nextTime <= now) {
                                nextTime += 24 * 60 * 60 * 1000L
                            }
                            nextTime
                        } else {
                            System.currentTimeMillis() + alert.snoozeDuration * 60 * 1000L
                        }
                        
                        if (newStartTime <= alert.endTime) {
                            val updatedAlert = alert.copy(startTime = newStartTime)
                            dao.update(updatedAlert)
                            
                            val settings = SettingsDataStore(context)
                            val lat = settings.mapLat.first()
                            val lon = settings.mapLon.first()
                            AlertScheduler.schedule(context, updatedAlert, lat, lon)
                        } else {
                            dao.update(alert.copy(isEnabled = false))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
