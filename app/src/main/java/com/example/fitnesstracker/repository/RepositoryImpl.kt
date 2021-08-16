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
import com.example.fitnesstracker.data.database.helpers.InsertIntoDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectFromDbHelper
import com.example.fitnesstracker.data.database.helpers.UpdateIntoDbHelper
import com.example.fitnesstracker.data.retrofit.RetrofitBuilder
import com.example.fitnesstracker.models.columnIndex.NotificationColumnIndexFromDb
import com.example.fitnesstracker.models.columnIndex.TrackColumnIndexFromDb
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
import com.example.fitnesstracker.models.tracks.TrackFromDb
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
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

    override fun getTracks(trackRequest: TrackRequest?): Task<TrackResponse> {
        return Task.callInBackground {
            val execute = RetrofitBuilder().apiService.getTracks(trackRequest = trackRequest)
            val body = execute.execute().body()
            val list = getListOfTracks()
            if (body != null && body.trackForData.size > list.size) {
                insertDataInDb(tracksInfo = body.trackForData)
            }
            if ((body?.error == null || body.status != ERROR) && getListNotSendTracksFromDb().isNotEmpty()) {
                val trackForData = TrackForData(0, 0, 0, 0)
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
                        getPointsForCurrentTrackFromDb(trackId = idInDb).forEach { listFromDb ->
                            list.add(PointForData(listFromDb.lng, listFromDb.lat))
                        }
                    }
                    it.result.status == ERROR -> {
                        getPointsForCurrentTrackFromDb(trackId = idInDb).forEach { listFromDb ->
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

    override fun saveTrack(saveTrackRequest: SaveTrackRequest): Task<SaveTrackResponse> {
        return Task.callInBackground {
            val execute =
                RetrofitBuilder().apiService.saveTrack(savePointsRequest = saveTrackRequest)
            execute.clone().execute().body()
        }
    }

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

    private fun getPointsForCurrentTrackFromDb(trackId: Int): List<PointForData> {
        return getListOfPointsToCurrentTrack(trackId = trackId)
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
            getLastPositionInDb(NOTIFICATION_TIME_NAME)
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
                .setName(tableName = NOTIFICATION_TIME_NAME)
                .updatesValues(nameOfField = NOTIFICATION_TIME, updateValue = updateValue)
                .updatesValues(nameOfField = CURRENT_HOUR, updateValue = hours)
                .updatesValues(nameOfField = CURRENT_MINUTE, updateValue = minutes)
                .where(whereArgs = "$ID = $id")
                .update(db = App.INSTANCE.myDataBase)
        }
    }

    override fun clearDb(context: Context): Task<Unit> {
        return Task.callInBackground {
            setSharedPrefToDefaultValues(context = context)
            App.INSTANCE.myDataBase.execSQL("DROP TABLE $TRACKERS")
            App.INSTANCE.myDataBase.execSQL("DROP TABLE $ALL_POINTS")
            App.INSTANCE.myDataBase.execSQL("DROP TABLE $NOTIFICATION_TIME_NAME")
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
                saveTrackResponse.result.status == ERROR -> {
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

    private fun getPointsFromServer(pointsRequest: PointsRequest?): Task<PointsResponse> {
        return Task.callInBackground {
            val execute =
                RetrofitBuilder().apiService.getPointsForCurrentTrack(pointsRequest = pointsRequest)
            execute.clone().execute().body()
        }
    }

    private fun checkThisPointIntoDb(currentTrackId: Int): Boolean {
        var cursor: Cursor? = null
        val haveData: Boolean
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = ALL_POINTS)
                .selectParams(allParams = "*")
                .where(whereArgs = "$CURRENT_TRACK = $currentTrackId")
                .select(db = App.INSTANCE.myDataBase)
            haveData = cursor.moveToFirst()
        } finally {
            cursor?.close()
        }
        return haveData
    }

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
        val trackIdInDb = getLastPositionInDb(TRACKERS)
        insertThePoints(
            serverId = id,
            trackIdInDb = trackIdInDb!!,
            listOfPoints = listOfPoints
        )
    }

    private fun getLastPositionInDb(table: String): Int? {
        var cursor: Cursor? = null
        var id: Int? = null
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = table)
                .selectParams(allParams = MAX)
                .select(db = App.INSTANCE.myDataBase)
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
        InsertIntoDBHelper()
            .setTableName(name = TRACKERS)
            .addFieldsAndValuesToInsert(
                nameOfField = ID_FROM_SERVER,
                insertingValue = id.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = BEGIN_TIME,
                insertingValue = beginTime.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = RUNNING_TIME,
                insertingValue = calendar.time.time.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = IS_SEND,
                insertingValue = isSend.toString()
            )
            .addFieldsAndValuesToInsert(
                nameOfField = DISTANCE,
                insertingValue = distance.toString()
            )
            .insertTheValues(db = App.INSTANCE.myDataBase)
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

    private fun insertNotificationInDb(
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
            .insertTheValues(db = App.INSTANCE.myDataBase)
    }

    private fun saveTracksWithNullId(
        trackRequest: TrackRequest?,
        trackForData: TrackForData,
        trackFromDb: TrackFromDb
    ) {
        if (trackForData.serverId == 0) {
            trackForData.serverId = null
            saveTrack(
                saveTrackRequest = SaveTrackRequest(
                    token = trackRequest?.token ?: "",
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


    private fun getListOfNotificationFromDb(): List<Notification> {
        var cursor: Cursor? = null
        val listOfNotification = mutableListOf<Notification>()
        try {
            cursor = SelectFromDbHelper()
                .selectParams(allParams = "*")
                .nameOfTable(table = NOTIFICATION_TIME_NAME)
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

    private fun getListOfPointsToCurrentTrack(trackId: Int): List<PointForData> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<PointForData>()
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = ALL_POINTS)
                .selectParams(allParams = "*")
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

    private fun getListNotSendTracksFromDb(): List<TrackFromDb> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<TrackFromDb>()
        try {
            cursor = SelectFromDbHelper()
                .nameOfTable(table = TRACKERS)
                .selectParams(allParams = "*")
                .where(whereArgs = "$IS_SEND = 1")
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
