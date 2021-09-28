package com.example.main.screens.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.core.Constants.PATTERN_DATE_HOURS_MINUTES
import com.example.core.models.notification.Notification
import com.example.main.R
import java.text.SimpleDateFormat
import java.util.*

class NotificationListAdapter(
    private val notificationList: List<Notification>,
    private val enableNotification: (Notification) -> Unit,
    private val closeNotification: (Notification) -> Unit,
    private val changeNotification: (Notification) -> Unit,
    private val calendar: Calendar
) : RecyclerView.Adapter<NotificationListAdapter.ViewHolder>() {

    class ViewHolder(
        private val item: View,
        private val enableNotification: (Notification) -> Unit,
        private val closeNotification: (Notification) -> Unit,
        private val changeTimeOfNotification: (Notification) -> Unit,
        private val calendar: Calendar
    ) : RecyclerView.ViewHolder(item) {
        private val timeText = item.findViewById<TextView>(R.id.notification_time)
        private var date: SimpleDateFormat? = null

        fun bind(notification: Notification) {
            calendar.time = Date(notification.date)
            calendar[Calendar.HOUR_OF_DAY] = notification.hours
            calendar[Calendar.MINUTE] = notification.minutes
            date = SimpleDateFormat(PATTERN_DATE_HOURS_MINUTES, Locale.getDefault())
            timeText.text = date?.format(calendar.time)
            enableNotification(notification)
            item.setOnClickListener {
                changeTimeOfNotification(notification)
            }
            item.setOnLongClickListener {
                closeNotification(notification)
                true
            }
        }

        fun unbind() {
            item.setOnClickListener(null)
            item.setOnLongClickListener(null)
            date = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.notification_item, parent, false
        )
        return ViewHolder(view, enableNotification, closeNotification, changeNotification, calendar)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notificationList[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = notificationList.size
}