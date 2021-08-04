package com.example.fitnesstracker.screens.running

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.CheckLocationService
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_TRACK
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ID
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.IS_SEND
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectDbHelper
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.save.SaveTrackRequest
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.example.fitnesstracker.screens.main.IS_FROM_NOTIFICATION
import com.example.fitnesstracker.screens.main.MainActivity
import java.text.SimpleDateFormat
import java.util.*

class RunningActivity : AppCompatActivity() {

    private lateinit var startBtn: Button
    private lateinit var finishBtn: Button
    private lateinit var timeRunning: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var finishTimeRunning: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var navDrawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private var builder: AlertDialog.Builder? = null
    private var handler: Handler? = null
    private val coordinationList = mutableListOf<PointForData>()
    private val repo = App.INSTANCE.repositoryImpl
    private var calendar = Calendar.getInstance()
    private val timeZone = SimpleTimeZone.getTimeZone("UTC")
    private var isFinish = true
    private var distance = 0
    private var beginTime = 0L
    private var endTime = 0L
    private var trackIdInDb = 0

    private var tMilliSec = 0L
    private var tStart = 0L
    private var tBuff = 0L
    private var tUpdate = 0L
    private var sec = 0
    private var min = 0
    private var millis = 0
    private var hours = 0

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val distanceFromBroadcast = intent?.getFloatArrayExtra("distance")
            distance = distanceFromBroadcast?.sum()!!.toInt()
            distanceTextView.text = distanceFromBroadcast.sum().toLong().toString()
            coordinationList.addAll(
                intent.getParcelableArrayListExtra<PointForData>("allCoordinates")!!
                    .toMutableList()
            )

