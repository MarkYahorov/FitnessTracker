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
import com.example.fitnesstracker.screens.main.MainActivity.Companion.EMPTY_VALUE
import com.example.fitnesstracker.screens.main.list.TrackListFragment
import com.example.fitnesstracker.screens.main.notification.NotificationFragment.Companion.MAX
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.ERROR
import java.util.*

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
            val execute = RetrofitBuilder().apiService.getTracks(trackRequest = trackRequest)
            val body = execute.execute().body()
            val list = getListOfTracks()
            if (body != null && body.trackForData.size > list.size) {
                insertDataInDb(body.trackForData)
            }
            if (body != null && getListNotSendTracksFromDb().isNotEmpty()) {
                val trackForData = TrackForData(0, 0, 0, 0)
                val listOfNotSendTracks = getListNotSendTracksFromDb()
                listOfNotSendTracks.forEach {
                    trackForData.beginTime = it.beginTime
                    trackForData.serverId = it.serverId
                    trackForData.distance = it.distance
                    trackForData.time = it.time
                    val serverID = saveTracksWithNullId(trackRequest, trackForData, it)
                    updateOneField(TRACKERS, ID_FROM_SERVER, serverID!!, "$ID = ${it.id}")
                    updateOneField(
                        ALL_POINTS,
                        ID_FROM_SERVER,
                        serverID,
                        "$CURRENT_TRACK = ${it.id}"
                    )
                    updateOneField(TRACKERS, IS_SEND, 0, "$ID = ${it.id}")
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

    override fun clearDb(context: Context): Task<Unit> {
        return Task.callInBackground {
            clearTable(TRACKERS)
            clearTable(ALL_POINTS)
            clearTable(NOTIFICATION_TIME_NAME)
            setSharedPrefToDefaultValues(context = context)
        }
    }

    override fun clearDbWithWereArgs(name: String, whereArgs: String): Task<Unit> {
        return Task.callInBackground {
            UpdateDbHelper()
                .setName(tableName = name)
                .where(whereArgs = whereArgs)
                .delete(db = App.INSTANCE.db)
        }
    }

    override fun insertTrackAndPointsInDbAfterSavinginServer(
        saveTrackResponse: Task<SaveTrackResponse>,
        beginTime: Long,
        calendar: Calendar,
        distance: Int,
        list: List<PointForData>
    ): Task<Unit> {
        return Task.callInBackground {
            when {
                saveTrackResponse.error != null -> {
                    insertTheTrack(null, 1, beginTime, calendar, distance)
                    val id = getLastTrackInDb()
                    insertThePoints(null, id!!, list)
                }
                saveTrackResponse.result.status == ERROR -> {
                    insertTheTrack(null, 1, beginTime, calendar, distance)
                    val id = getLastTrackInDb()
                    insertThePoints(null, id!!,list)

                }
                else -> {
                    insertTheTrack(
                        saveTrackResponse.result.serverId,
                        0,
                        beginTime,
                        calendar,
                        distance
                    )
                    val id = getLastTrackInDb()
                    insertThePoints(saveTrackResponse.result.serverId, id!!, list)
                }
            }
        }
    }

    private fun getLastTrackInDb(): Int? {
        var cursor: Cursor? = null
        var id: Int? = null
        try {
            cursor = SelectDbHelper()
                .nameOfTable(TRACKERS)
                .selectParams("max($ID) as $ID")
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

    private fun insertTheTrack(
        id: Int?,
        isSend: Int,
        beginTime: Long,
        calendar: Calendar,
        distance: Int
    ) {
        InsertDBHelper()
            .setTableName(TRACKERS)
            .addFieldsAndValuesToInsert(ID_FROM_SERVER, id.toString())
            .addFieldsAndValuesToInsert(BEGIN_TIME, beginTime.toString())
            .addFieldsAndValuesToInsert(
                RUNNING_TIME,
                calendar.time.time.toString()
            )
            .addFieldsAndValuesToInsert(IS_SEND, isSend.toString())
            .addFieldsAndValuesToInsert(DISTANCE, distance.toString())
            .insertTheValues(App.INSTANCE.db)
    }

    private fun insertThePoints(id: Int?, trackIdInDb: Int, list: List<PointForData>) {
        list.forEach {
            InsertDBHelper()
                .setTableName(ALL_POINTS)
                .addFieldsAndValuesToInsert(
                    ID_FROM_SERVER,
                    id.toString()
                )
                .addFieldsAndValuesToInsert(
                    LATITUDE,
                    it.lat.toString()
                )
                .addFieldsAndValuesToInsert(CURRENT_TRACK, trackIdInDb.toString())
                .addFieldsAndValuesToInsert(
                    LONGITUDE,
                    it.lng.toString()
                )
                .insertTheValues(App.INSTANCE.db)
        }
    }

    private fun setSharedPrefToDefaultValues(context: Context) {
        context.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putString(CURRENT_TOKEN, EMPTY_VALUE)
            .apply()
        context.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(TrackListFragment.IS_FIRST, true)
            .apply()
    }

    private fun updateOneField(
        tableName: String,
        fieldName: String,
        value: Int,
        whereArgs: String
    ) {
        UpdateDbHelper()
            .setName(tableName = tableName)
            .updatesValues(nameOfField = fieldName, updateValue = value)
            .where(whereArgs = whereArgs)
            .update(db = App.INSTANCE.db)
    }

    private fun getLastNotificationFromDb(): Int {
        var cursor: Cursor? = null
        var id = 0
        try {
            cursor = SelectDbHelper()
                .nameOfTable(table = NOTIFICATION_TIME_NAME)
                .selectParams(allParams = MAX)
                .select(db = App.INSTANCE.db)
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
                insertingValue = list.size.toString()
            )
            .insertTheValues(db = App.INSTANCE.db)
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
                    token = trackRequest.token,
                    serverId = trackForData.serverId,
                    beginTime = trackForData.beginTime,
                    time = trackForData.time,
                    distance = trackForData.distance,
                    pointForData = getListOfPointsToCurrentTrack(tracks.id!!)
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
                .selectParams(allParams = "*")
                .nameOfTable(table = NOTIFICATION_TIME_NAME)
                .select(db = App.INSTANCE.db)
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
                .nameOfTable(table = ALL_POINTS)
                .selectParams(allParams = "*")
                .where(whereArgs = "$CURRENT_TRACK = $id")
                .select(db = App.INSTANCE.db)
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
            cursor = SelectDbHelper()
                .nameOfTable(table = TRACKERS)
                .selectParams(allParams = "*")
                .select(db = App.INSTANCE.db)
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

    private fun getListNotSendTracksFromDb(): List<Tracks> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<Tracks>()
        try {
            cursor = SelectDbHelper()
                .nameOfTable(table = TRACKERS)
                .selectParams(allParams = "*")
                .where(whereArgs = "$IS_SEND = 1")
                .select(db = App.INSTANCE.db)
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
            clearTable(name = TRACKERS)
        }
        val isSend = 0
        tracksInfo.forEach {
            InsertDBHelper()
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
                .insertTheValues(db = App.INSTANCE.db)
        }

    }

    private fun clearTable(name: String) {
        UpdateDbHelper()
            .setName(tableName = name)
            .delete(db = App.INSTANCE.db)
    }
}