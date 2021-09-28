package com.example.core

import com.example.core.di.BaseComponent


interface Provider {

    fun provideComponent(): BaseComponent
}