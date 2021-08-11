package com.example.fitnesstracker.models.columnIndex

data class NotificationColumnIndexFromDb(
    val idIndexFromDb: Int,
    var dateIndexFromDb: Int,
    val positionIndexFromDb: Int,
    var hoursIndexFromDb: Int,
    var minutesIndexFromDb:Int
)
