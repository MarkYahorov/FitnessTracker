package com.example.fitnesstracker

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.fitnesstracker.screens.main.IS_FROM_NOTIFICATION
import com.example.fitnesstracker.screens.main.running.RunningActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val asd = Intent(context, RunningActivity::class.java)
            .putExtra(IS_FROM_NOTIFICATION, true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, asd, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = Notification.Builder(context, "alarmChanel")
                .setContentTitle(context.getText(R.string.welcome_registr_text))
                .setContentText(context.getText(R.string.login_btn_text))
                .setSmallIcon(R.drawable.ic_baseline_home_24)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTicker(context.getText(R.string.password_text_hint))
                .build()
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(123, builder)
        }
    }
}