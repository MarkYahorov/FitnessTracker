package com.example.fitnesstracker.repository

import android.content.Context
import android.database.Cursor
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.database.helpers.InsertIntoDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectFromDbHelper
import com.example.fitnesstracker.data.database.helpers.UpdateIntoDbHelper
import com.example.fitnesstracker.models.columnIndex.NotificationColumnIndexFromDb
import com.example.fitnesstracker.models.columnIndex.TrackColumnIndexFromDb
import com.example.fitnesstracker.models.notification.Notification
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.save.SaveTrackResponse
import com.example.fitnesstracker.models.tracks.TrackFromDb
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.example.fitnesstracker.screens.main.list.TrackListFragment
import com.example.fitnesstracker.screens.main.notification.NotificationFragment
import com.example.fitnesstracker.screens.running.RunningActivity
import java.util.*

class RepositoryForDbImpl: RepositoryForDB {

    override fun getListOfTrack(): Task<List<TrackFromDb>> {
        return Task.callInBackground {
            getListOfTracks()
        }
    }

    override fun getListOfNotification(): Task<List<Notification>> {
        return Task.callInBackground {
            getListOfNotificationFromDb()
        }
    }

    override fun insertNotification(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        listOfNotifications: List<Notification>
    ): Task<Int> {
        return Task.callInBackground {
            insertNotificationInDb(
                alarmDate = alarmDate,
                alarmHours = alarmHours,
                alarmMinutes = alarmMinutes,
                listOfNotifications = listOfNotifications
            )
        }.continueWith {
            getLastPositionInDb(FitnessDatabase.NOTIFICATION_TIME_NAME)
        }
    }

    override fun updateNotifications(
        updateValue: Long,
        hours: Int,
        minutes: Int,
        id: Int
    ): Task<Unit> {
        return Task.callInBackground {
            UpdateIntoDbHelper()
                .setName(tableName = FitnessDatabase.NOTIFICATION_TIME_NAME)
                .updatesValues(nameOfField = FitnessDatabase.NOTIFICATION_TIME, updateValue = updateValue)
                .updatesValues(nameOfField = FitnessDatabase.CURRENT_HOUR, updateValue = hours)
                .updatesValues(nameOfField = FitnessDatabase.CURRENT_MINUTE, updateValue = minutes)
                .where(whereArgs = "${FitnessDatabase.ID} = $id")
                .update(db = App.INSTANCE.myDataBase)
        }
    }

    override fun clearDb(context: Context): Task<Unit> {
        return Task.callInBackground {
            setSharedPrefToDefaultValues(context = context)
            App.INSTANCE.myDataBase.execSQL("DROP TABLE ${FitnessDatabase.TRACKERS}")
            App.INSTANCE.myDataBase.execSQL("DROP TABLE ${FitnessDatabase.ALL_POINTS}")
            App.INSTANCE.myDataBase.execSQL("DROP TABLE ${FitnessDatabase.NOTIFICATION_TIME_NAME}")
        }
    }

    override fun clearDbWithWereArgs(tableName: String, whereArgs: String): Task<Unit> {
        return Task.callInBackground {
            UpdateIntoDbHelper()
                .setName(tableName = tableName)
                .where(whereArgs = whereArgs)
                .delete(db = App.INSTANCE.myDataBase)
        }
    }

    override fun insertTrackAndPointsInDbAfterSavingInServer(
        saveTrackResponse: Task<SaveTrackResponse>,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        listOfPoints: List<PointForData>
    ): Task<Unit> {
        return Task.callInBackground {
            when {
                saveTrackResponse.error != null -> {
                    insertDataAfterRunning(
                        id = null,
                        beginTime = beginTime,
                        calendar = calendar,
                        distance = distance,
                        listOfPoints = listOfPoints,
                        isSend = 1
                    )
                }
                saveTrackResponse.result.status == RunningActivity.ERROR -> {
                    insertDataAfterRunning(
                        id = null,
                        beginTime = beginTime,
                        calendar = calendar,
                        distance = distance,
                        listOfPoints = listOfPoints,
                        isSend = 1
                    )
                }
                else -> {
                    insertDataAfterRunning(
                        id = saveTrackResponse.result.serverId,
                        beginTime = beginTime,
                        calendar = calendar,
                        distance = distance,
                        listOfPoints = listOfPoints,
                        isSend = 0
                    )
                }
            }
        }
    }

