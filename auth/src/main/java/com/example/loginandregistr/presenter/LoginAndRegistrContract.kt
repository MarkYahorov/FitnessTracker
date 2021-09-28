package com.example.loginandregistr.presenter

import com.example.base.presenter.base.BaseContract

interface LoginAndRegistrContract {

    interface LoginAndRegistrView : BaseContract.BaseView {
        fun goToNextScreen(token: String?)
        fun showError(error: String?)
        fun setVisibilityViews(isLogin: Boolean)
        fun showAllRegistrationError()
        fun showAllLoginError()
        fun showEmailError()
        fun showPasswordError()
        fun showPasswordMatchingError()
    }

    interface LoginAndRegistrPresenter : BaseContract.BasePresenter<LoginAndRegistrView> {
        fun setLoginText(email:String)
        fun setPasswordText(password: String)
        fun setRepeatText(repeatPassword: String)
        fun setName(name: String)
        fun setLastName(lastName: String)
        fun login()
        fun registration()
    }
}