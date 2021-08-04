package com.example.fitnesstracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.repository.Repository
import com.example.fitnesstracker.repository.RepositoryImpl
import com.example.fitnesstracker.screens.splash.SplashScreenActivity

class App: Application() {

    companion object{
        lateinit var INSTANCE: App
    }

    var whatFragment = 0
    lateinit var db: SQLiteDatabase
    val repositoryImpl: Repository = RepositoryImpl()

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        db = FitnessDatabase(this).writableDatabase
    }
}