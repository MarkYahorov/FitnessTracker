package com.example.run.presenter

import android.os.Handler
import com.example.base.presenter.base.BaseContract
import com.example.core.models.points.PointForData
import com.example.core.models.save.SaveTrackRequest
import com.example.core.models.save.SaveTrackResponse
import java.util.*

interface RunningContract {

    interface RunningView: BaseContract.BaseView {
        fun showMessage(message: String?)
        fun showMessage(message: com.example.core.models.save.SaveTrackResponse)
        fun bind()
        fun unbind()
        fun setTextFromCalculator(text: String)
    }

    interface RunningPresenter: BaseContract.BasePresenter<RunningView>{
        fun startTimer(beginTime: Long, calendar: Calendar, handler: Handler): Runnable
        fun enabledPermissions(isEnabled: Boolean)
        fun bindService(beginTime: Long)
        fun unBindService()
        fun saveTrack(saveTrackRequest: com.example.core.models.save.SaveTrackRequest?,
                      beginTime: Long,
                      calendar: Calendar,
                      distance: Int,
                      list: List<com.example.core.models.points.PointForData>)
    }
}