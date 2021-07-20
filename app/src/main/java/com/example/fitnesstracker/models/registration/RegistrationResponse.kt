package com.example.fitnesstracker.models.registration

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RegistrationResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("token")
    val token: String
): Parcelable
