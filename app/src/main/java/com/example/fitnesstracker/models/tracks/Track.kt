package com.example.fitnesstracker.models.tracks

import com.google.gson.annotations.SerializedName
import java.util.*

data class Track(
    @SerializedName("id")
    val id: Int,
    @SerializedName("beginsAt")
    val beginTime: Date,
    @SerializedName("time")
    val time: Long,
    @SerializedName("distance")
    val distance: Int
)