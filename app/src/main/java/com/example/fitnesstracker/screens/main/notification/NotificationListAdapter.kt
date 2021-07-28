package com.example.fitnesstracker.screens.main.notification

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
        private val enabledNotificationSwitch =
            item.findViewById<SwitchCompat>(R.id.enabled_notification_switch)

        fun bind(notification: Notification) {
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm:ss,SS", Locale.getDefault())
            timeText.text = date.format(notification.time)
            if (enabledNotificationSwitch.isChecked) {
                enableNotification(notification)
            }
            enabledNotificationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    buttonView.isChecked = false
                    enableNotification(notification)
                } else {
                    buttonView.isChecked = true
                    closeNotification(notification)
                }
            }
            item.setOnClickListener {
                setTime(notification)
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