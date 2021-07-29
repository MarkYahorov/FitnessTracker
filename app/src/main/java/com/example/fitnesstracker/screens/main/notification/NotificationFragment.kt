package com.example.fitnesstracker.screens.main.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bolts.Task
import com.example.fitnesstracker.AlarmReceiver
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.models.notification.Notification
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

class NotificationFragment : Fragment() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var addNotificationBtn: FloatingActionButton
    private lateinit var alarmManager: AlarmManager

    private val notificationList = mutableListOf<Notification>()
    private val repo = App.INSTANCE.repositoryImpl
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
        return view
    }

    private fun getNotificationFromDb() {
        repo.getListOfNotification()
            .continueWith({
                if (it.error != null) {
                    Log.e("key", "${it.error.message}")
                } else {
                    it.result.forEach { notification ->
                        notificationList.add(Notification(notification.id, notification.time))
                    }
                    notificationRecyclerView.adapter?.notifyDataSetChanged()
                }
            }, Task.UI_THREAD_EXECUTOR)
    }

    private fun createNotifyChanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notChan = NotificationChannel(
                "alarmChanel",
                "Alarm Chanel",
                NotificationManager.IMPORTANCE_HIGH
            )
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
                    setAlarmManager(it.id, it.time)
                }, closeNotification = {
                    setCancelAlarmBtnClickListener()
                }, setTime = {
                    updateAlarm(it.id)
                })
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun updateAlarm(currentId: Int) {
        val datePicker = createDataBicker()
        datePicker.show(childFragmentManager, "DATE_PICKER")
        datePicker.addOnPositiveButtonClickListener {
            currentDate = datePicker.selection!!
            val timePicker = createTimePicker()
            timePicker.show(childFragmentManager, "TIME_PICKER")
            timePicker.addOnPositiveButtonClickListener {
                currentTime = ((timePicker.hour * 3600000) + (timePicker.minute * 60000)).toLong()
                currentAlarmTime = currentDate + currentTime
                if (currentAlarmTime <= Calendar.getInstance().timeInMillis) {
                    Toast.makeText(requireContext(), "Select a future date", Toast.LENGTH_LONG)
                        .show()
                } else {
                    updateNotificationsInDb(currentAlarmTime, currentId)
                    setAlarmManager(currentId+1, currentAlarmTime)
                    notificationList[currentId-1].time = currentAlarmTime
                    notificationRecyclerView.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun updateNotificationsInDb(updateValue: Long, id: Int) {
        App.INSTANCE.db.compileStatement("UPDATE NotificationTime SET time = $updateValue WHERE id=$id")
            .execute()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getNotificationFromDb()
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
            createAlarm()
        }
    }

    private fun createAlarm() {
        val datePicker = createDataBicker()
        datePicker.show(childFragmentManager, "DATE_PICKER")
        datePicker.addOnPositiveButtonClickListener {
            currentDate = datePicker.selection!!
            val timePicker = createTimePicker()
            timePicker.show(childFragmentManager, "TIME_PICKER")
            timePicker.addOnPositiveButtonClickListener {
                createAlarmManagerForInsertIntoDb(timePicker)
            }
        }
    }

    private fun createDataBicker(): MaterialDatePicker<Long> {
        return MaterialDatePicker.Builder
            .datePicker()
            .build()
    }

    private fun createTimePicker(): MaterialTimePicker {
        return MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select Alarm Time")
            .build()
    }

    private fun createAlarmManagerForInsertIntoDb(timePicker: MaterialTimePicker) {
        currentTime = ((timePicker.hour * 3600000) + (timePicker.minute * 60000)).toLong()
        currentAlarmTime = currentDate + currentTime
        if (currentAlarmTime <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(requireContext(), "Select a future date", Toast.LENGTH_LONG)
                .show()
        } else {
            insertNotificationInDb()
            setAlarmManager(notificationList.size + 1, currentAlarmTime)
            notificationList.add(Notification(notificationList.size, currentAlarmTime))
            notificationRecyclerView.adapter?.notifyDataSetChanged()
        }

    }

    private fun insertNotificationInDb() {
        InsertDBHelper()
            .setTableName("NotificationTime")
            .addFieldsAndValuesToInsert(
                FitnessDatabase.NOTIFICATION_TIME,
                currentAlarmTime.toString()
            )
            .insertTheValues(App.INSTANCE.db)
    }

    private fun setAlarmManager(channelMustHave: Int, triggerAtMillis: Long) {
        val alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
            .putExtra("NEW_REQUEST_CODE", channelMustHave)
        val pendingIntent =
            PendingIntent.getBroadcast(requireContext(), channelMustHave, intent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, triggerAtMillis,
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