package com.example.fitnesstracker.repository

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
import com.example.fitnesstracker.models.tracks.TrackForData
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.TrackResponse
import com.example.fitnesstracker.models.tracks.Tracks

interface Repository {
    fun login(loginRequest: LoginRequest): Task<LoginResponse>
    fun registration(registrationRequest: RegistrationRequest) :Task<RegistrationResponse>
    fun getTracks(trackRequest: TrackRequest): Task<TrackResponse>
    fun getPointsForCurrentTrack(pointsRequest: PointsRequest): Task<PointsResponse>
    fun saveTrack(saveTrackRequest: SaveTrackRequest): Task<SaveTrackResponse>
    fun getListOfTrack():Task<List<Tracks>>
    fun getListOfNotification(): Task<List<Notification>>
    fun getPointsForCurrentTrackFromDb(id:Int): Task<List<PointForData>>
}