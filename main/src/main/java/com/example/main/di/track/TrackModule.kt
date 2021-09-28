package com.example.main.di.track

import com.example.main.data.dataSource.DbSource
import com.example.main.data.dataSource.DbSourceImpl
import com.example.main.presenter.track.TrackContract
import com.example.main.presenter.track.TrackPresenter
import com.example.main.data.repository.Repository
import com.example.main.data.repository.RepositoryImpl
import dagger.Binds
import dagger.Module

@Module
interface TrackModule {

    @Binds
    fun presenter(trackPresenter: TrackPresenter): TrackContract.TrackPresenter

    @Binds
    fun repository(repositoryImpl: RepositoryImpl): Repository

    @Binds
    fun dbSource(dbSourceImpl: DbSourceImpl): DbSource
}