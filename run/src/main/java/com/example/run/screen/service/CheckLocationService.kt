package com.example.run.screen.service

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
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import com.example.run.R
import com.example.run.screen.GPSListener
import java.util.*


class CheckLocationService : Service(), LocationListener {

    companion object {
        private const val EXAMPLE_SERVICE_CHANNEL_ID = "exampleServiceChanel"
        private const val EXAMPLE_SERVICE_CHANNEL_NAME = "example Service Chanel"
        private const val MIN_TIME_MS = 3000L
        private const val MIN_DISTANCE_M = 5F
        private const val FOREGROUND_ID = 1
        private const val PENDING_INTENT_REQUEST_CODE = 0
        private const val FIRST_ELEMENT_IN_LIST = 0
        private const val SIZE_OF_FLOAT_ARRAY = 1
    }

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var oldLatitude: Double? = null
    private var oldLongitude: Double? = null
    private val listOfPoints = mutableListOf<com.example.core.models.points.PointForData>()
    private val distanceList = FloatArray(SIZE_OF_FLOAT_ARRAY)
    private val allDistanceList = mutableListOf<Float>()
    private var locationManager: LocationManager? = null
    private var binder = MyBinder()
    private var listenerListener: GPSListener? = null

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        if (isGpsEnabled()) {
            createNotifyChanel()
            startForeground()
            locationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
        } else {

        }
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(GPS_PROVIDER)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotifyChanel() {
        val notificationChannel = NotificationChannel(
            EXAMPLE_SERVICE_CHANNEL_ID,
            EXAMPLE_SERVICE_CHANNEL_NAME,
            IMPORTANCE_HIGH
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        manager.createNotificationChannel(notificationChannel)
    }

    private fun startForeground() {
        val pendingIntent: PendingIntent =
            Intent(this, CheckLocationService::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, PENDING_INTENT_REQUEST_CODE, notificationIntent, 0)
            }
        val notification = createNotification(pendingIntent = pendingIntent)
        startForeground(FOREGROUND_ID, notification)
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

    fun getList(): List<com.example.core.models.points.PointForData> {
        return listOfPoints
    }

    fun getDistanceList(): List<Float> {
        return allDistanceList
    }

    @SuppressLint("MissingPermission")
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopForeground(true)
        return super.onUnbind(intent)
    }

    override fun onLocationChanged(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude
        listOfPoints.add(
            com.example.core.models.points.PointForData(
                currentLongitude,
                currentLatitude
            )
        )
        if (oldLatitude == null || oldLongitude == null) {
            oldLatitude = location.latitude
            oldLongitude = location.longitude
        }
        calculateDistance()
        allDistanceList.add(distanceList[FIRST_ELEMENT_IN_LIST])
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

    override fun onDestroy() {
        locationManager?.removeUpdates(this)
        locationManager = null
        super.onDestroy()
    }

    override fun onProviderEnabled(provider: String) {
        super.onProviderEnabled(provider)
    }

    override fun onProviderDisabled(provider: String) {
        listenerListener?.disabledGPS()
        super.onProviderDisabled(provider)
    }

    inner class MyBinder : Binder() {
        fun getService(): CheckLocationService {
            return this@CheckLocationService
        }

        fun setListener(listenerListener: GPSListener) {
            this@CheckLocationService.listenerListener = listenerListener
        }

        @SuppressLint("MissingPermission")
        fun start(): Boolean {
            return if(isGpsEnabled()){
                locationManager?.requestLocationUpdates(GPS_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_M, this@CheckLocationService)
                true
            } else {
                listenerListener?.disabledGPS()
                false
            }
        }
    }
}