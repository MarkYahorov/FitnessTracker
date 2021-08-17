package com.example.fitnesstracker.repository

import android.content.Context
import bolts.Task
import com.example.fitnesstracker.models.notification.Notification
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.save.SaveTrackResponse
import com.example.fitnesstracker.models.tracks.TrackFromDb
import java.util.*

interface RepositoryForDB {
    fun getListOfTrack(): Task<List<TrackFromDb>>
    fun getListOfNotification(): Task<List<Notification>>
    fun insertNotification(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        listOfNotifications: List<Notification>
    ): Task<Int>

    fun updateNotifications(updateValue: Long, hours: Int, minutes: Int, id: Int): Task<Unit>
    fun clearDb(context: Context): Task<Unit>
    fun clearDbWithWereArgs(tableName: String, whereArgs: String): Task<Unit>
    fun insertTrackAndPointsInDbAfterSavingInServer(
        saveTrackResponse: Task<SaveTrackResponse>,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        listOfPoints: List<PointForData>
    ): Task<Unit>
}