            if (coordinationList.size > 1) {
                repo.saveTrack(createSaveTrackRequest())
                    .continueWith({ saveTrackResponse ->
                        when {
                            saveTrackResponse.error != null -> {
                                insertTheTrack(null, 1)
                                val id = getLastTrackInDb()
                                insertThePoints(null, id!!)
                                createAlertDialog("Lost Internet Connection")
                            }
                            saveTrackResponse.result.status == "error" -> {
                                insertTheTrack(null, 1)
                                val id = getLastTrackInDb()
                                insertThePoints(null, id!!)
                                createAlertDialog(saveTrackResponse.result.error)
                            }
                            else -> {
                                insertTheTrack(saveTrackResponse.result.serverId, 0)
                                val id = getLastTrackInDb()
                                insertThePoints(saveTrackResponse.result.serverId, id!!)
                            }
                        }
                    }, Task.UI_THREAD_EXECUTOR)
                Log.e("key", "${coordinationList.size}")
            } else {
                createAlertDialog("YOU DON'T MOVING")
            }
        }
    }

    private fun getLastTrackInDb(): Int? {
        var cursor: Cursor? = null
        var id: Int? = null
        try {
            cursor = SelectDbHelper()
                .nameOfTable("trackers")
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

    private val timer = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            tMilliSec = SystemClock.elapsedRealtime() - tStart
            tUpdate = tBuff + tMilliSec
            sec = (tUpdate / 1000).toInt()
            min = sec / 60
            hours = sec / 3600
            sec %= 60
            millis = (tUpdate % 100).toInt()
            calendar[Calendar.HOUR_OF_DAY] = hours
            calendar[Calendar.MINUTE] = min
            calendar[Calendar.SECOND] = sec
            calendar[Calendar.MILLISECOND] = millis
            timeRunning.text = "${String.format("%02d", hours)}: ${
                String.format(
                    "%02d",
                    min
                )
            }: ${String.format("%02d", sec)}: ${String.format("%02d", millis)}"
            endTime = SystemClock.elapsedRealtime()
            handler?.postDelayed(this, 40)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        checkPermissions()
        initAll()
        trackIdInDb = intent.getIntExtra("track_id", 0)
        if (savedInstanceState != null) {
            beginTime = savedInstanceState.getLong("BEGIN_TIME")
            isFinish = savedInstanceState.getBoolean("IS_FINISH")
            distanceTextView.text = savedInstanceState.getString("DISTANCE")
            finishTimeRunning.text = savedInstanceState.getString("FINISH_TIME")
            startBtn.isVisible = savedInstanceState.getBoolean("BOOL")
            timeRunning.isVisible = savedInstanceState.getBoolean("TR")
            finishBtn.isVisible = savedInstanceState.getBoolean("FB")
            distanceTextView.isVisible = savedInstanceState.getBoolean("DTV")
            finishTimeRunning.isVisible = savedInstanceState.getBoolean("FTV")
            tStart = savedInstanceState.getLong("start")
            handler?.postDelayed(timer, 0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    private fun setButtonsClickListeners() {
        calendar.timeZone = timeZone
        startBtn.setOnClickListener {
            if (!checkPermissions() && isGpsEnabled()) {
                val intent = Intent(this, CheckLocationService::class.java)
                    .putExtra("Bool", true)
                getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
                    .edit()
                    .putInt("CURRENT_ACTIVITY", 1)
                    .apply()
                isFinish = false
                beginTime = System.currentTimeMillis()
                startService(intent)
                finishBtn.isVisible = true
                startBtn.isVisible = false
                timeRunning.isVisible = true
                tStart = SystemClock.elapsedRealtime()
                handler?.postDelayed(timer, 0)
                setIsFromNotificationInSharedPref()
            } else if (!isGpsEnabled()){
                createAlertDialog("ENABLE GPS")
            } else {
                createAlertDialog("ENABLE PERMISSIONS")
            }
        }
        finishBtn.setOnClickListener {
            if (isGpsEnabled()) {
                isFinish = true
                val intent = Intent(this, CheckLocationService::class.java)
                    .putExtra("Bool", false)
                startService(intent)
                finishBtn.isVisible = false
                distanceTextView.isVisible = true
                timeRunning.isVisible = false
                finishTimeRunning.isVisible = true
                handler?.removeCallbacks(timer)
                val format = SimpleDateFormat("HH:mm:ss,SS", Locale.getDefault())
                format.timeZone = timeZone
                finishTimeRunning.text = format.format(calendar.time.time)
                getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
                    .edit()
                    .putInt("CURRENT_ACTIVITY", 0)
                    .apply()
                Log.e("key", "${calendar.time.time}")
            } else {
                createAlertDialog("ENABLE GPS")
            }
        }
    }

    private fun setToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun createDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            navDrawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        toggle.setToolbarNavigationClickListener {
            onBackPressed()
        }
        navDrawer.addDrawerListener(toggle)
    }

    private fun setIsFromNotificationInSharedPref() {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(IS_FROM_NOTIFICATION, false)
            .apply()
    }

    private fun insertTheTrack(id: Int?, isSend: Int) {
        InsertDBHelper()
            .setTableName("trackers")
            .addFieldsAndValuesToInsert(FitnessDatabase.ID_FROM_SERVER, id.toString())
            .addFieldsAndValuesToInsert(FitnessDatabase.BEGIN_TIME, beginTime.toString())
            .addFieldsAndValuesToInsert(
                FitnessDatabase.RUNNING_TIME,
                calendar.time.time.toString()
            )
            .addFieldsAndValuesToInsert(IS_SEND, isSend.toString())
            .addFieldsAndValuesToInsert(FitnessDatabase.DISTANCE, distance.toString())
            .insertTheValues(App.INSTANCE.db)
    }

    private fun insertThePoints(id: Int?, trackIdInDb: Int) {
        coordinationList.forEach {
            InsertDBHelper()
                .setTableName("allPoints")
                .addFieldsAndValuesToInsert(
                    FitnessDatabase.ID_FROM_SERVER,
                    id.toString()
                )
                .addFieldsAndValuesToInsert(
                    FitnessDatabase.LATITUDE,
                    it.lat.toString()
                )
                .addFieldsAndValuesToInsert(CURRENT_TRACK, trackIdInDb.toString())
                .addFieldsAndValuesToInsert(
                    FitnessDatabase.LONGITUDE,
                    it.lng.toString()
                )
                .insertTheValues(App.INSTANCE.db)
        }
    }

    private fun getTokenFromSharedPref(): String {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, "").toString()
    }

    private fun createSaveTrackRequest() = SaveTrackRequest(
        token = getTokenFromSharedPref(),
        serverId = null,
        beginTime = beginTime,
        time = calendar.time.time,
        distance = distance,
        pointForData = coordinationList
    )

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        setButtonsClickListeners()
        setToolbar()
        createDrawer()
        toggle.isDrawerIndicatorEnabled = false
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            true
        } else {
            createAlertDialog("GPS IS NOT ENABLED")
            false
        }
    }

    override fun onResume() {
        super.onResume()

        registerReceiver(broadcastReceiver, IntentFilter("location_update"))
    }

    private fun initAll() {
        distanceTextView = findViewById(R.id.distance_running)
        startBtn = findViewById(R.id.start_btn)
        finishBtn = findViewById(R.id.finish_btn)
        timeRunning = findViewById(R.id.time_text)
        finishTimeRunning = findViewById(R.id.finish_trunning_time)
        toolbar = findViewById(R.id.running_toolbar)
        navDrawer = findViewById(R.id.running_drawer)
        builder = AlertDialog.Builder(this)
        handler = Handler()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("IS_FINISH", isFinish)
        outState.putLong("BEGIN_TIME", beginTime)
        outState.putBoolean("BOOL", startBtn.isVisible)
        outState.putBoolean("TR", timeRunning.isVisible)
        outState.putBoolean("FB", finishBtn.isVisible)
        outState.putBoolean("DTV", distanceTextView.isVisible)
        outState.putBoolean("FTV", finishTimeRunning.isVisible)
        outState.putLong("start", tStart)
        outState.putString("DISTANCE", distanceTextView.text.toString())
        outState.putString("FINISH_TIME", finishTimeRunning.text.toString())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            true
        } else {
            false
        }
    }

    private fun createAlertDialog(error: String?) {
        builder?.setPositiveButton("Ok, thanks") { _, _ ->
        }
        builder?.setTitle("ERROR")
        builder?.setMessage(error)
        builder?.setIcon(R.drawable.ic_baseline_error_outline_24)
        builder?.show()
    }

    override fun onBackPressed() {
        if (isFinish) {
            if (intent.extras?.get(IS_FROM_NOTIFICATION) == false) {
                finish()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else {
            createAlertDialog("Press the finish button before exiting")
            return
        }
        super.onBackPressed()
    }
}