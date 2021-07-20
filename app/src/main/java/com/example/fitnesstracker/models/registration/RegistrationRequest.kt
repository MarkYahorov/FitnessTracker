package com.example.fitnesstracker.models.registration

import com.google.gson.annotations.SerializedName

data class RegistrationRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String
)