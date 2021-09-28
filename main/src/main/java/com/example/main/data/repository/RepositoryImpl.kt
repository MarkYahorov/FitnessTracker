package com.example.main.data.repository

import android.content.Context
import com.example.core.database.FitnessDatabase
import com.example.core.models.notification.Notification
import com.example.core.models.points.PointForData
import com.example.core.models.points.PointsRequest
import com.example.core.models.save.SaveTrackRequest
import com.example.core.models.save.SaveTrackResponse
import com.example.core.models.tracks.TrackFromDb
import com.example.core.models.tracks.TrackRequest
import com.example.core.retrofit.ApiService
import com.example.main.data.dataSource.DbSource
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class RepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dbSource: DbSource
) : Repository {

    companion object {
        private const val WAS_SEND = 0
    }

    override fun getTracks(
        isFirstTimeOrRefresh: Boolean,
        token: String
    ): Observable<List<TrackFromDb>> {
        return if (isFirstTimeOrRefresh) {
            sendNotSaveTracks(token).andThen(fetchTracksFromServer(token))
                .andThen(Observable.fromCallable { dbSource.getTracks() })
        } else {
            Observable.fromCallable { dbSource.getTracks() }
                .subscribeOn(Schedulers.io())
        }
    }

    override fun getPointsForCurrentTrack(
        idInDb: Int,
        serverId: Int,
        pointsRequest: PointsRequest?
    ): Single<List<PointForData>> {
        return if (!dbSource.checkThisPointIntoDb(idInDb)) {
            apiService.getPointsForCurrentTrack(pointsRequest)
                .subscribeOn(Schedulers.io())
                .map {
                    it.pointForData
                }
                .doOnSuccess {
                    dbSource.savePoints(
                        trackIdInDb = idInDb,
                        listOfPoints = it
                    )
                }
        } else {
            Single.fromCallable {
                dbSource.getPointsForCurrentTrack(idInDb)
            }.subscribeOn(Schedulers.io())
        }
    }

    override fun getNotifications(): Observable<List<Notification>> {
        return Observable.fromCallable {
            dbSource.getListOfNotifications()
        }.subscribeOn(Schedulers.io())
    }

    override fun saveNotifications(
        alarmDate: Long,
        alarmHours: Int,
        alarmMinutes: Int,
        listOfNotifications: List<Notification>
    ): Observable<Int> {
        return Observable.fromCallable {
            dbSource.saveNotifications(alarmDate, alarmHours, alarmMinutes, listOfNotifications)
        }.subscribeOn(Schedulers.io())
    }

    override fun updateNotifications(
        updateValue: Long,
        hours: Int,
        minutes: Int,
        id: Int
    ): Observable<Unit> {
        return Observable.fromCallable {
            dbSource.updateNotifications(updateValue, hours, minutes, id)
        }.subscribeOn(Schedulers.io())
    }

    override fun clearDbWithWhereArgs(tableName: String, whereArgs: String): Observable<Unit> {
        return Observable.fromCallable { dbSource.clearDbWithWhereArgs(tableName, whereArgs) }
            .subscribeOn(Schedulers.io())
    }

    override fun clearDb(context: Context): Observable<Unit> {
        return Observable.fromCallable { dbSource.clearDb(context) }
            .subscribeOn(Schedulers.io())
    }

    private fun sendNotSaveTracks(token: String): Completable {
        return Single.fromCallable { dbSource.getListNotSendTracks() }
            .observeOn(Schedulers.io())
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMap { trackForData ->
                saveNotSendTracks(token, trackForData)
                    .doOnNext { saveResponse ->
                        updateTracksInDb(saveResponse, trackForData.id)
                    }
            }
            .ignoreElements()
    }

    private fun saveNotSendTracks(
        token: String,
        trackForData: TrackFromDb
    ): Observable<SaveTrackResponse> {
        return apiService.saveTrack(
            savePointsRequest = SaveTrackRequest(
                token = token,
                serverId = trackForData.serverId,
                beginTime = trackForData.beginTime,
                time = trackForData.time,
                distance = trackForData.distance,
                pointForData = dbSource.getPointsForCurrentTrack(
                    trackForData.id
                )
            )
        )
    }

    private fun fetchTracksFromServer(token: String): Completable {
        return apiService.getTracks(TrackRequest(token))
            .observeOn(Schedulers.io())
            .map { response -> response.trackForData }
            .doOnSuccess { items -> dbSource.saveTracks(items) }
            .ignoreElement()
    }

    private fun updateTracksInDb(saveResponse: SaveTrackResponse, trackId: Int) {
        val id = saveResponse.serverId
        if (id != null) {
            dbSource.updateField(
                tableName = FitnessDatabase.TRACKERS,
                fieldName = FitnessDatabase.ID_FROM_SERVER,
                value = id,
                whereArgs = "${FitnessDatabase.ID} = $trackId"
            )
            dbSource.updateField(
                tableName = FitnessDatabase.ALL_POINTS,
                fieldName = FitnessDatabase.ID_FROM_SERVER,
                value = id,
                whereArgs = "${FitnessDatabase.CURRENT_TRACK} = $trackId"
            )
            dbSource.updateField(
                tableName = FitnessDatabase.TRACKERS,
                fieldName = FitnessDatabase.IS_SEND,
                value = WAS_SEND,
                whereArgs = "${FitnessDatabase.ID} = $trackId"
            )
        }
    }
}