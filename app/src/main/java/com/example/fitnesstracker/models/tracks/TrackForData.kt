package com.example.fitnesstracker.models.tracks

import com.google.gson.annotations.SerializedName
import java.util.*

data class TrackForData(
    @SerializedName("id")
    var serverId: Int?,
    @SerializedName("beginsAt")
    var beginTime: Long,
    @SerializedName("time")
    var time: Long,
    @SerializedName("distance")
    var distance: Int
)