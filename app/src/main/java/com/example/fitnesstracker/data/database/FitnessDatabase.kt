package com.example.fitnesstracker.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.fitnesstracker.data.database.helpers.CreateDBHelper

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
        CreateDBHelper()
            .setName("trackers")
            .addField(ID, INTEGER_NOT_NULL_PRIMARY_KEY_AUTOINCREMENT)
            .addField(ID_FROM_SERVER, INTEGER)
            .addField(BEGIN_TIME, TEXT_NOT_NULL)
            .addField(RUNNING_TIME, LONG_NOT_NULL)
            .addField(DISTANCE, INTEGER_NOT_NULL)
            .create(db = db)
    }

    private fun createPointsTable(db: SQLiteDatabase?) {
        CreateDBHelper()
            .setName("allPoints")
            .addField(ID, INTEGER_NOT_NULL_PRIMARY_KEY_AUTOINCREMENT)
            .addField(ID_FROM_SERVER, INTEGER)
            .addField(LATITUDE, REAL_NOT_NULL)
            .addField(LONGITUDE, REAL_NOT_NULL)
            .create(db = db)
    }

    private fun createNotificationDb(db: SQLiteDatabase?){
        CreateDBHelper()
            .setName("NotificationTime")
            .addField(ID, INTEGER_NOT_NULL_PRIMARY_KEY_AUTOINCREMENT)
            .addField(NOTIFICATION_TIME, LONG_NOT_NULL)
            .create(db = db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }
}