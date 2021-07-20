package com.example.fitnesstracker.repository

import bolts.Task
import com.example.fitnesstracker.data.retrofit.RetrofitBuilder
import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse

class RepositoryImpl : Repository {
    override fun login(loginRequest: LoginRequest): Task<LoginResponse> {
        return Task.callInBackground {
            val execute = RetrofitBuilder().apiService.login(loginRequest = loginRequest)
            execute.execute().body()
        }
    }

    override fun registration(registrationRequest: RegistrationRequest): Task<RegistrationResponse> {
        return Task.callInBackground {
            val execute =
                RetrofitBuilder().apiService.registration(registrationRequest = registrationRequest)
            execute.execute().body()
        }
    }
}