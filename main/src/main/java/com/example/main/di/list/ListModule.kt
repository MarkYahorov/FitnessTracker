package com.example.main.di.list

import com.example.main.data.dataSource.DbSource
import com.example.main.data.dataSource.DbSourceImpl
import com.example.main.presenter.list.ListContract
import com.example.main.presenter.list.ListPresenter
import com.example.main.data.repository.Repository
import com.example.main.data.repository.RepositoryImpl
import dagger.Binds
import dagger.Module

@Module
interface ListModule {

    @Binds
    fun listPresenter(listPresenter: ListPresenter): ListContract.ListPresenter

    @Binds
    fun repository(repositoryFromServerImpl: RepositoryImpl): Repository

    @Binds
    fun dbSource(dbSourceImpl: DbSourceImpl): DbSource
}