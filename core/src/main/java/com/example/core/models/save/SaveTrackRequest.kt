package com.example.core.models.save

import com.example.core.models.points.PointForData
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
    val pointForData: List<PointForData>
)
