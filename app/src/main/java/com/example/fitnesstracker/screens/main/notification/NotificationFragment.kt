package com.example.fitnesstracker.screens.main.notification

import android.app.*
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.AlarmReceiver
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.notification.Notification
import com.google.android.material.datepicker.DateSelector
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class NotificationFragment : Fragment() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var addNotificationBtn: FloatingActionButton
    private lateinit var alarmManager: AlarmManager
    private lateinit var calendar: Calendar

    private val notificationList = mutableListOf<Notification>()
    private var currentDate = 0L
    private var currentTime = 0L
    private var currentAlarmTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)
        initAll(view)
        createNotifyChanel()
        calendar = Calendar.getInstance()
        return view
    }

    private fun createNotifyChanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notChan = NotificationChannel("alarmChanel",
                "Alarm Chanel",
                NotificationManager.IMPORTANCE_HIGH)
            val manager = activity?.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notChan)
        }
    }

    private fun initAll(view: View) {
        notificationRecyclerView = view.findViewById(R.id.notification_recycler)
        addNotificationBtn = view.findViewById(R.id.add_notification_btn)
    }

    private fun initRecycler() {
        with(notificationRecyclerView) {
            adapter = NotificationListAdapter(notificationList = notificationList,
                enableNotification = {
                    setAlarmManager(it.raznica)
                }, closeNotification = {
                    setCancelAlarmBtnClickListener()
                }, setTime = {
                    showTimePicker()
                })
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecycler()
        if (savedInstanceState != null) {
            currentDate = savedInstanceState.getLong("CURRENT_DATE")
            currentAlarmTime = savedInstanceState.getLong("CURRENT_ALARM_TIME")
            currentTime = savedInstanceState.getLong("CURRENT_TIME")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("CURRENT_DATE", currentDate)
        outState.putLong("CURRENT_ALARM_TIME", currentAlarmTime)
        outState.putLong("CURRENT_TIME", currentTime)
    }

    override fun onStart() {
        super.onStart()
        selectTimeBtnClickListener()
    }

    private fun selectTimeBtnClickListener() {
        addNotificationBtn.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val datePicker = MaterialDatePicker.Builder
            .datePicker()
            .build()
        datePicker.show(childFragmentManager, "DATE_BUILDER")
        datePicker.addOnPositiveButtonClickListener {
            currentDate = datePicker.selection!!
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Alarm Time")
                .build()
            timePicker.show(childFragmentManager, "ALARM")
            timePicker.addOnPositiveButtonClickListener {
                currentTime = ((timePicker.hour * 3600000) + (timePicker.minute * 60000)).toLong()
                currentAlarmTime = currentDate + currentTime
                if (currentAlarmTime <= Calendar.getInstance().timeInMillis) {
                    Toast.makeText(requireContext(), "Select a future date", Toast.LENGTH_LONG)
                        .show()
                } else {
                    val raznica = currentAlarmTime - Calendar.getInstance().time.time
                    setAlarmManager(raznica)
                    notificationList.add(Notification(currentAlarmTime,raznica))
                }
            }
        }
    }

    private fun setAlarmManager(raznicaTime:Long) {
        alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, raznicaTime,
                AlarmManager.INTERVAL_DAY, pendingIntent
            )
        }
    }

    private fun setCancelAlarmBtnClickListener() {
        alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, 0)
        alarmManager.cancel(pendingIntent)
    }
}