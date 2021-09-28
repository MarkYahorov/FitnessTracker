package com.example.main.data.dataSource

import android.content.Context
import com.example.core.models.notification.Notification
import com.example.core.models.points.PointForData
import com.example.core.models.tracks.TrackForData
import com.example.core.models.tracks.TrackFromDb

interface DbSource {

    fun getTracks(): List<TrackFromDb>
    fun getListNotSendTracks(): List<TrackFromDb>
    fun getPointsForCurrentTrack(trackId: Int): List<PointForData>

    fun saveNotifications(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        listOfNotifications: List<Notification>
    ): Int

    fun getListOfNotifications(): List<Notification>
    fun updateNotifications(
        updateValue: Long,
        hours: Int,
        minutes: Int,
        id: Int
    )
    fun saveTracks(tracksInfo: List<TrackForData>)

    fun clearDbWithWhereArgs(tableName: String, whereArgs: String)
    fun clearDb(context: Context)
    fun updateField(
        tableName: String,
        fieldName: String,
        value: Int,
        whereArgs: String
    )

    fun savePoints(trackIdInDb: Int, listOfPoints: List<PointForData>)
    fun checkThisPointIntoDb(currentTrackId: Int): Boolean
}