package com.example.fitnesstracker.models.points

import com.google.gson.annotations.SerializedName

data class PointsResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("points")
    val pointForData: List<PointForData>,
    @SerializedName("code")
    val error: String?
)
