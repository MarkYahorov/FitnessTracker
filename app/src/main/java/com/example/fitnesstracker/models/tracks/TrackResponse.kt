package com.example.fitnesstracker.models.tracks

import com.google.gson.annotations.SerializedName

data class TrackResponse (
    @SerializedName("status")
    val status: String,
    @SerializedName("tracks")
    val trackForData: List<TrackForData>,
    @SerializedName("code")
    val error: String?
)