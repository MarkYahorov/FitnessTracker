package com.example.fitnesstracker

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.example.fitnesstracker.models.points.PointForData
import java.util.*


class CheckLocationService : Service() {

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var oldLatitude: Double? = null
    private var oldLongitude: Double? = null
    private val listOfPoints = mutableListOf<PointForData>()
    private val distanceList = FloatArray(1)
    private val allDistanceList = mutableListOf<Float>()
    private lateinit var locationManager: LocationManager
    private var countOfTap = 0

    private val listener = LocationListener { location ->
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
        Location.distanceBetween(currentLatitude,
            currentLongitude,
            oldLatitude!!,
            oldLongitude!!,
            distanceList)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        createNotifyChanel()
        createNotify()
        locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun createNotify() {
        val pendingIntent: PendingIntent =
            Intent(this, CheckLocationService::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "exampleServiceChanel")
                .setContentTitle(getText(R.string.error_message_repeat_password))
                .setContentText(getText(R.string.error_message_password))
                .setSmallIcon(R.drawable.ic_baseline_error_outline_24)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.error_message_email))
                .build()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.extras?.get("Bool") == true) {
            countOfTap = intent.getIntExtra("countOfTap",1)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 5F, listener)
        } else {
            val endIntent = Intent("location_update")
                .putExtra("allCoordinates", listOfPoints as ArrayList<PointForData>)
                .putExtra("distance", allDistanceList.toFloatArray())
            sendBroadcast(endIntent)
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

//    @RequiresApi(Build.VERSION_CODES.M)
//    private fun checkInternetConnection(context: Context): Boolean {
//        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
//        val netInfo = cm.activeNetworkInfo
//        return netInfo != null && netInfo.isConnected
//    }

    private fun createNotifyChanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notChan = NotificationChannel("exampleServiceChanel",
                "example Service Chanel",
                NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(notChan)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}