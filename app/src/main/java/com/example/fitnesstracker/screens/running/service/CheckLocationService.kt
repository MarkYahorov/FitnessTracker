package com.example.fitnesstracker.screens.running.service

import android.annotation.SuppressLint
import android.app.*
import android.app.Notification.CATEGORY_SERVICE
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.ALL_COORDINATES
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.DISTANCE_FROM_SERVICE
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.IS_START
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.LOCATION_UPDATE
import java.util.*


class CheckLocationService : Service(), LocationListener {

    companion object {
        private const val EXAMPLE_SERVICE_CHANNEL_ID = "exampleServiceChanel"
        private const val EXAMPLE_SERVICE_CHANNEL_NAME = "example Service Chanel"
        private const val MIN_TIME_MS = 3000L
        private const val MIN_DISTANCE_M = 5F
    }

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var oldLatitude: Double? = null
    private var oldLongitude: Double? = null
    private val listOfPoints = mutableListOf<PointForData>()
    private val distanceList = FloatArray(1)
    private val allDistanceList = mutableListOf<Float>()
    private var locationManager: LocationManager? = null

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        createNotifyChanel()
        startForeground()
        locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun startForeground() {
        val pendingIntent: PendingIntent =
            Intent(this, CheckLocationService::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        val notification = createNotification(pendingIntent = pendingIntent)
        startForeground(1, notification)
    }

    @SuppressLint("InlinedApi")
    private fun createNotification(pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(this, EXAMPLE_SERVICE_CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle(getText(R.string.check_gps))
            .setSmallIcon(R.drawable.ic_baseline_run_circle_24)
            .setContentIntent(pendingIntent)
            .setPriority(
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> IMPORTANCE_HIGH
                    else -> PRIORITY_HIGH
                }
            )
            .setCategory(CATEGORY_SERVICE)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.extras?.get(IS_START) == true) {
            locationManager?.requestLocationUpdates(GPS_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_M, this)
        } else {
            val endIntent = Intent(LOCATION_UPDATE)
                .putExtra(ALL_COORDINATES, listOfPoints as ArrayList<PointForData>)
                .putExtra(DISTANCE_FROM_SERVICE, allDistanceList.toFloatArray())
            locationManager = null
            sendBroadcast(endIntent)
            stopForeground(true)
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun createNotifyChanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notChan = NotificationChannel(
                EXAMPLE_SERVICE_CHANNEL_ID,
                EXAMPLE_SERVICE_CHANNEL_NAME,
                IMPORTANCE_HIGH
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notChan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            manager.createNotificationChannel(notChan)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onLocationChanged(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude
        listOfPoints.add(PointForData(currentLongitude, currentLatitude))
        if (oldLatitude == null || oldLongitude == null) {
            oldLatitude = location.latitude
            oldLongitude = location.longitude
        }
        calculateDistance()
        allDistanceList.add(distanceList[0])
        oldLatitude = currentLatitude
        oldLongitude = currentLongitude
    }

    private fun calculateDistance() {
        Location.distanceBetween(
            currentLatitude,
            currentLongitude,
            oldLatitude!!,
            oldLongitude!!,
            distanceList
        )
    }
}