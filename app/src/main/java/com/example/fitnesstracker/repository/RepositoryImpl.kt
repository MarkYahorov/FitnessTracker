package com.example.fitnesstracker.repository

import android.content.Context
import android.database.Cursor
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ALL_POINTS
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.BEGIN_TIME
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_HOUR
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_MINUTE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_TRACK
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.DISTANCE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ID
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ID_FROM_SERVER
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.IS_SEND
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.LATITUDE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.LONGITUDE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.NOTIFICATION_TIME
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.NOTIFICATION_TIME_NAME
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.POSITION_IN_LIST
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.RUNNING_TIME
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.TRACKERS
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectDbHelper
import com.example.fitnesstracker.data.database.helpers.UpdateDbHelper
import com.example.fitnesstracker.data.retrofit.RetrofitBuilder
import com.example.fitnesstracker.models.TrackColumnIndexFromDb
import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.notification.Notification
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.points.PointsRequest
import com.example.fitnesstracker.models.points.PointsResponse
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse
import com.example.fitnesstracker.models.save.SaveTrackRequest
import com.example.fitnesstracker.models.save.SaveTrackResponse
import com.example.fitnesstracker.models.tracks.TrackForData
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.TrackResponse
import com.example.fitnesstracker.models.tracks.Tracks
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.example.fitnesstracker.screens.main.MainActivity
import com.example.fitnesstracker.screens.main.MainActivity.Companion.EMPTY_VALUE
import com.example.fitnesstracker.screens.main.list.TrackListFragment
import com.example.fitnesstracker.screens.main.notification.NotificationFragment.Companion.MAX

class RepositoryImpl : Repository {

    override fun login(loginRequest: LoginRequest): Task<LoginResponse> {
        return Task.callInBackground {
            val execute = RetrofitBuilder().apiService.login(loginRequest = loginRequest)
            execute.execute().body()
        }
    }

    override fun registration(registrationRequest: RegistrationRequest): Task<RegistrationResponse> {
        return Task.callInBackground {
            val execute =
                RetrofitBuilder().apiService.registration(registrationRequest = registrationRequest)
            execute.execute().body()
        }
    }

    override fun getTracks(trackRequest: TrackRequest): Task<TrackResponse> {
        return Task.callInBackground {
            val trackForData = TrackForData(0, 0, 0, 0)
            val execute = RetrofitBuilder().apiService.getTracks(trackRequest = trackRequest)
            val body = execute.execute().body()
            val list = getListOfTracks()
            if (body != null && body.trackForData.size > list.size) {
                insertDataInDb(body.trackForData)
            }
            if (body != null && getListNotSendTracksFromDb().isNotEmpty()) {
                val listOfNotSendTracks = getListNotSendTracksFromDb()
                listOfNotSendTracks.forEach {
                    trackForData.beginTime = it.beginTime
                    trackForData.serverId = it.serverId
                    trackForData.distance = it.distance
                    trackForData.time = it.time
                    val serverID = saveTracksWithNullId(trackRequest, trackForData, it)
                    updateOneElement(TRACKERS, ID_FROM_SERVER, serverID!!, "$ID = ${it.id}")
                    updateOneElement(
                        ALL_POINTS,
                        ID_FROM_SERVER,
                        serverID,
                        "$CURRENT_TRACK = ${it.id}"
                    )
                    updateOneElement(TRACKERS, IS_SEND, 0, "$ID = ${it.id}")
                }
            }
            body
        }
    }

    override fun getPointsForCurrentTrack(pointsRequest: PointsRequest): Task<PointsResponse> {
        return Task.callInBackground {
            val execute =
                RetrofitBuilder().apiService.getPointsForCurrentTrack(pointsRequest = pointsRequest)
            execute.clone().execute().body()
        }
    }

    override fun saveTrack(saveTrackRequest: SaveTrackRequest): Task<SaveTrackResponse> {
        return Task.callInBackground {
            val execute =
                RetrofitBuilder().apiService.saveTrack(savePointsRequest = saveTrackRequest)
            execute.execute().body()
        }
    }

    override fun getListOfTrack(): Task<List<Tracks>> {
        return Task.callInBackground {
            getListOfTracks()
        }
    }

