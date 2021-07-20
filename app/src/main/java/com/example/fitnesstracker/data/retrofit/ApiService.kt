package com.example.fitnesstracker.data.retrofit

import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("lesson-26.php?method=login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST
    fun registration(@Body registrationRequest: RegistrationRequest): Call<RegistrationResponse>
}