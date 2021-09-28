package com.example.base.presenter.base

interface BaseContract {

    interface BaseView {
        fun showLoading()
        fun hideLoading()
    }

    interface BasePresenter<View : BaseView> {
        fun attach(view: View)
        fun detach()
    }
}