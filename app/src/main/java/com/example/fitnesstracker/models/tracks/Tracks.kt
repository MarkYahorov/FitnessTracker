package com.example.fitnesstracker.models.tracks

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tracks (
    val id: Int?,
    var serverId: Int,
    val beginTime: Long,
    val time: Long,
    val distance: Int
): Parcelable