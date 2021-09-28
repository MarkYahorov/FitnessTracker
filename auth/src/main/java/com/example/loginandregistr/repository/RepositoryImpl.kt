package com.example.loginandregistr.repository

import com.example.core.Provider
import com.example.core.models.login.LoginRequest
import com.example.core.models.login.LoginResponse
import com.example.core.models.registration.RegistrationRequest
import com.example.core.models.registration.RegistrationResponse
import com.example.core.retrofit.ApiService
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class RepositoryImpl @Inject constructor(private val apiService: ApiService) : Repository {

    override fun login(loginRequest: LoginRequest): Observable<LoginResponse> {
        return apiService.login(loginRequest)
            .subscribeOn(Schedulers.io())
    }

    override fun registration(registrationRequest: RegistrationRequest): Observable<RegistrationResponse> {
        return apiService.registration(registrationRequest)
            .subscribeOn(Schedulers.io())
    }
}