    override fun getListOfNotification(): Task<List<Notification>> {
        return Task.callInBackground {
            getListOfNotificationFromDb()
        }
    }

    override fun getPointsForCurrentTrackFromDb(id: Int): Task<List<PointForData>> {
        return Task.callInBackground {
            getListOfPointsToCurrentTrack(id = id)
        }
    }

    override fun insertNotification(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        list: List<Notification>
    ): Task<Int> {
        return Task.callInBackground {
            insertNotificationInDb(alarmDate, alarmHours, alarmMinutes, list)
        }.continueWith {
            getLastNotificationFromDb()
        }
    }

    override fun updateNotifications(
        updateValue: Long,
        hours: Int,
        minutes: Int,
        id: Int
    ): Task<Unit> {
        return Task.callInBackground {
            UpdateDbHelper()
                .setName(NOTIFICATION_TIME_NAME)
                .updatesValues(NOTIFICATION_TIME, updateValue)
                .updatesValues(CURRENT_HOUR, hours)
                .updatesValues(CURRENT_MINUTE, minutes)
                .where("$ID = $id")
                .update(App.INSTANCE.db)
        }
    }

    private fun updateOneElement(
        tableName: String,
        fieldName: String,
        value: Int,
        whereArgs: String
    ) {
        UpdateDbHelper()
            .setName(tableName)
            .updatesValues(fieldName, value)
            .where(whereArgs)
            .update(App.INSTANCE.db)
    }

