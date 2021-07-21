package com.example.fitnesstracker.models.tracks

import com.google.gson.annotations.SerializedName

data class TrackResponse (
    @SerializedName("status")
    val status: String,
    @SerializedName("tracks")
    val tracks: List<Track>
)