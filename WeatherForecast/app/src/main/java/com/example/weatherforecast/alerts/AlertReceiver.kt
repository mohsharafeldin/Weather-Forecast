package com.example.weatherforecast.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.weatherforecast.datasource.local.WeatherLocalDataSource
import com.example.weatherforecast.datasource.remote.WeatherRemoteDataSource
import com.example.weatherforecast.db.WeatherDatabase
import com.example.weatherforecast.network.RetrofitClient
import com.example.weatherforecast.repository.WeatherRepositoryImpl
import com.example.weatherforecast.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.weatherforecast.R
import com.example.weatherforecast.settings.LocaleHelper

class AlertReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "weather_alerts_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alertType = intent.getStringExtra("alert_type") ?: "NOTIFICATION"
        val alertId = intent.getIntExtra("alert_id", -1)
        val lat = intent.getDoubleExtra("lat", 30.0444)
        val lon = intent.getDoubleExtra("lon", 31.2357)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsDataStore = SettingsDataStore(context)
                val tempUnit = settingsDataStore.temperatureUnit.first()
                val lang = settingsDataStore.language.first()
                val localizedContext = LocaleHelper.setLocale(context, lang)

                val database = WeatherDatabase.getInstance(context)
                val localDataSource = WeatherLocalDataSource(
                    database.favoriteLocationDao(),
                    database.weatherAlertDao(),
                    database.cachedForecastDao()
                )
                val remoteDataSource = WeatherRemoteDataSource(RetrofitClient.weatherApiService)
                val repository = WeatherRepositoryImpl(remoteDataSource, localDataSource)

                val cachedForecast = repository.getCachedForecastSync()
                val current = cachedForecast?.list?.firstOrNull()

                val weatherId = current?.weather?.firstOrNull()?.id ?: 0
                val temp = current?.main?.temp ?: 0.0
                val windSpeed = current?.wind?.speed ?: 0.0
                val description = current?.weather?.firstOrNull()?.description ?: "Unknown"

                val alertMessages = mutableListOf<String>()

                if (weatherId in 200..599) {
                    alertMessages.add(localizedContext.getString(R.string.alert_rain, description))
                }
                if (weatherId in 600..699) {
                    alertMessages.add(localizedContext.getString(R.string.alert_snow, description))
                }
                if (weatherId in 700..799) {
                    alertMessages.add(localizedContext.getString(R.string.alert_fog, description))
                }
                if (windSpeed > 10) {
                    alertMessages.add(localizedContext.getString(R.string.alert_wind, com.example.weatherforecast.utils.formatLocal(windSpeed, 1)))
                }
                if (tempUnit == "metric" && temp < 0) {
                    alertMessages.add(localizedContext.getString(R.string.alert_temp_low, temp.toInt()))
                }
                if (tempUnit == "metric" && temp > 40) {
                    alertMessages.add(localizedContext.getString(R.string.alert_temp_high, temp.toInt()))
                }

                val message = if (alertMessages.isNotEmpty()) {
                    alertMessages.joinToString("\n")
                } else {
                    localizedContext.getString(R.string.alert_current_weather, description, temp.toInt())
                }

                if (alertType == "ALARM") {
                    AlarmSoundService.start(context, message, alertId)
                } else {
                    val notificationId = System.currentTimeMillis().toInt()
                    showNotification(context, localizedContext, message, notificationId, alertId)
                }
            } catch (e: Exception) {
                val lang = try { SettingsDataStore(context).language.first() } catch (_: Exception) { "en" }
                val localizedContext = LocaleHelper.setLocale(context, lang)
                val fallbackMessage = localizedContext.getString(R.string.alert_fallback)
                if (alertType == "ALARM") {
                    AlarmSoundService.start(context, fallbackMessage, alertId)
                } else {
                    val notificationId = System.currentTimeMillis().toInt()
                    showNotification(context, localizedContext, fallbackMessage, notificationId, alertId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, localizedContext: Context, message: String, notificationId: Int, alertId: Int) {
        createNotificationChannel(context, localizedContext)
        
        val dismissIntent = Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("alert_id", alertId)
            action = "DISMISS"
        }
        val dismissPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId, dismissIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("alert_id", alertId)
            action = "STOP"
        }
        val stopPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId + 1, stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(localizedContext.getString(R.string.weather_alert))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                localizedContext.getString(R.string.snooze),
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_delete,
                localizedContext.getString(R.string.stop),
                stopPendingIntent
            )
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun showNotificationWithSound(context: Context, localizedContext: Context, message: String, notificationId: Int, alertId: Int) {
        createNotificationChannel(context, localizedContext)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        
        val dismissIntent = Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("alert_id", alertId)
            action = "DISMISS"
        }
        val dismissPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId, dismissIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("alert_id", alertId)
            action = "STOP"
        }
        val stopPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId + 1, stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(localizedContext.getString(R.string.weather_alarm))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                localizedContext.getString(R.string.snooze),
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_delete,
                localizedContext.getString(R.string.stop),
                stopPendingIntent
            )
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context, localizedContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                localizedContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = localizedContext.getString(R.string.notification_channel_desc)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
