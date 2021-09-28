package com.example.core.di

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.core.retrofit.ApiService
import dagger.BindsInstance
import dagger.Component

@Component(modules = [BaseModule::class])
interface BaseComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): BaseComponent
    }

    fun api(): ApiService
    fun db(): SQLiteDatabase
    fun inject(application: Application)
}