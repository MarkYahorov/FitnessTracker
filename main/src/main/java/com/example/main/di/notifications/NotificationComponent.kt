package com.example.main.di.notifications

import com.example.core.di.BaseComponent
import com.example.main.presenter.notifications.NotificationContract
import com.example.main.screens.notification.NotificationFragment
import dagger.Component

@Component(modules = [NotificationModule::class], dependencies = [BaseComponent::class])
interface NotificationComponent {

    @Component.Factory
    interface Factory {
        fun create(baseComponent: BaseComponent): NotificationComponent
    }

    fun inject(notificationFragment: NotificationFragment)
    fun notificationPresenter(): NotificationContract.NotificationPresenter
}