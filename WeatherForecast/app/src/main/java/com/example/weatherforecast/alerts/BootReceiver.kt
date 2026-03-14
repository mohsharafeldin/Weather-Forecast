package com.example.weatherforecast.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.weatherforecast.db.WeatherDatabase
import com.example.weatherforecast.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bootActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )
        if (intent.action in bootActions) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = WeatherDatabase.getInstance(context)
                    val dao = database.weatherAlertDao()
                    val settings = SettingsDataStore(context)
                    
                    val currentTime = System.currentTimeMillis()
                    val activeAlerts = dao.getActiveAlerts(currentTime)
                    
                    if (activeAlerts.isNotEmpty()) {
                        val lat = settings.mapLat.first()
                        val lon = settings.mapLon.first()
                        
                        for (alert in activeAlerts) {
                            AlertScheduler.schedule(context, alert, lat, lon)
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
