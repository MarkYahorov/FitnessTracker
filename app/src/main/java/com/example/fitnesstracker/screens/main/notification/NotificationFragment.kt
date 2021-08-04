package com.example.fitnesstracker.screens.main.notification

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Bundle
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
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_HOUR
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_MINUTE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ID
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.POSITION_IN_LIST
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectDbHelper
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
    private var builder: AlertDialog.Builder? = null
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
        return view
    }

    private fun getNotificationFromDb() {
        repo.getListOfNotification()
            .continueWith({
                notificationList.addAll(it.result)
                notificationRecyclerView.adapter?.notifyDataSetChanged()
            }, Task.UI_THREAD_EXECUTOR)
    }

    private fun initAll(view: View) {
        notificationRecyclerView = view.findViewById(R.id.notification_recycler)
        addNotificationBtn = view.findViewById(R.id.add_notification_btn)
        builder = AlertDialog.Builder(requireContext())
    }

    private fun initRecycler() {
        with(notificationRecyclerView) {
            adapter = NotificationListAdapter(notificationList = notificationList,
                enableNotification = {
                    setAlarmManager(it.id, it.date, it.hours, it.minutes)
                }, closeNotification = {
                    createAlertDialog(it)
                }, setTime = {
                    updateAlarm(it.id, it.position)
                })
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun createAlertDialog(notification: Notification) {
        builder?.setPositiveButton("YES") { dialog, _ ->
            setCancelAlarmBtnClickListener(notification.id)
            App.INSTANCE.db.execSQL("DELETE FROM NotificationTime WHERE $ID = ${notification.id}")
            notificationList.removeAt(notification.position)
            notificationRecyclerView.adapter?.notifyItemRemoved(notification.id)
            dialog.dismiss()
            dialog.cancel()
        }
        builder?.setNegativeButton("NO") { dialog, _ ->
            dialog.dismiss()
            dialog.cancel()
        }
        builder?.setTitle("ALARM")
        builder?.setMessage("Sure?")
        builder?.setIcon(R.drawable.ic_baseline_error_outline_24)
        builder?.show()
    }

    private fun updateAlarm(currentId: Int, position: Int) {
        val datePicker = createDataBicker()
        datePicker.show(childFragmentManager, "DATE_PICKER")
        datePicker.addOnPositiveButtonClickListener {
            currentDate = datePicker.selection!!
            val timePicker = createTimePicker()
            timePicker.show(childFragmentManager, "TIME_PICKER")
            timePicker.addOnPositiveButtonClickListener {
                currentTime = ((timePicker.hour * 3600000) + (timePicker.minute * 60000)).toLong()
                currentAlarmTime = currentDate + currentTime
                val calendar = Calendar.getInstance()
                calendar.time = Date(datePicker.selection!!)
                calendar[Calendar.HOUR_OF_DAY] = timePicker.hour
                calendar[Calendar.MINUTE] = timePicker.minute
                if (calendar.time.time <= Calendar.getInstance().timeInMillis) {
                    Toast.makeText(requireContext(), "Select a future date", Toast.LENGTH_LONG)
                        .show()
                } else {
                    updateNotificationsInDb(
                        datePicker.selection!!,
                        timePicker.hour,
                        timePicker.minute,
                        currentId
                    )
                    setAlarmManager(
                        currentId + 1,
                        datePicker.selection!!,
                        timePicker.hour,
                        timePicker.minute
                    )
                    notificationList[position].date = datePicker.selection!!
                    notificationList[position].hours = timePicker.hour
                    notificationList[position].minutes = timePicker.minute
                    notificationRecyclerView.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun updateNotificationsInDb(updateValue: Long, hours: Int, minutes: Int, id: Int) {
        App.INSTANCE.db.compileStatement("UPDATE NotificationTime SET time = $updateValue, hour = $hours, minute = $minutes WHERE id=$id")
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
                createAlarmManagerForInsertIntoDb(datePicker, timePicker)
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

    private fun createAlarmManagerForInsertIntoDb(
        datePicker: MaterialDatePicker<Long>,
        timePicker: MaterialTimePicker
    ) {
        currentTime = ((timePicker.hour * 3600000) + (timePicker.minute * 60000)).toLong()
        currentAlarmTime = currentDate + currentTime
        val calendar = Calendar.getInstance()
        calendar.time = Date(datePicker.selection!!)
        calendar[Calendar.HOUR_OF_DAY] = timePicker.hour
        calendar[Calendar.MINUTE] = timePicker.minute
        if (calendar.time.time <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(requireContext(), "Select a future date", Toast.LENGTH_LONG)
                .show()
        } else {
            insertNotificationInDb(datePicker.selection!!, timePicker.hour, timePicker.minute)
            setAlarmManager(
                notificationList.size + 1,
                datePicker.selection!!,
                timePicker.hour,
                timePicker.minute
            )
            val id = getLastNotificationFromDb()
            notificationList.add(
                Notification(
                    id,
                    datePicker.selection!!,
                    notificationList.size,
                    timePicker.hour,
                    timePicker.minute
                )
            )
            notificationRecyclerView.adapter?.notifyDataSetChanged()
        }

    }

    private fun insertNotificationInDb(alarmDate: Long, alarmHours: Int, alarmMinutes: Int) {
        InsertDBHelper()
            .setTableName("NotificationTime")
            .addFieldsAndValuesToInsert(
                FitnessDatabase.NOTIFICATION_TIME,
                alarmDate.toString()
            )
            .addFieldsAndValuesToInsert(CURRENT_HOUR, alarmHours.toString())
            .addFieldsAndValuesToInsert(CURRENT_MINUTE, alarmMinutes.toString())
            .addFieldsAndValuesToInsert(POSITION_IN_LIST, notificationList.size.toString())
            .insertTheValues(App.INSTANCE.db)
    }

    private fun getLastNotificationFromDb(): Int {
        var cursor: Cursor? = null
        var id = 0
        try {
            cursor = SelectDbHelper()
                .nameOfTable("NotificationTime")
                .selectParams("max($ID) as $ID")
                .select(App.INSTANCE.db)
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(ID)
                do {
                    id = cursor.getInt(idIndex)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return id
    }

    private fun setAlarmManager(channelMustHave: Int, date: Long, hours: Int, minutes: Int) {
        val alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
            .putExtra("NEW_REQUEST_CODE", channelMustHave)
        val pendingIntent =
            PendingIntent.getBroadcast(requireContext(), channelMustHave, intent, 0)
        val calendar = Calendar.getInstance()
        calendar.time = Date(date)
        calendar[Calendar.HOUR_OF_DAY] = hours
        calendar[Calendar.MINUTE] = minutes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (System.currentTimeMillis() < calendar.time.time) {
                val info = AlarmManager.AlarmClockInfo(calendar.time.time, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        }
    }

    private fun setCancelAlarmBtnClickListener(channelMustHave: Int) {
        alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), channelMustHave, intent, 0)
        alarmManager.cancel(pendingIntent)
    }
}