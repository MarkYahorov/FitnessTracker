package com.example.main.presenter.notifications

import android.content.DialogInterface
import com.example.base.presenter.base.BaseContract
import com.example.core.models.notification.Notification

interface NotificationContract {

    interface NotificationView: BaseContract.BaseView {
        fun setData(notificationList: List<com.example.core.models.notification.Notification>)
        fun deleteNotification(notification: com.example.core.models.notification.Notification, dialog: DialogInterface)
        fun saveNotification(id: Int)
    }

    interface NotificationPresenter: BaseContract.BasePresenter<NotificationView> {
        fun loadData()
        fun updateNotification(currentDate: Long, currentHour: Int, currentMinutes: Int, currentId: Int)
        fun deleteNotification(notification: com.example.core.models.notification.Notification, dialog: DialogInterface)
        fun saveNotification(currentDate: Long, currentHour: Int, currentMinutes: Int, notificationList: List<com.example.core.models.notification.Notification>)
    }
}