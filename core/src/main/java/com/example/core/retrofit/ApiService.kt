package com.example.core.retrofit

import com.example.core.models.login.LoginRequest
import com.example.core.models.login.LoginResponse
import com.example.core.models.points.PointsRequest
import com.example.core.models.points.PointsResponse
import com.example.core.models.registration.RegistrationRequest
import com.example.core.models.registration.RegistrationResponse
import com.example.core.models.save.SaveTrackRequest
import com.example.core.models.save.SaveTrackResponse
import com.example.core.models.tracks.TrackRequest
import com.example.core.models.tracks.TrackResponse
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("lesson-26.php?method=login")
    fun login(@Body loginRequest: LoginRequest): Observable<LoginResponse>

    @POST("lesson-26.php?method=register")
    fun registration(@Body registrationRequest: RegistrationRequest): Observable<RegistrationResponse>

    @POST("lesson-26.php?method=tracks")
    fun getTracks(@Body trackRequest: TrackRequest?): Single<TrackResponse>

    @POST("lesson-26.php?method=points")
    fun getPointsForCurrentTrack(@Body pointsRequest: PointsRequest?): Single<PointsResponse>

    @POST("lesson-26.php?method=save")
    fun saveTrack(@Body savePointsRequest: SaveTrackRequest?): Observable<SaveTrackResponse>
}