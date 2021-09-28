package com.example.main.presenter.track

import com.example.base.presenter.base.BasePresenter
import com.example.core.models.points.PointsRequest
import com.example.main.data.repository.Repository
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class TrackPresenter @Inject constructor(private val repository: Repository) :
    BasePresenter<TrackContract.TrackView>(), TrackContract.TrackPresenter {

    override fun loadPoints(pointsRequest: PointsRequest?, idInDb: Int, serverId: Int) {
        getCompositeDisposable().add(
            repository.getPointsForCurrentTrack(
                pointsRequest = pointsRequest,
                idInDb = idInDb,
                serverId = serverId
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    getView().setData(list)
                }, { error ->
                    getView().showError(error.message)
                })
        )
    }
}