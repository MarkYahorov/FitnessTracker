package com.example.run.di

import com.example.core.di.BaseComponent
import com.example.run.presenter.RunningContract
import com.example.run.screen.RunningActivity
import dagger.Component

@Component(modules = [RunningModule::class], dependencies = [BaseComponent::class])
interface RunningComponent {

    @Component.Factory
    interface Factory {
        fun create(baseComponent: BaseComponent): RunningComponent
    }

    fun inject(runningActivity: RunningActivity)
    fun presenter(): RunningContract.RunningPresenter
}