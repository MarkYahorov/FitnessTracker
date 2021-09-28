package com.example.main.presenter.notifications

import android.content.DialogInterface
import com.example.base.presenter.base.BasePresenter
import com.example.main.data.repository.Repository
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class NotificationPresenter @Inject constructor(private val repository: Repository) :
    BasePresenter<NotificationContract.NotificationView>(),
    NotificationContract.NotificationPresenter {

    override fun loadData() {
        getCompositeDisposable().add(
        repository.getNotifications()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                getView().setData(it)
            }
        )
    }

    override fun updateNotification(
        currentDate: Long,
        currentHour: Int,
        currentMinutes: Int,
        currentId: Int) {
        getCompositeDisposable().add(
            repository.updateNotifications(currentDate, currentHour, currentMinutes, currentId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        )
    }

    override fun deleteNotification(notification: com.example.core.models.notification.Notification, dialog: DialogInterface) {
        getCompositeDisposable().add(
        repository.clearDbWithWhereArgs(com.example.core.database.FitnessDatabase.NOTIFICATION_TIME_NAME, "${com.example.core.database.FitnessDatabase.ID} = ${notification.id}")
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                getView().deleteNotification(notification, dialog)
            }
        )
    }

    override fun saveNotification(
        currentDate: Long,
        currentHour: Int,
        currentMinutes: Int,
        notificationList: List<com.example.core.models.notification.Notification>
    ) {
        getCompositeDisposable().add(
        repository.saveNotifications(currentDate, currentHour, currentMinutes, notificationList)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                getView().saveNotification(id = it)
            }
        )
    }
}