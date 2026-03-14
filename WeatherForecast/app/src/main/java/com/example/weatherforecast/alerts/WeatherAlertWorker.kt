package com.example.weatherforecast.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.weatherforecast.db.WeatherDatabase
import com.example.weatherforecast.datasource.local.WeatherLocalDataSource
import com.example.weatherforecast.datasource.remote.WeatherRemoteDataSource
import com.example.weatherforecast.network.RetrofitClient
import com.example.weatherforecast.repository.WeatherRepositoryImpl
import com.example.weatherforecast.settings.SettingsDataStore
import kotlinx.coroutines.flow.first
import com.example.weatherforecast.R

class WeatherAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "weather_alerts_channel"
        const val WORK_NAME = "weather_alert_check"
    }

    override suspend fun doWork(): Result {
        return try {
            val database = WeatherDatabase.getInstance(context)
            val localDataSource = WeatherLocalDataSource(
                database.favoriteLocationDao(),
                database.weatherAlertDao(),
                database.cachedForecastDao()
            )
            val remoteDataSource = WeatherRemoteDataSource(RetrofitClient.weatherApiService)
            val repository = WeatherRepositoryImpl(remoteDataSource, localDataSource)
            val settingsDataStore = SettingsDataStore(context)

            val currentTime = System.currentTimeMillis()
            val activeAlerts = repository.getActiveAlerts(currentTime)

            if (activeAlerts.isEmpty()) return Result.success()

            val tempUnit = settingsDataStore.temperatureUnit.first()
            val lang = settingsDataStore.language.first()
            val locationMode = settingsDataStore.locationMode.first()

            val lat = settingsDataStore.mapLat.first()
            val lon = settingsDataStore.mapLon.first()

            val forecast = repository.getForecast(lat, lon, tempUnit, lang).first()
            val current = forecast.list.firstOrNull() ?: return Result.success()


            val localizedContext = com.example.weatherforecast.settings.LocaleHelper.setLocale(context, lang)

            val weatherId = current.weather.firstOrNull()?.id ?: 0
            val temp = current.main.temp
            val windSpeed = current.wind.speed
            val description = current.weather.firstOrNull()?.description ?: ""

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

            val currentStr = localizedContext.getString(R.string.alert_current_weather, description, temp.toInt())
            alertMessages.add(currentStr)

            if (alertMessages.isNotEmpty()) {
                for (alert in activeAlerts) {
                    if (alert.startTime <= currentTime && alert.endTime >= currentTime) {
                        val message = alertMessages.joinToString("\n")
                        if (alert.alertType == "ALARM") {
                            AlarmSoundService.start(context, message, alert.id)
                        } else {
                            val notificationId = System.currentTimeMillis().toInt()
                            showNotification(message, notificationId, alert.id, localizedContext)
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(message: String, notificationId: Int, alertId: Int, localizedContext: Context) {
        createNotificationChannel(localizedContext)
        val dismissIntent = android.content.Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("alert_id", alertId)
            action = "DISMISS"
        }
        val dismissPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId, dismissIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = android.content.Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("alert_id", alertId)
            action = "STOP"
        }
        val stopPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId + 1, stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    private fun showNotificationWithSound(message: String, notificationId: Int, alertId: Int, localizedContext: Context) {
        createNotificationChannel(localizedContext)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        
        val dismissIntent = android.content.Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("alert_id", alertId)
            action = "DISMISS"
        }
        val dismissPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId, dismissIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = android.content.Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("alert_id", alertId)
            action = "STOP"
        }
        val stopPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId + 1, stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    private fun createNotificationChannel(localizedContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = localizedContext.getString(R.string.notification_channel_name)
            val descriptionText = localizedContext.getString(R.string.notification_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
