package com.example.fitnesstracker

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.retrofit.RetrofitBuilder
import com.example.fitnesstracker.repository.Repository
import com.example.fitnesstracker.repository.RepositoryImpl

class App : Application() {

    companion object {
        lateinit var INSTANCE: App
        const val PATTERN_WITH_SECONDS = "HH:mm:ss,SS"
        const val PATTERN_WITHOUT_SECONDS = "dd.MM.yyyy HH:mm"
        const val UTC = "UTC"
        const val RUNNING_ACTIVITY_MARKER = 1
        const val MAIN_ACTIVITY_MARKER = 0
    }

    lateinit var myDataBase: SQLiteDatabase
    val repositoryImpl: Repository = RepositoryImpl()
    val apiService = RetrofitBuilder().apiService

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        myDataBase = FitnessDatabase(this).writableDatabase
    }
}