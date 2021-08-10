package com.example.fitnesstracker.screens.main.notification

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.Intent
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
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ID
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.NOTIFICATION_TIME_NAME
import com.example.fitnesstracker.models.notification.Notification
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

class NotificationFragment : Fragment() {

    companion object {
        const val NEW_REQUEST_CODE = "NEW_REQUEST_CODE"
        const val MAX = "max($ID) as $ID"
        private const val DATE_PICKER = "DATE_PICKER"
        private const val TIME_PICKER = "TIME_PICKER"
        private const val CURRENT_DATE = "CURRENT_DATE"
        private const val CURRENT_ALARM_TIME = "CURRENT_ALARM_TIME"
        private const val CURRENT_TIME = "CURRENT_TIME"
    }

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var addNotificationBtn: FloatingActionButton
    private lateinit var alarmManager: AlarmManager

    private val notificationList = mutableListOf<Notification>()
    private var builder: AlertDialog.Builder? = null
    private var calendar = Calendar.getInstance()
    private val repo = App.INSTANCE.repositoryImpl
    private var currentDate = 0L
    private var currentHour = 0
    private var currentMinutes = 0

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
            adapter = NotificationListAdapter(
                notificationList = notificationList,
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
        builder?.setPositiveButton(R.string.yes) { dialog, _ ->
            setCancelAlarmBtnClickListener(notification.id)
            repo.clearDbWithWereArgs(NOTIFICATION_TIME_NAME, "$ID = ${notification.id}")
                .continueWith({
                    notificationList.removeAt(notification.position)
                    notificationRecyclerView.adapter?.notifyItemRemoved(notification.id)
                    dialog.dismiss()
                    dialog.cancel()
                }, Task.UI_THREAD_EXECUTOR)
        }
        builder?.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
            dialog.cancel()
        }
        builder?.setTitle(R.string.alarm)
        builder?.setMessage(R.string.sure)
        builder?.setIcon(R.drawable.ic_baseline_error_outline_24)
        builder?.show()
    }

    private fun updateAlarm(currentId: Int, position: Int) {
        val datePicker = createDataBicker()
        datePicker.show(childFragmentManager, DATE_PICKER)
        datePicker.addOnPositiveButtonClickListener {
            currentDate = datePicker.selection!!
            val timePicker = createTimePicker()
            timePicker.show(childFragmentManager, TIME_PICKER)
            timePicker.addOnPositiveButtonClickListener {
                setCalendarTime(timePicker)
                updateAlarmInDb(currentId, position)
            }
        }
    }

    private fun updateAlarmInDb(currentId: Int, position: Int) {
        if (calendar.time.time <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(requireContext(), R.string.toast_waring, Toast.LENGTH_LONG)
                .show()
        } else {
            repo.updateNotifications(currentDate, currentHour, currentMinutes, currentId)
            setAlarmManager(
                currentId + 1,
                currentDate,
                currentHour,
                currentMinutes
            )
            notificationList[position].date = currentDate
            notificationList[position].hours = currentHour
            notificationList[position].minutes = currentMinutes
            notificationRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun setCalendarTime(timePicker: MaterialTimePicker) {
        calendar.time = Date(currentDate)
        currentHour = timePicker.hour
        currentMinutes = timePicker.minute
        calendar[Calendar.HOUR_OF_DAY] = currentHour
        calendar[Calendar.MINUTE] = currentMinutes
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getNotificationFromDb()
        initRecycler()
        if (savedInstanceState != null) {
            currentDate = savedInstanceState.getLong(CURRENT_DATE)
            currentMinutes = savedInstanceState.getInt(CURRENT_ALARM_TIME)
            currentHour = savedInstanceState.getInt(CURRENT_TIME)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(CURRENT_DATE, currentDate)
        outState.putInt(CURRENT_ALARM_TIME, currentMinutes)
        outState.putInt(CURRENT_TIME, currentHour)
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
        datePicker.show(childFragmentManager, DATE_PICKER)
        datePicker.addOnPositiveButtonClickListener {
            currentDate = datePicker.selection!!
            val timePicker = createTimePicker()
            timePicker.show(childFragmentManager, TIME_PICKER)
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
            .setTitleText(R.string.select_alarm_time)
            .build()
    }

    private fun createAlarmManagerForInsertIntoDb(
        timePicker: MaterialTimePicker
    ) {
        setCalendarTime(timePicker)
        if (calendar.time.time <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(requireContext(), R.string.toast_waring, Toast.LENGTH_LONG)
                .show()
        } else {
            repo.insertNotification(currentDate, currentHour, currentMinutes, notificationList)
                .continueWith({
                    val id = it.result
                    createNotificationList(id)
                    setAlarmManager(
                        notificationList.size + 1,
                        currentDate,
                        currentHour,
                        currentMinutes
                    )
                    notificationRecyclerView.adapter?.notifyDataSetChanged()
                }, Task.UI_THREAD_EXECUTOR)
        }
    }

    private fun createNotificationList(id: Int) {
        notificationList.add(
            Notification(
                id,
                currentDate,
                notificationList.size,
                currentHour,
                currentMinutes
            )
        )
    }

    private fun setAlarmManager(channelMustHave: Int, date: Long, hours: Int, minutes: Int) {
        val alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
            .putExtra(NEW_REQUEST_CODE, channelMustHave)
        val pendingIntent =
            PendingIntent.getBroadcast(requireContext(), channelMustHave, intent, 0)
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

    override fun onStop() {
        super.onStop()
        addNotificationBtn.setOnClickListener(null)
    }
}