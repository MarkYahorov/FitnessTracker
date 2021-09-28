package com.example.main.presenter.track

import com.example.base.presenter.base.BaseContract
import com.example.core.models.points.PointForData
import com.example.core.models.points.PointsRequest

interface TrackContract {

    interface TrackView : BaseContract.BaseView {
        fun setData(listOfPoints: List<com.example.core.models.points.PointForData>)
        fun showError(error: String?)
    }

    interface TrackPresenter: BaseContract.BasePresenter<TrackView> {
        fun loadPoints(pointsRequest: com.example.core.models.points.PointsRequest?, idInDb: Int, serverId: Int)
    }
}