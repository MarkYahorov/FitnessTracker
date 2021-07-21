package com.example.fitnesstracker.data.retrofit

import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.TrackResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("lesson-26.php?method=login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST("lesson-26.php?method=register")
    fun registration(@Body registrationRequest: RegistrationRequest): Call<RegistrationResponse>

    @POST("lesson-26.php?method=tracks")
    fun getTracks(@Body trackRequest: TrackRequest): Call<TrackResponse>
}