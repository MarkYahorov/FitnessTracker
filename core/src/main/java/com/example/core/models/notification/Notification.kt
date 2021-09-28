package com.example.core.models.notification

data class Notification(
    val id: Int,
    var date: Long,
    val position: Int,
    var hours: Int,
    var minutes: Int
)
