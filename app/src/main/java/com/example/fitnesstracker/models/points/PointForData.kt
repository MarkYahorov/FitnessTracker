package com.example.fitnesstracker.models.points

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PointForData(
    @SerializedName("lng")
    val lng: Double,
    @SerializedName("lat")
    val lat: Double
) : Parcelable
