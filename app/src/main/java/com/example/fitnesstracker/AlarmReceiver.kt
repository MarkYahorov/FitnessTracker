package com.example.fitnesstracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.fitnesstracker.screens.main.IS_FROM_NOTIFICATION
import com.example.fitnesstracker.screens.running.RunningActivity


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createNotifyChanel(context)
        val asd = Intent(context, RunningActivity::class.java)
            .putExtra(IS_FROM_NOTIFICATION, true)
        val currentRequestCode = intent.getIntExtra("NEW_REQUEST_CODE",1)
        Log.e("key", "$currentRequestCode")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, currentRequestCode, asd, 0)
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = Notification.Builder(context, "alarmChanel")
                .setContentTitle(context.getText(R.string.welcome_registr_text))
                .setContentText(context.getText(R.string.login_btn_text))
                .setSmallIcon(R.drawable.ic_baseline_home_24)
                .setContentIntent(pendingIntent)
                .setSound(alarmSound)
                .setAutoCancel(true)
                .setTicker(context.getText(R.string.password_text_hint))
                .build()
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(currentRequestCode, builder)
        }
    }

    private fun createNotifyChanel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notChan = NotificationChannel(
                "alarmChanel",
                "Alarm Chanel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notChan)
        }
    }
}