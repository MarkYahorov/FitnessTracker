package com.example.base.presenter.base

import io.reactivex.disposables.CompositeDisposable

abstract class BasePresenter<View : BaseContract.BaseView> : BaseContract.BasePresenter<View> {

    private var view: View? = null
    private var dispose: CompositeDisposable = CompositeDisposable()

    override fun attach(view: View) {
        this.view = view
        if (dispose.isDisposed) {
            dispose = CompositeDisposable()
        }
    }

    override fun detach() {
        dispose.dispose()
        view = null
    }

    protected fun getView(): View {
        return view ?: error("View is not attached")
    }

    protected fun getCompositeDisposable(): CompositeDisposable {
        return dispose
    }
}