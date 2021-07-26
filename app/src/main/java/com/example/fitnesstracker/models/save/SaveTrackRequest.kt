package com.example.fitnesstracker.models.save

import com.example.fitnesstracker.models.points.Point
import com.google.gson.annotations.SerializedName

data class SaveTrackRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("id")
    val serverId:Int?,
    @SerializedName("beginsAt")
    val beginTime: Long,
    @SerializedName("time")
    val time: Long,
    @SerializedName("distance")
    val distance: Int,
    @SerializedName("points")
    val points: List<Point>
)
