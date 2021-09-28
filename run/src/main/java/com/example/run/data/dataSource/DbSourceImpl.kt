package com.example.run.data.dataSource

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.core.database.FitnessDatabase
import com.example.core.database.FitnessDatabase.Companion.ALL_POINTS
import com.example.core.database.FitnessDatabase.Companion.BEGIN_TIME
import com.example.core.database.FitnessDatabase.Companion.CURRENT_TRACK
import com.example.core.database.FitnessDatabase.Companion.DISTANCE
import com.example.core.database.FitnessDatabase.Companion.ID
import com.example.core.database.FitnessDatabase.Companion.ID_FROM_SERVER
import com.example.core.database.FitnessDatabase.Companion.IS_SEND
import com.example.core.database.FitnessDatabase.Companion.LATITUDE
import com.example.core.database.FitnessDatabase.Companion.LONGITUDE
import com.example.core.database.FitnessDatabase.Companion.RUNNING_TIME
import com.example.core.database.FitnessDatabase.Companion.TRACKERS
import com.example.core.database.helpers.InsertIntoDBHelper
import com.example.core.database.helpers.SelectFromDbHelper
import com.example.core.models.points.PointForData
import com.example.core.models.save.SaveTrackResponse
import com.example.core.models.tracks.TrackForData
import com.example.run.screen.RunningActivity.Companion.ERROR
import java.util.*
import javax.inject.Inject

class DbSourceImpl @Inject constructor(private val db:SQLiteDatabase): DbSource {

    companion object {
        private const val NOT_SEND = 1
        private const val WAS_SEND = 0
        private const val MAX = "max($ID) as $ID"
        private const val ERROR = "error"
    }

    override fun saveTrackAndPointsFromSavingInServer(
        saveTrackResponse: SaveTrackResponse,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        listOfPoints: List<PointForData>
    ) {
        if (saveTrackResponse.status == ERROR) {
            insertDataAfterRunning(
                id = null,
                beginTime = beginTime,
                calendar = calendar,
                distance = distance,
                listOfPoints = listOfPoints,
                isSend = NOT_SEND
            )
        } else {
            insertDataAfterRunning(
                id = saveTrackResponse.serverId,
                beginTime = beginTime,
                calendar = calendar,
                distance = distance,
                listOfPoints = listOfPoints,
                isSend = WAS_SEND
            )
        }
    }

    private fun insertDataAfterRunning(
        id: Int?,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        listOfPoints: List<PointForData>,
        isSend: Int
    ) {
        insertTheTrack(
            id = id,
            isSend = isSend,
            beginTime = beginTime,
            calendar = calendar,
            distance = distance
        )
        val trackIdInDb = getLastPositionInDb()
        insertThePoints(
            trackIdInDb = trackIdInDb!!,
            listOfPoints = listOfPoints
        )
    }

    private fun getLastPositionInDb(): Int? {
        var cursor: Cursor? = null
        var id: Int? = null
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = TRACKERS)
                .selectParams(allParams = MAX)
                .select(db = db)
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(ID)
                do {
                    id = cursor.getInt(idIndex)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return id
    }

    private fun insertTheTrack(
        id: Int?,
        isSend: Int,
        beginTime: Long,
        calendar: Calendar,
        distance: Int
    ) {
        InsertIntoDBHelper()
            .setTableName(name = TRACKERS)
            .addFieldsAndValuesToInsert(
                nameOfField = ID_FROM_SERVER,
                insertingValue = id.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = BEGIN_TIME,
                insertingValue = beginTime.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = RUNNING_TIME,
                insertingValue = calendar.time.time.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = IS_SEND,
                insertingValue = isSend.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = DISTANCE,
                insertingValue = distance.toString()
            )
            .insertTheValues(db = db)
    }

    private fun insertThePoints(
        trackIdInDb: Int,
        listOfPoints: List<PointForData>
    ) {
        listOfPoints.forEach {
            InsertIntoDBHelper()
                .setTableName(name = ALL_POINTS)
                .addFieldsAndValuesToInsert(
                    nameOfField = LATITUDE,
                    insertingValue = it.lat.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = CURRENT_TRACK,
                    insertingValue = trackIdInDb.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = LONGITUDE,
                    insertingValue = it.lng.toString()
                )
                .insertTheValues(db = db)
        }
    }
}