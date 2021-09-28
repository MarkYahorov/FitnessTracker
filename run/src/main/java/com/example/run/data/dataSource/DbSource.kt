package com.example.run.data.dataSource

import com.example.core.models.points.PointForData
import com.example.core.models.save.SaveTrackResponse
import java.util.*

interface DbSource {

    fun saveTrackAndPointsFromSavingInServer(
        saveTrackResponse: SaveTrackResponse,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        listOfPoints: List<PointForData>
    )
}