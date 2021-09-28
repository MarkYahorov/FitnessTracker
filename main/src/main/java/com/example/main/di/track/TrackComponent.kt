package com.example.main.di.track

import com.example.core.di.BaseComponent
import com.example.main.presenter.track.TrackContract
import com.example.main.screens.track.TrackFragment
import dagger.Component

@Component(modules = [TrackModule::class], dependencies = [BaseComponent::class])
interface TrackComponent {

    @Component.Factory
    interface Factory {
        fun create(baseComponent: BaseComponent): TrackComponent
    }

    fun inject(trackFragment: TrackFragment)
    fun trackPresenter(): TrackContract.TrackPresenter
}