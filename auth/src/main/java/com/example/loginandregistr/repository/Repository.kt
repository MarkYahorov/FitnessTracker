package com.example.loginandregistr.repository

import com.example.core.models.login.LoginRequest
import com.example.core.models.login.LoginResponse
import com.example.core.models.registration.RegistrationRequest
import com.example.core.models.registration.RegistrationResponse
import io.reactivex.Observable

interface Repository {

    fun login(loginRequest: LoginRequest): Observable<LoginResponse>
    fun registration(registrationRequest: RegistrationRequest): Observable<RegistrationResponse>
}