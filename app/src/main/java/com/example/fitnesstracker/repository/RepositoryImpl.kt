package com.example.fitnesstracker.repository

import android.database.Cursor
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectDbHelper
import com.example.fitnesstracker.data.retrofit.RetrofitBuilder
import com.example.fitnesstracker.models.ColumnIndexFromDb
import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.points.PointsRequest
import com.example.fitnesstracker.models.points.PointsResponse
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse
import com.example.fitnesstracker.models.save.SaveTrackRequest
import com.example.fitnesstracker.models.save.SaveTrackResponse
import com.example.fitnesstracker.models.tracks.Track
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.TrackResponse

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
            if (body!=null) {
                insertDataInDb(body.tracks)
            }
            body
        }
    }

    override fun getPointsForCurrentTrack(pointsRequest: PointsRequest): Task<PointsResponse> {
        return Task.callInBackground {
            val execute =
                RetrofitBuilder().apiService.getPointsForCurrentTrack(pointsRequest = pointsRequest)
            execute.execute().body()
        }
    }

    override fun saveTrack(saveTrackRequest: SaveTrackRequest): Task<SaveTrackResponse> {
        return Task.callInBackground {
            val execute =
                RetrofitBuilder().apiService.saveTrack(savePointsRequest = saveTrackRequest)
            execute.execute().body()
        }
    }

    override fun getListOfTrack(): Task<List<Track>> {
        return Task.callInBackground {
            getListOfTracks()
        }
    }

    private fun getListOfTracks(): List<Track> {
        var cursor: Cursor? = null
        val listOfTracks = mutableListOf<Track>()
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

    private fun createTrack(cursor: Cursor, index: ColumnIndexFromDb) = Track (
        id = cursor.getInt(index._id),
        beginTime = cursor.getLong(index.beginAt),
        time = cursor.getLong(index.time),
        distance = cursor.getInt(index.distance)
    )

    private fun getColumnIndex(cursor: Cursor) = ColumnIndexFromDb(
        _id = cursor.getColumnIndexOrThrow("_id"),
        beginAt = cursor.getColumnIndexOrThrow("beginAt"),
        time = cursor.getColumnIndexOrThrow("time"),
        distance = cursor.getColumnIndexOrThrow("distance")
    )

    private fun insertDataInDb(tracksInfo:List<Track>){
        clearDb()
        tracksInfo.forEach {
            InsertDBHelper()
                .setTableName("trackers")
                .addFieldsAndValuesToInsert(FitnessDatabase.ID_FROM_SERVER, it.id.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.BEGIN_TIME, it.beginTime.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.RUNNING_TIME, it.time.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.DISTANCE, it.distance.toString())
                .insertTheValues(App.INSTANCE.db)
        }
    }

    private fun clearDb(){
        App.INSTANCE.db.execSQL("DELETE FROM trackers")
    }
}