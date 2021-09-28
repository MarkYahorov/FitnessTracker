package com.example.base.screen.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.base.presenter.base.BaseContract

abstract class BaseActivity<Presenter: BaseContract.BasePresenter<View>, View: BaseContract.BaseView>: AppCompatActivity(){

    private var presenter: Presenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = createPresenter()
    }

    override fun onStart() {
        super.onStart()
        presenter?.attach(getMVPView())
    }

    override fun onDestroy() {
        presenter?.detach()
        presenter = null
        super.onDestroy()
    }

    open fun getMVPView(): View {
        return this as? View ?: error("noView")
    }

    protected fun getPresenter(): Presenter {
        return presenter ?: error("Presenter is not created")
    }

    abstract fun createPresenter(): Presenter
}