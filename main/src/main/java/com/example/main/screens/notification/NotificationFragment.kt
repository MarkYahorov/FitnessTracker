package com.example.main.screens.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.base.screen.base.fragment.BaseFragment
import com.example.core.models.notification.Notification
import com.example.core.provideBaseComponent
import com.example.main.R
import com.example.main.di.notifications.DaggerNotificationComponent
import com.example.main.presenter.notifications.NotificationContract
import com.example.main.screens.notification.receiver.AlarmReceiver
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

class NotificationFragment :
    BaseFragment<NotificationContract.NotificationPresenter, NotificationContract.NotificationView>(),
    NotificationContract.NotificationView {

    companion object {
        const val NEW_REQUEST_CODE = "NEW_REQUEST_CODE"
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
    private val notificationComponent by lazy {
        DaggerNotificationComponent.factory().create(provideBaseComponent(requireContext().applicationContext))
    }

    private val notificationList = mutableListOf<Notification>()
    private var calendar = Calendar.getInstance()
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
        notificationComponent.inject(this)
        return view
    }

    private fun initAll(view: View) {
        notificationRecyclerView = view.findViewById(R.id.notification_recycler)
        addNotificationBtn = view.findViewById(R.id.add_notification_btn)
        alertDialog = AlertDialog.Builder(requireContext())
        alarmManager = requireContext().getSystemService(ALARM_SERVICE) as AlarmManager
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
        getPresenter().loadData()
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
                },
                calendar = calendar
            )
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
            getPresenter().deleteNotification(notification, dialog)
            dialog.dismiss()
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

    @SuppressLint("NotifyDataSetChanged")
    private fun updateNotificationInDb(currentId: Int, position: Int) {
        if (calendar.time.time <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(requireContext(), R.string.toast_waring, Toast.LENGTH_LONG)
                .show()
        } else {
            getPresenter().updateNotification(currentDate, currentHour, currentMinutes, currentId)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun createAlarmManagerForInsertIntoDb(
        timePicker: MaterialTimePicker
    ) {
        setCalendarTime(timePicker = timePicker)
        if (calendar.time.time <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(requireContext(), R.string.toast_waring, Toast.LENGTH_LONG)
                .show()
        } else {
            getPresenter()
                .saveNotification(currentDate, currentHour, currentMinutes, notificationList)
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

    @SuppressLint("NotifyDataSetChanged")
    override fun setData(notificationList: List<Notification>) {
        this.notificationList.addAll(notificationList)
        notificationRecyclerView?.adapter?.notifyDataSetChanged()
    }

    override fun deleteNotification(notification: Notification, dialog: DialogInterface) {
        notificationList.removeAt(notification.position)
        notificationRecyclerView?.adapter?.notifyItemRemoved(notification.id)
        dialog.dismiss()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun saveNotification(id: Int) {
        createNotificationList(id = id)
        setAlarmManager(
            channelMustHave = id,
            date = currentDate,
            hours = currentHour,
            minutes = currentMinutes
        )
        notificationRecyclerView?.adapter?.notifyDataSetChanged()
    }

    override fun showLoading() {
        TODO("Not yet implemented")
    }

    override fun hideLoading() {
        TODO("Not yet implemented")
    }

    override fun createPresenter(): NotificationContract.NotificationPresenter {
        return notificationComponent.notificationPresenter()
    }
}