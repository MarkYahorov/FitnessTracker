package com.example.main.data.repository

import android.content.Context
import com.example.core.models.notification.Notification
import com.example.core.models.points.PointForData
import com.example.core.models.points.PointsRequest
import com.example.core.models.tracks.TrackFromDb
import io.reactivex.Observable
import io.reactivex.Single

interface Repository {

    fun getTracks(isFirstTimeOrRefresh: Boolean, token: String): Observable<List<TrackFromDb>>
    fun getPointsForCurrentTrack(
        idInDb: Int,
        serverId: Int,
        pointsRequest: PointsRequest?
    ): Single<List<PointForData>>

    fun getNotifications(): Observable<List<Notification>>
    fun saveNotifications(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        listOfNotifications: List<Notification>
    ): Observable<Int>

    fun updateNotifications(updateValue: Long, hours: Int, minutes: Int, id: Int): Observable<Unit>

    fun clearDbWithWhereArgs(tableName: String, whereArgs: String): Observable<Unit>
    fun clearDb(context: Context): Observable<Unit>
}