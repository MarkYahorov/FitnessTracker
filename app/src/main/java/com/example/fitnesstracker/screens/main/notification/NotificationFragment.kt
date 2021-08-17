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
import com.example.fitnesstracker.screens.main.notification.receiver.AlarmReceiver
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
        private const val CURRENT_POSITION = "CURRENT_POSITION"
    }

    private var notificationRecyclerView: RecyclerView? = null
    private var addNotificationBtn: FloatingActionButton? = null
    private var alarmManager: AlarmManager? = null
    private var alertDialog: AlertDialog.Builder? = null

    private val notificationList = mutableListOf<Notification>()
    private var calendar = Calendar.getInstance()
    private val repo = App.INSTANCE.repositoryImpl
    private var currentDate = 0L
    private var currentHour = 0
    private var currentMinutes = 0
    private var scrollPositionOfRecycler = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)
        initAll(view = view)
        return view
    }

    private fun initAll(view: View) {
        notificationRecyclerView = view.findViewById(R.id.notification_recycler)
        addNotificationBtn = view.findViewById(R.id.add_notification_btn)
        alertDialog = AlertDialog.Builder(requireContext())
        alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getNotificationFromDb()
        initRecycler()
        if (savedInstanceState != null) {
            currentDate = savedInstanceState.getLong(CURRENT_DATE)
            currentMinutes = savedInstanceState.getInt(CURRENT_ALARM_TIME)
            currentHour = savedInstanceState.getInt(CURRENT_TIME)
            scrollPositionOfRecycler = savedInstanceState.getInt(CURRENT_POSITION)
            notificationRecyclerView?.scrollToPosition(scrollPositionOfRecycler)
        }
    }

    private fun getNotificationFromDb() {
        repo.getListOfNotification()
            .continueWith({
                notificationList.addAll(it.result)
                notificationRecyclerView?.adapter?.notifyDataSetChanged()
            }, Task.UI_THREAD_EXECUTOR)
    }

    private fun initRecycler() {
        with(notificationRecyclerView) {
            this?.adapter = NotificationListAdapter(
                notificationList = notificationList,
                enableNotification = {
                    setAlarmManager(
                        channelMustHave = it.id,
                        date = it.date,
                        hours = it.hours,
                        minutes = it.minutes
                    )
                }, closeNotification = {
                    createAlertDialogForRemoveNotification(notification = it)
                }, changeNotification = {
                    updateAlarm(currentId = it.id, position = it.position)
                })
            this?.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun setAlarmManager(channelMustHave: Int, date: Long, hours: Int, minutes: Int) {
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
                alarmManager?.setAlarmClock(info, pendingIntent)
            }
        }
    }

    private fun createAlertDialogForRemoveNotification(notification: Notification) {
        alertDialog?.setPositiveButton(R.string.yes) { dialog, _ ->
            setCancelAlarmBtnClickListener(notification.id)
            repo.clearDbWithWereArgs(NOTIFICATION_TIME_NAME, "$ID = ${notification.id}")
                .continueWith({
                    notificationList.removeAt(notification.position)
                    notificationRecyclerView?.adapter?.notifyItemRemoved(notification.id)
                    dialog.dismiss()
                }, Task.UI_THREAD_EXECUTOR)
        }
        alertDialog?.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog?.setTitle(R.string.alarm)
        alertDialog?.setMessage(R.string.sure)
        alertDialog?.setIcon(R.drawable.ic_baseline_error_outline_24)
        alertDialog?.show()
    }

    private fun setCancelAlarmBtnClickListener(channelMustHave: Int) {
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), channelMustHave, intent, 0)
        alarmManager?.cancel(pendingIntent)
    }

    private fun updateAlarm(currentId: Int, position: Int) {
        val datePicker = createDataBicker()
        datePicker.show(childFragmentManager, DATE_PICKER)
        datePicker.addOnPositiveButtonClickListener {
            currentDate = datePicker.selection!!
            val timePicker = createTimePicker()
            timePicker.show(childFragmentManager, TIME_PICKER)
            timePicker.addOnPositiveButtonClickListener {
                setCalendarTime(timePicker = timePicker)
                updateNotificationInDb(currentId = currentId, position = position)
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

    private fun setCalendarTime(timePicker: MaterialTimePicker) {
        calendar.time = Date(currentDate)
        currentHour = timePicker.hour
        currentMinutes = timePicker.minute
        calendar[Calendar.HOUR_OF_DAY] = currentHour
        calendar[Calendar.MINUTE] = currentMinutes
    }

    private fun updateNotificationInDb(currentId: Int, position: Int) {
        if (calendar.time.time <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(requireContext(), R.string.toast_waring, Toast.LENGTH_LONG)
                .show()
        } else {
            repo.updateNotifications(currentDate, currentHour, currentMinutes, currentId)
            setAlarmManager(
                channelMustHave = currentId,
                date = currentDate,
                hours = currentHour,
                minutes = currentMinutes
            )
            notificationList[position].date = currentDate
            notificationList[position].hours = currentHour
            notificationList[position].minutes = currentMinutes
            notificationRecyclerView?.adapter?.notifyDataSetChanged()
        }
    }


    override fun onStart() {
        super.onStart()
        selectTimeBtnClickListener()
        addScrollListener()
    }

    private fun selectTimeBtnClickListener() {
        addNotificationBtn?.setOnClickListener {
            createAlarm()
        }
    }

    private fun addScrollListener() {
        notificationRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                scrollPositionOfRecycler = layoutManager.findFirstVisibleItemPosition()
            }
        })
    }

    private fun createAlarm() {
        val datePicker = createDataBicker()
        datePicker.show(childFragmentManager, DATE_PICKER)
        datePicker.addOnPositiveButtonClickListener {
            currentDate = datePicker.selection!!
            val timePicker = createTimePicker()
            timePicker.show(childFragmentManager, TIME_PICKER)
            timePicker.addOnPositiveButtonClickListener {
                createAlarmManagerForInsertIntoDb(timePicker = timePicker)
            }
        }
    }

    private fun createAlarmManagerForInsertIntoDb(
        timePicker: MaterialTimePicker
    ) {
        setCalendarTime(timePicker = timePicker)
        if (calendar.time.time <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(requireContext(), R.string.toast_waring, Toast.LENGTH_LONG)
                .show()
        } else {
            repo.insertNotification(currentDate, currentHour, currentMinutes, notificationList)
                .continueWith({
                    val id = it.result
                    createNotificationList(id = id)
                    setAlarmManager(
                        channelMustHave = id,
                        date = currentDate,
                        hours = currentHour,
                        minutes = currentMinutes
                    )
                    notificationRecyclerView?.adapter?.notifyDataSetChanged()
                }, Task.UI_THREAD_EXECUTOR)
        }
    }

    private fun createNotificationList(id: Int) {
        notificationList.add(
            Notification(
                id = id,
                date = currentDate,
                position = notificationList.size,
                hours = currentHour,
                minutes = currentMinutes
            )
        )
    }

    override fun onStop() {
        super.onStop()
        addNotificationBtn?.setOnClickListener(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(CURRENT_DATE, currentDate)
        outState.putInt(CURRENT_ALARM_TIME, currentMinutes)
        outState.putInt(CURRENT_TIME, currentHour)
        outState.putInt(CURRENT_POSITION, scrollPositionOfRecycler)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationRecyclerView = null
        addNotificationBtn = null
        alertDialog = null
        alarmManager = null
    }
}