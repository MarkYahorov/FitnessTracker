package com.example.fitnesstracker.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.fitnesstracker.data.database.helpers.CreateTableHelper

const val FITNESS_DB = "FITNESS.db"
const val VERSION_DB = 1

class FitnessDatabase(context: Context) : SQLiteOpenHelper(context, FITNESS_DB, null, VERSION_DB) {

    companion object {
        const val ID = "id"
        const val ID_FROM_SERVER = "_id"
        const val BEGIN_TIME = "beginAt"
        const val RUNNING_TIME = "time"
        const val NOTIFICATION_TIME = "time"
        const val DISTANCE = "distance"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val CURRENT_TRACK = "currentTrack"
        const val IS_SEND = "isSend"
        const val POSITION_IN_LIST = "position"
        const val CURRENT_HOUR = "hour"
        const val CURRENT_MINUTE = "minute"
        const val TRACKERS = "trackers"
        const val ALL_POINTS = "allPoints"
        const val NOTIFICATION_TIME_NAME = "NotificationTime"
        private const val INTEGER_NOT_NULL_PRIMARY_KEY_AUTOINCREMENT =
            "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
        private const val INTEGER = "INTEGER"
        private const val INTEGER_NOT_NULL = "INTEGER NOT NULL"
        private const val LONG_NOT_NULL = "LONG NOT NULL"
        private const val TEXT_NOT_NULL = "TEXT NOT NULL"
        private const val REAL_NOT_NULL = "REAL NOT NULL"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        createTrackersTable(db = db)
        createPointsTable(db = db)
        createNotificationDb(db = db)
    }

    private fun createTrackersTable(db: SQLiteDatabase?) {
        CreateTableHelper()
            .setName(table = TRACKERS)
            .addField(title = ID, condition = INTEGER_NOT_NULL_PRIMARY_KEY_AUTOINCREMENT)
            .addField(title = ID_FROM_SERVER, condition = INTEGER)
            .addField(title = BEGIN_TIME, condition = TEXT_NOT_NULL)
            .addField(title = RUNNING_TIME, condition = LONG_NOT_NULL)
            .addField(title = DISTANCE, condition = INTEGER_NOT_NULL)
            .addField(title = IS_SEND, condition = INTEGER_NOT_NULL) // 1 or 0, 1- false, 0 true
            .create(db = db)
    }

    private fun createPointsTable(db: SQLiteDatabase?) {
        CreateTableHelper()
            .setName(table = ALL_POINTS)
            .addField(title = ID, condition = INTEGER_NOT_NULL_PRIMARY_KEY_AUTOINCREMENT)
            .addField(title = ID_FROM_SERVER, condition = INTEGER)
            .addField(title = CURRENT_TRACK, condition = INTEGER_NOT_NULL)
            .addField(title = LATITUDE, condition = REAL_NOT_NULL)
            .addField(title = LONGITUDE, condition = REAL_NOT_NULL)
            .create(db = db)
    }

    private fun createNotificationDb(db: SQLiteDatabase?) {
        CreateTableHelper()
            .setName(table = NOTIFICATION_TIME_NAME)
            .addField(title = ID, condition = INTEGER_NOT_NULL_PRIMARY_KEY_AUTOINCREMENT)
            .addField(title = NOTIFICATION_TIME, condition = LONG_NOT_NULL)
            .addField(title = POSITION_IN_LIST, condition = INTEGER_NOT_NULL)
            .addField(title = CURRENT_HOUR, condition = INTEGER_NOT_NULL)
            .addField(title = CURRENT_MINUTE, condition = INTEGER_NOT_NULL)
            .create(db = db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}