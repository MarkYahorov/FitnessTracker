package com.example.fitnesstracker.repository

import android.database.Cursor
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ALL_POINTS
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.BEGIN_TIME
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_TRACK
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.DISTANCE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ID
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ID_FROM_SERVER
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.IS_SEND
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.LATITUDE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.LONGITUDE
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.RUNNING_TIME
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.TRACKERS
import com.example.fitnesstracker.data.database.helpers.InsertIntoDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectFromDbHelper
import com.example.fitnesstracker.data.database.helpers.UpdateIntoDbHelper
import com.example.fitnesstracker.models.columnIndex.TrackColumnIndexFromDb
import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.points.PointsRequest
import com.example.fitnesstracker.models.points.PointsResponse
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse
import com.example.fitnesstracker.models.save.SaveTrackRequest
import com.example.fitnesstracker.models.save.SaveTrackResponse
import com.example.fitnesstracker.models.tracks.TrackForData
import com.example.fitnesstracker.models.tracks.TrackFromDb
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.TrackResponse
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.ERROR

class RepositoryFromServerImpl : RepositoryFromServer {

    companion object {
        private const val SELECT_ALL = "*"
        private const val EMPTY_VALUE = ""
        private const val NOT_SEND = 1
        private const val DEFAULT_VALUE_INT = 0
        private const val DEFAULT_VALUE_LONG = 0L
    }

    override fun login(loginRequest: LoginRequest): Task<LoginResponse> {
        return Task.callInBackground {
            val execute = App.INSTANCE.apiService.login(loginRequest = loginRequest)
            execute.execute().body()
        }
    }

    override fun registration(registrationRequest: RegistrationRequest): Task<RegistrationResponse> {
        return Task.callInBackground {
            val execute =
                App.INSTANCE.apiService.registration(registrationRequest = registrationRequest)
            execute.execute().body()
        }
    }

    override fun getTracks(trackRequest: TrackRequest?): Task<TrackResponse> {
        return Task.callInBackground {
            val execute = App.INSTANCE.apiService.getTracks(trackRequest = trackRequest)
            val body = execute.execute().body()
            val list = getListOfTracks()
            if (body != null && body.trackForData.size > list.size) {
                insertDataInDb(tracksInfo = body.trackForData)
            }
            if ((body?.error == null || body.status != ERROR) && getListNotSendTracksFromDb().isNotEmpty()) {
                val trackForData = TrackForData(
                    DEFAULT_VALUE_INT,
                    DEFAULT_VALUE_LONG,
                    DEFAULT_VALUE_LONG,
                    DEFAULT_VALUE_INT
                )
                val listOfNotSendTracks = getListNotSendTracksFromDb()
                listOfNotSendTracks.forEach {
                    trackForData.beginTime = it.beginTime
                    trackForData.serverId = it.serverId
                    trackForData.distance = it.distance
                    trackForData.time = it.time
                    saveTracksWithNullId(
                        trackRequest = trackRequest,
                        trackForData = trackForData,
                        trackFromDb = it
                    )
                }
            }
            body
        }
    }

    override fun getPointsForCurrentTrack(
        idInDb: Int,
        serverId: Int,
        pointsRequest: PointsRequest?
    ): Task<List<PointForData>> {
        val list = mutableListOf<PointForData>()
        return getPointsFromServer(pointsRequest = pointsRequest)
            .continueWith {
                when {
                    it.error != null -> {
                        getListOfPointsToCurrentTrack(trackId = idInDb).forEach { listFromDb ->
                            list.add(PointForData(listFromDb.lng, listFromDb.lat))
                        }
                    }
                    it.result.status == ERROR -> {
                        getListOfPointsToCurrentTrack(trackId = idInDb).forEach { listFromDb ->
                            list.add(PointForData(listFromDb.lng, listFromDb.lat))
                        }
                    }
                    else -> {
                        list.addAll(it.result.pointForData)
                        if (!checkThisPointIntoDb(currentTrackId = idInDb)) {
                            insertThePoints(
                                serverId = serverId,
                                trackIdInDb = idInDb,
                                listOfPoints = list
                            )
                        }
                    }
                }
                list
            }
    }

    override fun saveTrack(saveTrackRequest: SaveTrackRequest?): Task<SaveTrackResponse> {
        return Task.callInBackground {
            val execute =
                App.INSTANCE.apiService.saveTrack(savePointsRequest = saveTrackRequest)
            execute.clone().execute().body()
        }
    }

    private fun getPointsFromServer(pointsRequest: PointsRequest?): Task<PointsResponse> {
        return Task.callInBackground {
            val execute =
                App.INSTANCE.apiService.getPointsForCurrentTrack(pointsRequest = pointsRequest)
            execute.clone().execute().body()
        }
    }