    private fun getListOfTracks(): List<TrackFromDb> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<TrackFromDb>()
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = FitnessDatabase.TRACKERS)
                .selectParams(allParams = "*")
                .select(db = App.INSTANCE.myDataBase)
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

    private fun createTrack(cursor: Cursor, index: TrackColumnIndexFromDb) = TrackFromDb(
        id = cursor.getInt(index.id),
        serverId = cursor.getInt(index.serverId),
        beginTime = cursor.getLong(index.beginAt),
        time = cursor.getLong(index.time),
        distance = cursor.getInt(index.distance)
    )

    private fun getColumnIndex(cursor: Cursor) = TrackColumnIndexFromDb(
        id = cursor.getColumnIndexOrThrow(FitnessDatabase.ID),
        serverId = cursor.getColumnIndexOrThrow(FitnessDatabase.ID_FROM_SERVER),
        beginAt = cursor.getColumnIndexOrThrow(FitnessDatabase.BEGIN_TIME),
        time = cursor.getColumnIndexOrThrow(FitnessDatabase.RUNNING_TIME),
        distance = cursor.getColumnIndexOrThrow(FitnessDatabase.DISTANCE)
    )

    private fun insertDataAfterRunning(
        id: Int?,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        listOfPoints: List<PointForData>,
        isSend: Int
    ) {
        insertTheTrack(
            id = id,
            isSend = isSend,
            beginTime = beginTime,
            calendar = calendar,
            distance = distance
        )
        val trackIdInDb = getLastPositionInDb(FitnessDatabase.TRACKERS)
        insertThePoints(
            serverId = id,
            trackIdInDb = trackIdInDb!!,
            listOfPoints = listOfPoints
        )
    }

    private fun insertThePoints(
        serverId: Int?,
        trackIdInDb: Int,
        listOfPoints: List<PointForData>
    ) {
        listOfPoints.forEach {
            InsertIntoDBHelper()
                .setTableName(name = FitnessDatabase.ALL_POINTS)
                .addFieldsAndValuesToInsert(
                    nameOfField = FitnessDatabase.ID_FROM_SERVER,
                    insertingValue = serverId.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = FitnessDatabase.LATITUDE,
                    insertingValue = it.lat.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = FitnessDatabase.CURRENT_TRACK,
                    insertingValue = trackIdInDb.toString()
                )
                .addFieldsAndValuesToInsert(
                    nameOfField = FitnessDatabase.LONGITUDE,
                    insertingValue = it.lng.toString()
                )
                .insertTheValues(db = App.INSTANCE.myDataBase)
        }
    }

    private fun getLastPositionInDb(table: String): Int? {
        var cursor: Cursor? = null
        var id: Int? = null
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = table)
                .selectParams(allParams = NotificationFragment.MAX)
                .select(db = App.INSTANCE.myDataBase)
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(FitnessDatabase.ID)
                do {
                    id = cursor.getInt(idIndex)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return id
    }

    private fun insertTheTrack(
        id: Int?,
        isSend: Int,
        beginTime: Long,
        calendar: Calendar,
        distance: Int
    ) {
        InsertIntoDBHelper()
            .setTableName(name = FitnessDatabase.TRACKERS)
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.ID_FROM_SERVER,
                insertingValue = id.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.BEGIN_TIME,
                insertingValue = beginTime.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.RUNNING_TIME,
                insertingValue = calendar.time.time.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.IS_SEND,
                insertingValue = isSend.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.DISTANCE,
                insertingValue = distance.toString()
            )
            .insertTheValues(db = App.INSTANCE.myDataBase)
    }

    private fun setSharedPrefToDefaultValues(context: Context) {
        context.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putString(CURRENT_TOKEN, null)
            .apply()
        context.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(TrackListFragment.IS_FIRST, true)
            .apply()
    }

    private fun insertNotificationInDb(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        listOfNotifications: List<Notification>
    ) {
        InsertIntoDBHelper()
            .setTableName(name = FitnessDatabase.NOTIFICATION_TIME_NAME)
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.NOTIFICATION_TIME,
                insertingValue = alarmDate.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.CURRENT_HOUR,
                insertingValue = alarmHours.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.CURRENT_MINUTE,
                insertingValue = alarmMinutes.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = FitnessDatabase.POSITION_IN_LIST,
                insertingValue = listOfNotifications.size.toString()
            )
            .insertTheValues(db = App.INSTANCE.myDataBase)
    }

    private fun getListOfNotificationFromDb(): List<Notification> {
        var cursor: Cursor? = null
        val listOfNotification = mutableListOf<Notification>()
        try {
            cursor = SelectFromDbHelper()
                .selectParams(allParams = "*")
                .nameOfTable(table = FitnessDatabase.NOTIFICATION_TIME_NAME)
                .select(db = App.INSTANCE.myDataBase)
            if (cursor.moveToFirst()) {
                val indexFromDb = getNotificationColumnIndex(cursor = cursor)
                do {
                    val notification =
                        createNotification(cursor = cursor, indexFromDb = indexFromDb)
                    listOfNotification.add(notification)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfNotification
    }

    private fun getNotificationColumnIndex(cursor: Cursor) = NotificationColumnIndexFromDb(
        dateIndex = cursor.getColumnIndexOrThrow(FitnessDatabase.NOTIFICATION_TIME),
        idIndex = cursor.getColumnIndexOrThrow(FitnessDatabase.ID),
        positionIndex = cursor.getColumnIndexOrThrow(FitnessDatabase.POSITION_IN_LIST),
        hoursIndex = cursor.getColumnIndexOrThrow(FitnessDatabase.CURRENT_HOUR),
        minutesIndex = cursor.getColumnIndexOrThrow(FitnessDatabase.CURRENT_MINUTE),
    )

    private fun createNotification(cursor: Cursor, indexFromDb: NotificationColumnIndexFromDb) =
        Notification(
            date = cursor.getString(indexFromDb.dateIndex).toLong(),
            id = cursor.getInt(indexFromDb.idIndex),
            position = cursor.getString(indexFromDb.positionIndex).toInt(),
            hours = cursor.getString(indexFromDb.hoursIndex).toInt(),
            minutes = cursor.getString(indexFromDb.minutesIndex).toInt()
        )
}