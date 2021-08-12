package com.example.fitnesstracker.repository

import android.content.Context
import bolts.Task
import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.notification.Notification
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.points.PointsRequest
import com.example.fitnesstracker.models.points.PointsResponse
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse
import com.example.fitnesstracker.models.save.SaveTrackRequest
import com.example.fitnesstracker.models.save.SaveTrackResponse
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.TrackResponse
import com.example.fitnesstracker.models.tracks.Tracks
import java.util.*

interface Repository {
    fun login(loginRequest: LoginRequest): Task<LoginResponse>
    fun registration(registrationRequest: RegistrationRequest): Task<RegistrationResponse>
    fun getTracks(trackRequest: TrackRequest): Task<TrackResponse>
    fun getPointsForCurrentTrack(
        idInDb: Int,
        serverId: Int,
        pointsRequest: PointsRequest
    ): Task<List<PointForData>>

    fun saveTrack(saveTrackRequest: SaveTrackRequest): Task<SaveTrackResponse>
    fun getListOfTrack(): Task<List<Tracks>>
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