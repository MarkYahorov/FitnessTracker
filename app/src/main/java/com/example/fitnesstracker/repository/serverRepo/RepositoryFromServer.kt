package com.example.fitnesstracker.repository.serverRepo

import bolts.Task
import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.points.PointsRequest
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse
import com.example.fitnesstracker.models.save.SaveTrackRequest
import com.example.fitnesstracker.models.save.SaveTrackResponse
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.TrackResponse

interface RepositoryFromServer {
    fun login(loginRequest: LoginRequest): Task<LoginResponse>
    fun registration(registrationRequest: RegistrationRequest): Task<RegistrationResponse>
    fun getTracks(trackRequest: TrackRequest?): Task<TrackResponse>
    fun getPointsForCurrentTrack(
        idInDb: Int,
        serverId: Int,
        pointsRequest: PointsRequest?
    ): Task<List<PointForData>>

    fun saveTrack(saveTrackRequest: SaveTrackRequest?): Task<SaveTrackResponse>
}