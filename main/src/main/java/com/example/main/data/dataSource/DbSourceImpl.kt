package com.example.main.data.dataSource

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.core.Constants
import com.example.core.Constants.CURRENT_TOKEN
import com.example.core.Constants.FITNESS_SHARED
import com.example.core.Constants.IS_FIRST
import com.example.core.database.FitnessDatabase
import com.example.core.database.FitnessDatabase.Companion.ALL_POINTS
import com.example.core.database.FitnessDatabase.Companion.BEGIN_TIME
import com.example.core.database.FitnessDatabase.Companion.CURRENT_HOUR
import com.example.core.database.FitnessDatabase.Companion.CURRENT_MINUTE
import com.example.core.database.FitnessDatabase.Companion.CURRENT_TRACK
import com.example.core.database.FitnessDatabase.Companion.DISTANCE
import com.example.core.database.FitnessDatabase.Companion.ID
import com.example.core.database.FitnessDatabase.Companion.ID_FROM_SERVER
import com.example.core.database.FitnessDatabase.Companion.IS_SEND
import com.example.core.database.FitnessDatabase.Companion.LATITUDE
import com.example.core.database.FitnessDatabase.Companion.LONGITUDE
import com.example.core.database.FitnessDatabase.Companion.NOTIFICATION_TIME
import com.example.core.database.FitnessDatabase.Companion.NOTIFICATION_TIME_NAME
import com.example.core.database.FitnessDatabase.Companion.POSITION_IN_LIST
import com.example.core.database.FitnessDatabase.Companion.RUNNING_TIME
import com.example.core.database.FitnessDatabase.Companion.TRACKERS
import com.example.core.database.helpers.InsertIntoDBHelper
import com.example.core.database.helpers.SelectFromDbHelper
import com.example.core.database.helpers.UpdateIntoDbHelper
import com.example.core.models.columnIndex.NotificationColumnIndexFromDb
import com.example.core.models.columnIndex.TrackColumnIndexFromDb
import com.example.core.models.notification.Notification
import com.example.core.models.points.PointForData
import com.example.core.models.tracks.TrackForData
import com.example.core.models.tracks.TrackFromDb
import com.example.core.retrofit.ApiService
import javax.inject.Inject

