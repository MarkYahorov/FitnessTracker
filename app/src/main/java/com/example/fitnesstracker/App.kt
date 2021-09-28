package com.example.fitnesstracker

import android.app.Application
import android.content.Context
import android.content.Intent
import com.example.core.Provider
import com.example.core.di.BaseComponent
import com.example.core.di.DaggerBaseComponent
import com.example.run.screen.RunningActivity

class App : Application(), Provider{

    private val appComponent: BaseComponent by lazy {
        DaggerBaseComponent.factory()
            .create(this)
    }

    override fun onCreate() {
        super.onCreate()
        appComponent.inject(this)
    }

    override fun provideComponent(): BaseComponent = appComponent
}