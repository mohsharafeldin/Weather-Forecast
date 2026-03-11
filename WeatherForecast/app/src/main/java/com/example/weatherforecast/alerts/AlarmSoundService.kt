package com.example.weatherforecast.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.weatherforecast.R

class AlarmSoundService : Service() {

    companion object {
        const val CHANNEL_ID = "weather_alarm_channel"
        const val NOTIFICATION_ID = 9999
        const val EXTRA_MESSAGE = "alarm_message"
        const val EXTRA_ALERT_ID = "alert_id"

        fun start(context: Context, message: String, alertId: Int) {
            val intent = Intent(context, AlarmSoundService::class.java)
            intent.putExtra(EXTRA_MESSAGE, message)
            intent.putExtra(EXTRA_ALERT_ID, alertId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.weather_alarm)
        val alertId = intent?.getIntExtra(EXTRA_ALERT_ID, -1) ?: -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weather Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Persistent alarm for weather alerts"
                setSound(null, null)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val dismissIntent = Intent(this, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", NOTIFICATION_ID)
            putExtra("alert_id", alertId)
            action = "DISMISS"
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, NOTIFICATION_ID, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, StopAlarmReceiver::class.java).apply {
            putExtra("notification_id", NOTIFICATION_ID)
            putExtra("alert_id", alertId)
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, NOTIFICATION_ID + 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.weather_alarm))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.snooze),
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_delete,
                getString(R.string.stop),
                stopPendingIntent
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)

        startAlarmSound()

        return START_NOT_STICKY
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmSoundService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        super.onDestroy()
    }
}
