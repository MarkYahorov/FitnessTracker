package com.example.run.presenter

import android.os.Handler
import com.example.base.presenter.base.BasePresenter
import com.example.run.data.repository.Repository
import com.example.run.screen.calculate.TimeCalculator
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.*
import javax.inject.Inject

class RunningPresenter @Inject constructor(private val repository: Repository) :
    BasePresenter<RunningContract.RunningView>(),
    RunningContract.RunningPresenter {

    private var beginTime = 0L
    private var permissionsWasEnabled = false
    private val calculator = TimeCalculator()

    override fun enabledPermissions(isEnabled: Boolean) {
        permissionsWasEnabled = isEnabled
    }

    override fun bindService(beginTime: Long) {
        this.beginTime = beginTime
        getView().bind()
    }

    override fun unBindService() {
        getView().unbind()
    }

    override fun saveTrack(
        saveTrackRequest: com.example.core.models.save.SaveTrackRequest?,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        list: List<com.example.core.models.points.PointForData>
    ) {
        getCompositeDisposable().add(
            repository.saveTrack(savePointsRequest = saveTrackRequest)
                .flatMap { saveTrackResponse ->
                    Observable.fromCallable {
                        repository.saveTrackAndPoints(
                            saveTrackResponse = saveTrackResponse,
                            beginTime = beginTime,
                            calendar = calendar,
                            distance = distance,
                            listOfPoints = list
                        )
                        saveTrackResponse
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    getView().showMessage(it)
                }, {
                    getView().showMessage(it.message)
                })
        )
    }

    override fun startTimer(beginTime: Long, calendar: Calendar, handler: Handler) = object : Runnable {
        override fun run() {
            calculator.createTimer(beginTime, calendar)
            getView().setTextFromCalculator(calculator.getTimerString())
            handler.postDelayed(this, 40L)
        }
    }
}