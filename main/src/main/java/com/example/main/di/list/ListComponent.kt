package com.example.main.di.list

import com.example.core.di.BaseComponent
import com.example.main.presenter.list.ListContract
import com.example.main.screens.list.TrackListFragment
import dagger.Component

@Component(modules = [ListModule::class], dependencies = [BaseComponent::class])
interface ListComponent {

    @Component.Factory
    interface Factory{
        fun create(baseComponent: BaseComponent): ListComponent
    }

    fun inject(listFragment: TrackListFragment)
    fun presenter(): ListContract.ListPresenter
}