package com.example.fitnesstracker

import android.app.Application
import com.example.fitnesstracker.repository.Repository
import com.example.fitnesstracker.repository.RepositoryImpl

class App: Application() {

    companion object{
        lateinit var INSTANCE: App
    }

    val repositoryImpl: Repository = RepositoryImpl()

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }
}