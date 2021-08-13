package com.example.fitnesstracker

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.repository.Repository
import com.example.fitnesstracker.repository.RepositoryImpl

class App : Application() {

    companion object {
        lateinit var INSTANCE: App
    }

    lateinit var myDataBase: SQLiteDatabase
    val repositoryImpl: Repository = RepositoryImpl()

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        myDataBase = FitnessDatabase(this).writableDatabase
    }
}