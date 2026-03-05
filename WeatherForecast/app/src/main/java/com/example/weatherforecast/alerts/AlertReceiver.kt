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

class AlertReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "weather_alerts_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alertType = intent.getStringExtra("alert_type") ?: "NOTIFICATION"
        val lat = intent.getDoubleExtra("lat", 30.0444)
        val lon = intent.getDoubleExtra("lon", 31.2357)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsDataStore = SettingsDataStore(context)
                val tempUnit = settingsDataStore.temperatureUnit.first()
                val lang = settingsDataStore.language.first()

                val database = WeatherDatabase.getInstance(context)
                val localDataSource = WeatherLocalDataSource(
                    database.favoriteLocationDao(),
                    database.weatherAlertDao()
                )
                val remoteDataSource = WeatherRemoteDataSource(RetrofitClient.weatherApiService)
                val repository = WeatherRepositoryImpl(remoteDataSource, localDataSource)

                val forecast = repository.getForecast(lat, lon, tempUnit, lang)
                val current = forecast.list.firstOrNull()

                val weatherId = current?.weather?.firstOrNull()?.id ?: 0
                val temp = current?.main?.temp ?: 0.0
                val windSpeed = current?.wind?.speed ?: 0.0
                val description = current?.weather?.firstOrNull()?.description ?: "Unknown"

                val alertMessages = mutableListOf<String>()

                if (weatherId in 200..599) {
                    alertMessages.add("🌧️ Rain/Storm expected: $description")
                }
                if (weatherId in 600..699) {
                    alertMessages.add("❄️ Snow expected: $description")
                }
                if (weatherId in 700..799) {
                    alertMessages.add("🌫️ Low visibility: $description")
                }
                if (windSpeed > 10) {
                    alertMessages.add("💨 High wind speed: ${"%.1f".format(windSpeed)} m/s")
                }
                if (tempUnit == "metric" && temp < 0) {
                    alertMessages.add("🥶 Very low temperature: ${temp.toInt()}°C")
                }
                if (tempUnit == "metric" && temp > 40) {
                    alertMessages.add("🔥 Very high temperature: ${temp.toInt()}°C")
                }

                val message = if (alertMessages.isNotEmpty()) {
                    alertMessages.joinToString("\n")
                } else {
                    "Current weather: $description, ${temp.toInt()}°"
                }

                if (alertType == "ALARM") {
                    showNotificationWithSound(context, message)
                } else {
                    showNotification(context, message)
                }
            } catch (e: Exception) {
                val fallbackMessage = "Weather alert triggered. Check the app for details."
                if (alertType == "ALARM") {
                    showNotificationWithSound(context, fallbackMessage)
                } else {
                    showNotification(context, fallbackMessage)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, message: String) {
        createNotificationChannel(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Weather Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showNotificationWithSound(context: Context, message: String) {
        createNotificationChannel(context)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Weather Alert!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for weather alerts"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
