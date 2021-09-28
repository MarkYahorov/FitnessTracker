package com.example.run.data.repository

import com.example.core.models.points.PointForData
import com.example.core.models.save.SaveTrackRequest
import com.example.core.models.save.SaveTrackResponse
import com.example.core.retrofit.ApiService
import com.example.run.data.dataSource.DbSource
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

class RepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dbSource: DbSource
) : Repository {

    override fun saveTrackAndPoints(
        saveTrackResponse: SaveTrackResponse,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        listOfPoints: List<PointForData>
    ): Observable<Unit> {
        return Observable.fromCallable {
            dbSource.saveTrackAndPointsFromSavingInServer(
                saveTrackResponse,
                beginTime,
                calendar,
                distance,
                listOfPoints
            )
        }.subscribeOn(Schedulers.io())
    }

    override fun saveTrack(savePointsRequest: SaveTrackRequest?): Observable<SaveTrackResponse> {
        return apiService.saveTrack(savePointsRequest)
            .subscribeOn(Schedulers.io())
    }

}