package com.example.loginandregistr.di

import com.example.loginandregistr.presenter.LoginAndRegistrContract
import com.example.loginandregistr.presenter.LoginAndRegistrPresenter
import com.example.loginandregistr.repository.Repository
import com.example.loginandregistr.repository.RepositoryImpl
import dagger.Binds
import dagger.Module

@Module
interface LoginAndRegistrModule {

    @Binds
    fun loginPresenter(loginAndRegistrPresenter: LoginAndRegistrPresenter): LoginAndRegistrContract.LoginAndRegistrPresenter

    @Binds
    fun repository(repositoryFromServerImpl: RepositoryImpl): Repository
}