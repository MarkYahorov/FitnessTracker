package com.example.core.models.columnIndex

data class TrackColumnIndexFromDb(
    val id: Int,
    val serverId: Int,
    val beginAt: Int,
    val time: Int,
    val distance: Int
)