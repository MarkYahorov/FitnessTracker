package com.example.loginandregistr.di

import com.example.core.Provider
import com.example.core.di.BaseComponent
import com.example.core.retrofit.ApiService
import com.example.loginandregistr.presenter.LoginAndRegistrContract
import com.example.loginandregistr.screen.LoginAndRegisterActivity
import dagger.Component

@Component(modules = [LoginAndRegistrModule::class], dependencies = [BaseComponent::class])
internal interface LoginAndRegistrComponent {

    @Component.Factory
    interface Factory {
        fun create(baseComponent: BaseComponent): LoginAndRegistrComponent
    }

    fun inject(loginAndRegisterActivity: LoginAndRegisterActivity)
    fun presenter(): LoginAndRegistrContract.LoginAndRegistrPresenter
}