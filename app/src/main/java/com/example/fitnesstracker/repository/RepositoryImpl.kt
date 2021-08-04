package com.example.fitnesstracker.repository

import android.database.Cursor
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_HOUR
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_MINUTE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.IS_SEND
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.POSITION_IN_LIST
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectDbHelper
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
            val trackForData = TrackForData(0,0,0, 0)
            val execute = RetrofitBuilder().apiService.getTracks(trackRequest = trackRequest)
            val body = execute.execute().body()
            val list = getListOfTracks()
            if (body != null && body.trackForData.size > list.size) {
                insertDataInDb(body.trackForData)
            }
            if (body != null && body.trackForData.size < list.size) {
                val listOfNotSendTracks = getListNotSendTracksFromDb()
                listOfNotSendTracks.forEach {
                    trackForData.beginTime = it.beginTime
                    trackForData.serverId = it.serverId
                    trackForData.distance = it.distance
                    trackForData.time = it.time
                    val serverID = saveTracksWithNullId(trackRequest, trackForData,it)
                    App.INSTANCE.db.execSQL("UPDATE trackers SET _id = $serverID WHERE id = ${it.id}")
                    App.INSTANCE.db.execSQL("UPDATE allPoints SET _id = $serverID WHERE currentTrack = ${it.id}")
                    App.INSTANCE.db.execSQL("UPDATE trackers SET isSend = 0 WHERE id = ${it.id}")
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
        return Task.callInBackground{
            getListOfPointsToCurrentTrack(id = id)
        }
    }

    private fun saveTracksWithNullId(trackRequest: TrackRequest, trackForData:TrackForData, tracks: Tracks): Int? {
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
                .nameOfTable("NotificationTime")
                .select(App.INSTANCE.db)
            if (cursor.moveToFirst()) {
                val timeId = cursor.getColumnIndexOrThrow(FitnessDatabase.NOTIFICATION_TIME)
                val idColumn = cursor.getColumnIndexOrThrow(FitnessDatabase.ID)
                val positionIndex = cursor.getColumnIndexOrThrow(POSITION_IN_LIST)
                val hoursIndex = cursor.getColumnIndexOrThrow(CURRENT_HOUR)
                val minutesIndex = cursor.getColumnIndexOrThrow(CURRENT_MINUTE)
                do {
                    val time = cursor.getString(timeId).toLong()
                    val id = cursor.getInt(idColumn)
                    val position = cursor.getString(positionIndex).toInt()
                    val hour = cursor.getString(hoursIndex).toInt()
                    val minute = cursor.getString(minutesIndex).toInt()
                    listOfNotification.add(Notification(id = id, date = time, position =  position, hours = hour, minutes = minute))
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
                .nameOfTable("allPoints")
                .selectParams("*")
                .where("currentTrack = $id")
                .select(App.INSTANCE.db)
            if (cursor.moveToFirst()) {
                val latIndex = cursor.getColumnIndexOrThrow("latitude")
                val lonIndex = cursor.getColumnIndexOrThrow("longitude")
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
            cursor = SelectDbHelper().nameOfTable("trackers")
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
            cursor = SelectDbHelper().nameOfTable("trackers")
                .selectParams("*")
                .where("isSend = 1")
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
        id = cursor.getColumnIndexOrThrow("id"),
        _id = cursor.getColumnIndexOrThrow("_id"),
        beginAt = cursor.getColumnIndexOrThrow("beginAt"),
        time = cursor.getColumnIndexOrThrow("time"),
        distance = cursor.getColumnIndexOrThrow("distance")
    )

    private fun insertDataInDb(tracksInfo: List<TrackForData>) {
        clearDb()
        val isSend = 0
        tracksInfo.forEach {
            InsertDBHelper()
                .setTableName("trackers")
                .addFieldsAndValuesToInsert(FitnessDatabase.ID_FROM_SERVER, it.serverId.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.BEGIN_TIME, it.beginTime.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.RUNNING_TIME, it.time.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.DISTANCE, it.distance.toString())
                .addFieldsAndValuesToInsert(IS_SEND, isSend.toString())
                .insertTheValues(App.INSTANCE.db)
        }
    }

    private fun clearDb() {
        App.INSTANCE.db.execSQL("DELETE FROM trackers")
    }
}