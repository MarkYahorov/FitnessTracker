package com.example.run.data.repository

import com.example.core.models.save.SaveTrackRequest
import com.example.core.models.save.SaveTrackResponse
import io.reactivex.Observable
import java.util.*

interface Repository {

    fun saveTrackAndPoints(
        saveTrackResponse: com.example.core.models.save.SaveTrackResponse,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        listOfPoints: List<com.example.core.models.points.PointForData>
    ): Observable<Unit>

    fun saveTrack(savePointsRequest: SaveTrackRequest?): Observable<SaveTrackResponse>
}