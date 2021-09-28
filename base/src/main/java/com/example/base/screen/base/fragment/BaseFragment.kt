package com.example.base.screen.base.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.base.presenter.base.BaseContract

abstract class BaseFragment<Presenter: BaseContract.BasePresenter<View>, View: BaseContract.BaseView>: Fragment() {

    private var presenter: Presenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = createPresenter()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter?.attach(getMvpView())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.detach()
    }

    override fun onDestroy() {
        this.presenter = null
        super.onDestroy()
    }

    open fun getMvpView(): View{
        return this as? View ?: error("Cannot cast to view interface!")
    }

    protected fun getPresenter(): Presenter {
        return presenter ?: error("Presenter is not created")
    }


    abstract fun createPresenter(): Presenter
}