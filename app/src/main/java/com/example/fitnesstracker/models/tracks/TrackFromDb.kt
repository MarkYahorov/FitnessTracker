package com.example.fitnesstracker.models.tracks

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrackFromDb(
    var id: Int,
    var serverId: Int?,
    var beginTime: Long,
    var time: Long,
    var distance: Int
) : Parcelable