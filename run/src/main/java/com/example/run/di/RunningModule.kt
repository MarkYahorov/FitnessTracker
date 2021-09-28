package com.example.run.di

import com.example.run.data.dataSource.DbSource
import com.example.run.data.dataSource.DbSourceImpl
import com.example.run.presenter.RunningContract
import com.example.run.presenter.RunningPresenter
import com.example.run.data.repository.Repository
import com.example.run.data.repository.RepositoryImpl
import dagger.Binds
import dagger.Module

@Module
interface RunningModule {

    @Binds
    fun presenter(runningPresenter: RunningPresenter): RunningContract.RunningPresenter

    @Binds
    fun repository(repositoryImpl: RepositoryImpl): Repository

    @Binds
    fun dbSource(dbSourceImpl: DbSourceImpl):DbSource
}