    private fun getLastNotificationFromDb(): Int {
        var cursor: Cursor? = null
        var id = 0
        try {
            cursor = SelectDbHelper()
                .nameOfTable(NOTIFICATION_TIME_NAME)
                .selectParams(MAX)
                .select(App.INSTANCE.db)
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

    private fun insertNotificationInDb(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        list: List<Notification>
    ) {
        InsertDBHelper()
            .setTableName(NOTIFICATION_TIME_NAME)
            .addFieldsAndValuesToInsert(
                NOTIFICATION_TIME,
                alarmDate.toString()
            )
            .addFieldsAndValuesToInsert(CURRENT_HOUR, alarmHours.toString())
            .addFieldsAndValuesToInsert(CURRENT_MINUTE, alarmMinutes.toString())
            .addFieldsAndValuesToInsert(POSITION_IN_LIST, list.size.toString())
            .insertTheValues(App.INSTANCE.db)
    }

    private fun saveTracksWithNullId(
        trackRequest: TrackRequest,
        trackForData: TrackForData,
        tracks: Tracks
    ): Int? {
        var id: Int? = null
        if (trackForData.serverId == 0) {
            trackForData.serverId = null
            id = saveTrack(
                SaveTrackRequest(
                    trackRequest.token,
                    trackForData.serverId,
                    trackForData.beginTime,
                    trackForData.time,
                    trackForData.distance,
                    getListOfPointsToCurrentTrack(tracks.id!!)
                )
            ).result.serverId
        }
        return id
    }

    private fun getListOfNotificationFromDb(): List<Notification> {
        var cursor: Cursor? = null
        val listOfNotification = mutableListOf<Notification>()
        try {
            cursor = SelectDbHelper()
                .selectParams("*")
                .nameOfTable(NOTIFICATION_TIME_NAME)
                .select(App.INSTANCE.db)
            if (cursor.moveToFirst()) {
                val timeId = cursor.getColumnIndexOrThrow(NOTIFICATION_TIME)
                val idColumn = cursor.getColumnIndexOrThrow(ID)
                val positionIndex = cursor.getColumnIndexOrThrow(POSITION_IN_LIST)
                val hoursIndex = cursor.getColumnIndexOrThrow(CURRENT_HOUR)
                val minutesIndex = cursor.getColumnIndexOrThrow(CURRENT_MINUTE)
                do {
                    val time = cursor.getString(timeId).toLong()
                    val id = cursor.getInt(idColumn)
                    val position = cursor.getString(positionIndex).toInt()
                    val hour = cursor.getString(hoursIndex).toInt()
                    val minute = cursor.getString(minutesIndex).toInt()
                    listOfNotification.add(
                        Notification(
                            id = id,
                            date = time,
                            position = position,
                            hours = hour,
                            minutes = minute
                        )
                    )
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfNotification
    }

    private fun getListOfPointsToCurrentTrack(id: Int): List<PointForData> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<PointForData>()
        try {
            cursor = SelectDbHelper()
                .nameOfTable(ALL_POINTS)
                .selectParams("*")
                .where("$CURRENT_TRACK = $id")
                .select(App.INSTANCE.db)
            if (cursor.moveToFirst()) {
                val latIndex = cursor.getColumnIndexOrThrow(LATITUDE)
                val lonIndex = cursor.getColumnIndexOrThrow(LONGITUDE)
                do {
                    val lat = cursor.getString(latIndex)
                    val lon = cursor.getString(lonIndex)
                    listOfTracks.add(PointForData(lon.toDouble(), lat.toDouble()))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfTracks
    }

    private fun getListOfTracks(): List<Tracks> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<Tracks>()
        try {
            cursor = SelectDbHelper().nameOfTable(TRACKERS)
                .selectParams("*")
                .select(App.INSTANCE.db)
            if (cursor.moveToFirst()) {
                val index = getColumnIndex(cursor)
                do {
                    val track = createTrack(cursor, index)
                    listOfTracks.add(track)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfTracks
    }

    private fun getListNotSendTracksFromDb(): List<Tracks> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<Tracks>()
        try {
            cursor = SelectDbHelper().nameOfTable(TRACKERS)
                .selectParams("*")
                .where("$IS_SEND = 1")
                .select(App.INSTANCE.db)
            if (cursor.moveToFirst()) {
                val index = getColumnIndex(cursor)
                do {
                    val track = createTrack(cursor, index)
                    listOfTracks.add(track)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfTracks
    }

    private fun createTrack(cursor: Cursor, index: TrackColumnIndexFromDb) = Tracks(
        id = cursor.getInt(index.id),
        serverId = cursor.getInt(index._id),
        beginTime = cursor.getLong(index.beginAt),
        time = cursor.getLong(index.time),
        distance = cursor.getInt(index.distance)
    )

    private fun getColumnIndex(cursor: Cursor) = TrackColumnIndexFromDb(
        id = cursor.getColumnIndexOrThrow(ID),
        _id = cursor.getColumnIndexOrThrow(ID_FROM_SERVER),
        beginAt = cursor.getColumnIndexOrThrow(BEGIN_TIME),
        time = cursor.getColumnIndexOrThrow(RUNNING_TIME),
        distance = cursor.getColumnIndexOrThrow(DISTANCE)
    )

    private fun insertDataInDb(tracksInfo: List<TrackForData>) {
        if (getListOfTracks().isNotEmpty()) {
            clearTable(TRACKERS)
        }
        val isSend = 0
        tracksInfo.forEach {
            InsertDBHelper()
                .setTableName(TRACKERS)
                .addFieldsAndValuesToInsert(ID_FROM_SERVER, it.serverId.toString())
                .addFieldsAndValuesToInsert(BEGIN_TIME, it.beginTime.toString())
                .addFieldsAndValuesToInsert(RUNNING_TIME, it.time.toString())
                .addFieldsAndValuesToInsert(DISTANCE, it.distance.toString())
                .addFieldsAndValuesToInsert(IS_SEND, isSend.toString())
                .insertTheValues(App.INSTANCE.db)
        }

    }

    private fun clearTable(name: String) {
        UpdateDbHelper()
            .setName(name)
            .delete(App.INSTANCE.db)
    }

    override fun clearDb(context: Context): Task<Unit> {
        return Task.callInBackground {
            clearTable(TRACKERS)
            clearTable(ALL_POINTS)
            clearTable(NOTIFICATION_TIME_NAME)
            context.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
                .edit()
                .putString(CURRENT_TOKEN, EMPTY_VALUE)
                .apply()
            context.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(TrackListFragment.IS_FIRST, true)
                .apply()
        }
    }

    override fun clearDbWithWereArgs(name: String, whereArgs: String): Task<Unit> {
        return Task.callInBackground {
            UpdateDbHelper()
                .setName(name)
                .where(whereArgs)
                .delete(App.INSTANCE.db)
        }
    }
}