class DbSourceImpl @Inject constructor(
    private val db: SQLiteDatabase
) : DbSource {

    companion object {
        private const val SELECT_ALL = "*"
        private const val NOT_SEND = 1
        private const val WAS_SEND = 0
        private const val MAX = "max($ID) as $ID"
        private const val ERROR = "error"
    }

    override fun getTracks(): List<TrackFromDb> {
        return getListOfTracks()
    }

    override fun getListNotSendTracks(): List<TrackFromDb> {
        val listOfTracks = mutableListOf<TrackFromDb>()
        if (checkDataInDb()) {
            var cursor: Cursor? = null
            try {
                cursor = SelectFromDbHelper()
                    .nameOfTable(table = TRACKERS)
                    .selectParams(allParams = SELECT_ALL)
                    .where(whereArgs = "$IS_SEND = $NOT_SEND")
                    .select(db = db)
                if (cursor.moveToFirst()) {
                    val index = getColumnIndex(cursor = cursor)
                    do {
                        val track = createTrack(cursor = cursor, index = index)
                        listOfTracks.add(track)
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor?.close()
            }
        }
        return listOfTracks
    }

    override fun getPointsForCurrentTrack(trackId: Int): List<PointForData> {
        val listOfTracks = mutableListOf<PointForData>()
        var cursor: Cursor? = null
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = ALL_POINTS)
                .selectParams(allParams = SELECT_ALL)
                .where(whereArgs = "$CURRENT_TRACK = $trackId")
                .select(db = db)
            if (cursor.moveToFirst()) {
                val latIndex = cursor.getColumnIndexOrThrow(LATITUDE)
                val lonIndex = cursor.getColumnIndexOrThrow(LONGITUDE)
                do {
                    val lat = cursor.getString(latIndex)
                    val lon = cursor.getString(lonIndex)
                    listOfTracks.add(
                        PointForData(
                            lng = lon.toDouble(),
                            lat = lat.toDouble()
                        )
                    )
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfTracks
    }

    override fun saveNotifications(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        listOfNotifications: List<Notification>
    ): Int {
        insertNotification(alarmDate, alarmHours, alarmMinutes, listOfNotifications)
        return getLastPositionInDb(NOTIFICATION_TIME_NAME)!!
    }

    override fun getListOfNotifications(): List<Notification> {
        val listOfNotifications = mutableListOf<Notification>()
        var cursor: Cursor? = null
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(NOTIFICATION_TIME_NAME)
                .selectParams(SELECT_ALL)
                .select(db)
            if (cursor.moveToFirst()) {
                val indexFromDb = getNotificationColumnIndex(cursor = cursor)
                do {
                    val notification =
                        createNotification(cursor = cursor, indexFromDb = indexFromDb)
                    listOfNotifications.add(notification)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfNotifications
    }

    override fun updateNotifications(updateValue: Long, hours: Int, minutes: Int, id: Int) {
        UpdateIntoDbHelper()
            .setName(tableName = NOTIFICATION_TIME_NAME)
            .updatesValues(nameOfField = NOTIFICATION_TIME, updateValue = updateValue)
            .updatesValues(nameOfField = CURRENT_HOUR, updateValue = hours)
            .updatesValues(nameOfField = CURRENT_MINUTE, updateValue = minutes)
            .where(whereArgs = "$ID = $id")
            .update(db = db)
    }

    override fun saveTracks(tracksInfo: List<TrackForData>) {
        if (getListOfTracks().isNotEmpty()) {
            clearTable()
        }
        val isSend = 0
        tracksInfo.forEach {
            InsertIntoDBHelper()
                .setTableName(name = TRACKERS)
                .addFieldsAndValuesToInsert(
                    nameOfField = ID_FROM_SERVER,
                    insertingValue = it.serverId.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = BEGIN_TIME,
                    insertingValue = it.beginTime.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = RUNNING_TIME,
                    insertingValue = it.time.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = DISTANCE,
                    insertingValue = it.distance.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = IS_SEND,
                    insertingValue = isSend.toString()
                )
                .insertTheValues(db = db)
        }
    }

    override fun clearDbWithWhereArgs(tableName: String, whereArgs: String) {
        UpdateIntoDbHelper()
            .setName(tableName = tableName)
            .where(whereArgs = whereArgs)
            .delete(db = db)
    }

    override fun clearDb(context: Context) {
        setSharedPrefToDefaultValues(context = context)
        db.execSQL("DROP TABLE $TRACKERS")
        db.execSQL("DROP TABLE $ALL_POINTS")
        db.execSQL("DROP TABLE $NOTIFICATION_TIME_NAME")
    }

    override fun updateField(tableName: String, fieldName: String, value: Int, whereArgs: String) {
        UpdateIntoDbHelper()
            .setName(tableName = tableName)
            .updatesValues(nameOfField = fieldName, updateValue = value)
            .where(whereArgs = whereArgs)
            .update(db = db)
    }

    override fun savePoints(trackIdInDb: Int, listOfPoints: List<PointForData>) {
        listOfPoints.forEach {
            InsertIntoDBHelper()
                .setTableName(ALL_POINTS)
                .addFieldsAndValuesToInsert(
                    nameOfField = LATITUDE,
                    insertingValue = it.lat.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = CURRENT_TRACK,
                    insertingValue = trackIdInDb.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = LONGITUDE,
                    insertingValue = it.lng.toString()
                )
                .insertTheValues(db = db)
        }
    }

    override fun checkThisPointIntoDb(currentTrackId: Int): Boolean {
        var cursor: Cursor? = null
        val haveData: Boolean
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = ALL_POINTS)
                .selectParams(allParams = SELECT_ALL)
                .where(whereArgs = "$CURRENT_TRACK = $currentTrackId")
                .select(db = db)
            haveData = cursor.moveToFirst()
        } finally {
            cursor?.close()
        }
        return haveData
    }

    private fun getListOfTracks(): List<TrackFromDb> {
        val listOfTracks = mutableListOf<TrackFromDb>()
        var cursor: Cursor? = null
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = TRACKERS)
                .selectParams(allParams = SELECT_ALL)
                .select(db = db)
            if (cursor.moveToFirst()) {
                val index = getColumnIndex(cursor = cursor)
                do {
                    val track = createTrack(cursor = cursor, index = index)
                    listOfTracks.add(track)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfTracks
    }

    private fun getColumnIndex(cursor: Cursor) =
        TrackColumnIndexFromDb(
            id = cursor.getColumnIndexOrThrow(ID),
            serverId = cursor.getColumnIndexOrThrow(ID_FROM_SERVER),
            beginAt = cursor.getColumnIndexOrThrow(BEGIN_TIME),
            time = cursor.getColumnIndexOrThrow(RUNNING_TIME),
            distance = cursor.getColumnIndexOrThrow(DISTANCE)
        )

    private fun createTrack(cursor: Cursor, index: TrackColumnIndexFromDb) =
        TrackFromDb(
            id = cursor.getInt(index.id),
            serverId = cursor.getInt(index.serverId),
            beginTime = cursor.getLong(index.beginAt),
            time = cursor.getLong(index.time),
            distance = cursor.getInt(index.distance)
        )

    private fun checkDataInDb(): Boolean {
        var cursor: Cursor? = null
        val haveData: Boolean
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = TRACKERS)
                .selectParams(allParams = SELECT_ALL)
                .where(whereArgs = "$IS_SEND = $NOT_SEND")
                .select(db = db)
            haveData = cursor.moveToFirst()
        } finally {
            cursor?.close()
        }
        return haveData
    }

    private fun insertNotification(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        listOfNotifications: List<Notification>
    ) {
        InsertIntoDBHelper()
            .setTableName(name = NOTIFICATION_TIME_NAME)
            .addFieldsAndValuesToInsert(
                nameOfField = NOTIFICATION_TIME,
                insertingValue = alarmDate.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = CURRENT_HOUR,
                insertingValue = alarmHours.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = CURRENT_MINUTE,
                insertingValue = alarmMinutes.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = POSITION_IN_LIST,
                insertingValue = listOfNotifications.size.toString()
            )
            .insertTheValues(db = db)
    }

    private fun getLastPositionInDb(table: String): Int? {
        var cursor: Cursor? = null
        var id: Int? = null
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = table)
                .selectParams(allParams = MAX)
                .select(db = db)
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(ID)
                do {
                    id = cursor.getInt(idIndex)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return id
    }

    private fun getNotificationColumnIndex(cursor: Cursor) =
        NotificationColumnIndexFromDb(
            dateIndex = cursor.getColumnIndexOrThrow(NOTIFICATION_TIME),
            idIndex = cursor.getColumnIndexOrThrow(ID),
            positionIndex = cursor.getColumnIndexOrThrow(POSITION_IN_LIST),
            hoursIndex = cursor.getColumnIndexOrThrow(CURRENT_HOUR),
            minutesIndex = cursor.getColumnIndexOrThrow(CURRENT_MINUTE),
        )

    private fun createNotification(cursor: Cursor, indexFromDb: NotificationColumnIndexFromDb) =
        Notification(
            date = cursor.getString(indexFromDb.dateIndex).toLong(),
            id = cursor.getInt(indexFromDb.idIndex),
            position = cursor.getString(indexFromDb.positionIndex).toInt(),
            hours = cursor.getString(indexFromDb.hoursIndex).toInt(),
            minutes = cursor.getString(indexFromDb.minutesIndex).toInt()
        )

    private fun clearTable() {
        UpdateIntoDbHelper()
            .setName(tableName = TRACKERS)
            .delete(db = db)
    }

    private fun setSharedPrefToDefaultValues(context: Context) {
        context.getSharedPreferences(FITNESS_SHARED, MODE_PRIVATE)
            .edit()
            .putString(CURRENT_TOKEN, null)
            .apply()
        context.getSharedPreferences(FITNESS_SHARED, MODE_PRIVATE)
            .edit()
            .putBoolean(IS_FIRST, true)
            .apply()
    }
}