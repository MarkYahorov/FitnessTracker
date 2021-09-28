package com.example.main.di.notifications

import com.example.main.data.dataSource.DbSource
import com.example.main.data.dataSource.DbSourceImpl
import com.example.main.presenter.notifications.NotificationContract
import com.example.main.presenter.notifications.NotificationPresenter
import com.example.main.data.repository.Repository
import com.example.main.data.repository.RepositoryImpl
import dagger.Binds
import dagger.Module

@Module
interface NotificationModule {

    @Binds
    fun notificationPresenter(notificationPresenter: NotificationPresenter): NotificationContract.NotificationPresenter

    @Binds
    fun repository(repositoryImpl: RepositoryImpl): Repository

    @Binds
    fun dbSource(dbSourceImpl: DbSourceImpl): DbSource
}