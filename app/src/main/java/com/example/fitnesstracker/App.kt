package com.example.fitnesstracker

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.retrofit.ApiService
import com.example.fitnesstracker.data.retrofit.RetrofitBuilder
import com.example.fitnesstracker.repository.serverRepo.RepositoryFromServer
import com.example.fitnesstracker.repository.dataBaseRepo.RepositoryForDB
import com.example.fitnesstracker.repository.dataBaseRepo.RepositoryForDbImpl
import com.example.fitnesstracker.repository.serverRepo.RepositoryFromServerImpl

class App : Application() {

    companion object {
        lateinit var INSTANCE: App
        const val PATTERN_WITH_SECONDS = "HH:mm:ss,SS"
        const val PATTERN_DATE_HOURS_MINUTES= "dd.MM.yyyy HH:mm"
        const val UTC = "UTC"
        const val RUNNING_ACTIVITY_MARKER = 1
        const val MAIN_ACTIVITY_MARKER = 0
    }

    lateinit var myDataBase: SQLiteDatabase
    lateinit var apiService: ApiService
    val repositoryFromServerImpl: RepositoryFromServer = RepositoryFromServerImpl()
    val repositoryForDbImpl: RepositoryForDB = RepositoryForDbImpl()

    override fun onCreate() {
        super.onCreate()

        INSTANCE = this
        myDataBase = FitnessDatabase(this).writableDatabase
        apiService = RetrofitBuilder().apiService
    }
}