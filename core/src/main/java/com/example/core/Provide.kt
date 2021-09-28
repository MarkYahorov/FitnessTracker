package com.example.core

import android.content.Context
import android.content.Intent
import com.example.core.di.BaseComponent

fun provideBaseComponent(context: Context): BaseComponent{
    return if (context is Provider){
        (context as Provider).provideComponent()
    } else {
        throw IllegalStateException("Provide the application context which implement BaseComponentProvider")
    }
}