package com.example.main.presenter.list

import com.example.base.presenter.base.BaseContract
import com.example.core.models.tracks.TrackFromDb

interface ListContract {

    interface ListView : BaseContract.BaseView {
        fun setData(listOfTracks: List<TrackFromDb>)
        fun showError(error: String?)
        fun endLoading()
        fun setIsFirst(): Boolean
        fun changeIsFirst()
        fun showStartDialog()
    }

    interface ListPresenter: BaseContract.BasePresenter<ListView> {
        fun setIsFirstInApp()
        fun loadTracks(token: String)
        fun getTracks(fromServer: Boolean ,token: String)
    }
}