    private fun checkThisPointIntoDb(currentTrackId: Int): Boolean {
        var cursor: Cursor? = null
        val haveData: Boolean
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = ALL_POINTS)
                .selectParams(allParams = SELECT_ALL)
                .where(whereArgs = "$CURRENT_TRACK = $currentTrackId")
                .select(db = App.INSTANCE.myDataBase)
            haveData = cursor.moveToFirst()
        } finally {
            cursor?.close()
        }
        return haveData
    }

    private fun insertThePoints(
        serverId: Int?,
        trackIdInDb: Int,
        listOfPoints: List<PointForData>
    ) {
        listOfPoints.forEach {
            InsertIntoDBHelper()
                .setTableName(name = ALL_POINTS)
                .addFieldsAndValuesToInsert(
                    nameOfField = ID_FROM_SERVER,
                    insertingValue = serverId.toString()
                )
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
                .insertTheValues(db = App.INSTANCE.myDataBase)
        }
    }

    private fun updateOneField(
        tableName: String,
        fieldName: String,
        value: Int,
        whereArgs: String
    ) {
        UpdateIntoDbHelper()
            .setName(tableName = tableName)
            .updatesValues(nameOfField = fieldName, updateValue = value)
            .where(whereArgs = whereArgs)
            .update(db = App.INSTANCE.myDataBase)
    }

    private fun saveTracksWithNullId(
        trackRequest: TrackRequest?,
        trackForData: TrackForData,
        trackFromDb: TrackFromDb
    ) {
        if (trackForData.serverId == DEFAULT_VALUE_INT) {
            trackForData.serverId = null
            saveTrack(
                saveTrackRequest = SaveTrackRequest(
                    token = trackRequest?.token ?: EMPTY_VALUE,
                    serverId = trackForData.serverId,
                    beginTime = trackForData.beginTime,
                    time = trackForData.time,
                    distance = trackForData.distance,
                    pointForData = getListOfPointsToCurrentTrack(trackFromDb.id)
                )
            ).continueWith {
                processSaveResponseToInsertIntoDbNullIdTracksAndPoints(
                    saveResponse = it,
                    trackFromDb = trackFromDb
                )
            }
        }
    }

    private fun processSaveResponseToInsertIntoDbNullIdTracksAndPoints(
        saveResponse: Task<SaveTrackResponse>,
        trackFromDb: TrackFromDb
    ) {
        val id = saveResponse.result.serverId
        if (id != null) {
            updateOneField(
                tableName = TRACKERS,
                fieldName = ID_FROM_SERVER,
                value = id,
                whereArgs = "$ID = ${trackFromDb.id}"
            )
            updateOneField(
                tableName = ALL_POINTS,
                fieldName = ID_FROM_SERVER,
                value = id,
                whereArgs = "$CURRENT_TRACK = ${trackFromDb.id}"
            )
            updateOneField(
                tableName = TRACKERS,
                fieldName = IS_SEND,
                value = 0,
                whereArgs = "$ID = ${trackFromDb.id}"
            )
        }
    }

    private fun getListOfPointsToCurrentTrack(trackId: Int): List<PointForData> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<PointForData>()
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = ALL_POINTS)
                .selectParams(allParams = SELECT_ALL)
                .where(whereArgs = "$CURRENT_TRACK = $trackId")
                .select(db = App.INSTANCE.myDataBase)
            if (cursor.moveToFirst()) {
                val latIndex = cursor.getColumnIndexOrThrow(LATITUDE)
                val lonIndex = cursor.getColumnIndexOrThrow(LONGITUDE)
                do {
                    val lat = cursor.getString(latIndex)
                    val lon = cursor.getString(lonIndex)
                    listOfTracks.add(PointForData(lng = lon.toDouble(), lat = lat.toDouble()))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return listOfTracks
    }

    private fun getListOfTracks(): List<TrackFromDb> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<TrackFromDb>()
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = TRACKERS)
                .selectParams(allParams = SELECT_ALL)
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

    private fun getListNotSendTracksFromDb(): List<TrackFromDb> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<TrackFromDb>()
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = TRACKERS)
                .selectParams(allParams = SELECT_ALL)
                .where(whereArgs = "$IS_SEND = $NOT_SEND")
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
        id = cursor.getColumnIndexOrThrow(ID),
        serverId = cursor.getColumnIndexOrThrow(ID_FROM_SERVER),
        beginAt = cursor.getColumnIndexOrThrow(BEGIN_TIME),
        time = cursor.getColumnIndexOrThrow(RUNNING_TIME),
        distance = cursor.getColumnIndexOrThrow(DISTANCE)
    )

    private fun insertDataInDb(tracksInfo: List<TrackForData>) {
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
                .insertTheValues(db = App.INSTANCE.myDataBase)
        }
    }

    private fun clearTable() {
        UpdateIntoDbHelper()
            .setName(tableName = TRACKERS)
            .delete(db = App.INSTANCE.myDataBase)
    }
}
