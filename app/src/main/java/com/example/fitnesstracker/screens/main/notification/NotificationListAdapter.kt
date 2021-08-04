package com.example.fitnesstracker.screens.main.notification

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.notification.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationListAdapter(
    private val notificationList: List<Notification>,
    private val enableNotification: (Notification) -> Unit,
    private val closeNotification: (Notification) -> Unit,
    private val setTime: (Notification) -> Unit,
) : RecyclerView.Adapter<NotificationListAdapter.ViewHolder>() {

    class ViewHolder(
        private val item: View,
        private val enableNotification: (Notification) -> Unit,
        private val closeNotification: (Notification) -> Unit,
        private val setTime: (Notification) -> Unit,
    ) : RecyclerView.ViewHolder(item) {
        private val timeText = item.findViewById<TextView>(R.id.notification_time)

        fun bind(notification: Notification) {
            val calendar = Calendar.getInstance()
            calendar.time = Date(notification.date)
            calendar[Calendar.HOUR_OF_DAY] = notification.hours
            calendar[Calendar.MINUTE] = notification.minutes
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            timeText.text = date.format(calendar.time)
            enableNotification(notification)
            item.setOnClickListener {
                setTime(notification)
            }
            item.setOnLongClickListener {
                closeNotification(notification)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.notification_item, parent, false)
        return ViewHolder(view, enableNotification, closeNotification, setTime)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notificationList[position])
    }

    override fun getItemCount(): Int = notificationList.size
}