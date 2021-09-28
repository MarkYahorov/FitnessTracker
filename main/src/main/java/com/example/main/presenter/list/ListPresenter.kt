package com.example.main.presenter.list

import com.example.base.presenter.base.BasePresenter
import com.example.main.data.repository.Repository
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class ListPresenter @Inject constructor(private val repository: Repository) :
    BasePresenter<ListContract.ListView>(), ListContract.ListPresenter {

    private var isLoading = false
    private var isFirstInApp = true

    override fun setIsFirstInApp() {
        isFirstInApp = getView().setIsFirst()
    }

    override fun loadTracks(token: String) {
        if (isFirstInApp) {
            getView().changeIsFirst()
            getView().showStartDialog()
            getTracks(true, token)
        } else {
            getTracks(false, token)
        }
    }

    override fun getTracks(fromServer: Boolean ,token: String){
        if (!isLoading) {
            isLoading = true
            getCompositeDisposable().add(
                repository.getTracks(fromServer, token)
                    .map { response ->
                        response.sortedByDescending { track -> track.beginTime }
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        getView().setData(listOfTracks = response)
                    }, {
                        getView().showError(it.message)
                    }, {
                        getView().endLoading()
                        isLoading = false
                    })
            )
        } else {
            getView().endLoading()
        }
    }
}