package com.example.core.models.tracks

import com.google.gson.annotations.SerializedName

data class TrackRequest(
    @SerializedName("token")
    val token: String
)
