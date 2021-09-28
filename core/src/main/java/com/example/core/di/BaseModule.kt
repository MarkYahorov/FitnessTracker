package com.example.core.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.core.database.FitnessDatabase
import com.example.core.retrofit.ApiService
import com.example.core.retrofit.RetrofitBuilder
import dagger.Module
import dagger.Provides

@Module
class BaseModule {

    @Provides
    fun apiService(retrofitBuilder: RetrofitBuilder): ApiService {
        return retrofitBuilder.apiService
    }

    @Provides
    fun dataBase(context: Context): SQLiteDatabase {
        return FitnessDatabase(context).writableDatabase
    }
}