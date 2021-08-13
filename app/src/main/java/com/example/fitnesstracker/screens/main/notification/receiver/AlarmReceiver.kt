package com.example.fitnesstracker.screens.main.notification.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import com.example.fitnesstracker.R
import com.example.fitnesstracker.screens.main.IS_FROM_NOTIFICATION
import com.example.fitnesstracker.screens.main.notification.NotificationFragment.Companion.NEW_REQUEST_CODE
import com.example.fitnesstracker.screens.running.RunningActivity


class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val ALARM_CHANNEL = "alarmChanel"
        private const val ALARM_CHANNEL_NAME = "Alarm Chanel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChanel(context)
        val touchIntent = Intent(context, RunningActivity::class.java)
            .putExtra(IS_FROM_NOTIFICATION, true)
        val currentRequestCode = intent.getIntExtra(NEW_REQUEST_CODE, 1)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, currentRequestCode, touchIntent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = createNotification(context, pendingIntent)
            NotificationManagerCompat.from(context).notify(currentRequestCode, notification)
        }
    }

    private fun createNotification(context: Context, pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, ALARM_CHANNEL)
            .setContentTitle(context.getText(R.string.run))
            .setContentText(context.getText(R.string.must_run))
            .setSmallIcon(R.drawable.ic_run)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChanel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL,
                ALARM_CHANNEL_NAME,
                IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}