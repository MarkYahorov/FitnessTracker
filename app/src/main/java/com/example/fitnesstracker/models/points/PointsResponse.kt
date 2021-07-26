package com.example.fitnesstracker.models.points

import com.google.gson.annotations.SerializedName

data class PointsResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("points")
    val points: List<Point>
)
