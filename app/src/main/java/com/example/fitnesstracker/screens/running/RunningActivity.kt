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
import android.view.animation.AnimationUtils
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
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ALL_POINTS
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_TRACK
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.ID
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.IS_SEND
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.TRACKERS
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.data.database.helpers.SelectDbHelper
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.save.SaveTrackRequest
import com.example.fitnesstracker.models.save.SaveTrackResponse
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.example.fitnesstracker.screens.main.IS_FROM_NOTIFICATION
import com.example.fitnesstracker.screens.main.MainActivity
import java.text.SimpleDateFormat
import java.util.*


class RunningActivity : AppCompatActivity() {

    companion object {
        const val DISTANCE_FROM_SERVICE = "distance"
        const val ALL_COORDINATES = "allCoordinates"
        const val LOCATION_UPDATE = "location_update"
        const val PATTERN = "HH:mm:ss,SS"
        const val IS_START = "Bool"
        const val CURRENT_ACTIVITY = "CURRENT_ACTIVITY"
        const val UTC = "UTC"
        const val TRACK_ID = "track_id"
        private const val BEGIN_TIME = "BEGIN_TIME"
        private const val IS_FINISH = "IS_FINISH"
        private const val DISTANCE = "DISTANCE"
        private const val BOOL = "BOOL"
        private const val TR = "TR"
        private const val FB = "FB"
        private const val DTV = "DTV"
        private const val FTV = "FTV"
        private const val START = "start"
        private const val FINISH_TIME = "FINISH_TIME"
        private const val ERROR = "error"
        private const val FORMAT = "%02d"
    }

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
    private val timeZone = SimpleTimeZone.getTimeZone(UTC)
    private var isFinish = true
    private var distance = 0
    private var beginTime = 0L
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
            val distanceFromBroadcast = intent?.getFloatArrayExtra(DISTANCE_FROM_SERVICE)
            distance = distanceFromBroadcast?.sum()!!.toInt()
            distanceTextView.text = distanceFromBroadcast.sum().toLong().toString()
            coordinationList.addAll(
                intent.getParcelableArrayListExtra<PointForData>(ALL_COORDINATES)!!
                    .toMutableList()
            )
            if (coordinationList.size > 1) {
                repo.saveTrack(createSaveTrackRequest())
                    .continueWith({ saveTrackResponse ->
                        insertValuesInDb(saveTrackResponse)
                        saveTrackResponse
                    }, Task.BACKGROUND_EXECUTOR)
                    .continueWith {
                        showDialogs(it)
                    }
            } else {
                createAlertDialog(R.string.not_moving)
            }
        }

    }

    private val timer = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            calculateTime()
            setCalendarTimeForTimer()
            timeRunning.text = "${String.format(FORMAT, hours)}: ${
                String.format(
                    FORMAT,
                    min
                )
            }: ${String.format(FORMAT, sec)}: ${String.format(FORMAT, millis)}"
            handler?.postDelayed(this, 40)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        checkPermissions()
        initAll()
        trackIdInDb = intent.getIntExtra(TRACK_ID, 0)
        calendar.timeZone = timeZone
        if (savedInstanceState != null) {
            beginTime = savedInstanceState.getLong(BEGIN_TIME)
            isFinish = savedInstanceState.getBoolean(IS_FINISH)
            distanceTextView.text = savedInstanceState.getString(DISTANCE)
            finishTimeRunning.text = savedInstanceState.getString(FINISH_TIME)
            startBtn.isVisible = savedInstanceState.getBoolean(BOOL)
            timeRunning.isVisible = savedInstanceState.getBoolean(TR)
            finishBtn.isVisible = savedInstanceState.getBoolean(FB)
            distanceTextView.isVisible = savedInstanceState.getBoolean(DTV)
            finishTimeRunning.isVisible = savedInstanceState.getBoolean(FTV)
            tStart = savedInstanceState.getLong(START)
            handler?.postDelayed(timer, 0)
        }
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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        setStartBtnClickListeners()
        setFinishBtnListener()
        setToolbar()
        createDrawer()
        toggle.isDrawerIndicatorEnabled = false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setStartBtnClickListeners() {
        startBtn.setOnClickListener {
            if (!checkPermissions() && isGpsEnabled()) {
                startBtn.isEnabled = false
                isFinish = false
                val anim = AnimationUtils.loadAnimation(this, R.anim.flip_close)
                startBtn.animation = anim
                setAnimationForRunningViews(R.anim.flip_open)
                getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(CURRENT_ACTIVITY, 1)
                    .apply()
                startService(true)
                setVisibilityClickStartBtn()
                beginTime = System.currentTimeMillis()
                tStart = SystemClock.elapsedRealtime()
                handler?.postDelayed(timer, 0)
                setIsFromNotificationInSharedPref()
            } else if (checkPermissions()) {
                createAlertDialog(R.string.permissions_enabled)
            }
        }
    }

    private fun setFinishBtnListener() {
        finishBtn.setOnClickListener {
            if (isGpsEnabled()) {
                finishBtn.isEnabled = false
                isFinish = true
                setAnimationForRunningViews(R.anim.flip_close)
                setAnimationForEndViews(R.anim.flip_open)
                startService(false)
                setVisibilityFinishBtnClick()
                handler?.removeCallbacks(timer)
                val format = SimpleDateFormat(PATTERN, Locale.getDefault())
                format.timeZone = timeZone
                finishTimeRunning.text = format.format(calendar.time.time)
                getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(CURRENT_ACTIVITY, 0)
                    .apply()
            }
        }
    }

    private fun setVisibilityFinishBtnClick() {
        finishBtn.isVisible = false
        distanceTextView.isVisible = true
        timeRunning.isVisible = false
        finishTimeRunning.isVisible = true
    }

    private fun setToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun startService(value: Boolean) {
        val intent = Intent(this, CheckLocationService::class.java)
            .putExtra(IS_START, value)
        startService(intent)
    }

    private fun setVisibilityClickStartBtn() {
        finishBtn.isVisible = true
        startBtn.isVisible = false
        timeRunning.isVisible = true
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
            .setTableName(TRACKERS)
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
                .setTableName(ALL_POINTS)
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

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            true
        } else {
            createAlertDialog(R.string.gps_enabled)
            false
        }
    }

    private fun showDialogs(message: Task<Task<SaveTrackResponse>>) {
        when {
            message.error != null -> {
                createAlertDialog(R.string.not_internet)
            }
            message.result.result.status == ERROR -> {
                createAlertDialog(message.result.result.error!!)
            }
            else -> {
                createAlertDialog(getString(R.string.successfully))
            }
        }
    }

    private fun insertValuesInDb(saveTrackResponse: Task<SaveTrackResponse>) {
        when {
            saveTrackResponse.error != null -> {
                insertTheTrack(null, 1)
                val id = getLastTrackInDb()
                insertThePoints(null, id!!)
            }
            saveTrackResponse.result.status == ERROR -> {
                insertTheTrack(null, 1)
                val id = getLastTrackInDb()
                insertThePoints(null, id!!)

            }
            else -> {
                insertTheTrack(saveTrackResponse.result.serverId, 0)
                val id = getLastTrackInDb()
                insertThePoints(saveTrackResponse.result.serverId, id!!)
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

    private fun setCalendarTimeForTimer() {
        calendar[Calendar.HOUR_OF_DAY] = hours
        calendar[Calendar.MINUTE] = min
        calendar[Calendar.SECOND] = sec
        calendar[Calendar.MILLISECOND] = millis
    }

    private fun calculateTime() {
        tMilliSec = SystemClock.elapsedRealtime() - tStart
        tUpdate = tBuff + tMilliSec
        sec = (tUpdate / 1000).toInt()
        min = sec / 60
        hours = sec / 3600
        sec %= 60
        millis = (tUpdate % 100).toInt()
    }

    private fun setAnimationForRunningViews(anim: Int) {
        val anim2 = AnimationUtils.loadAnimation(this, anim)
        finishBtn.animation = anim2
        timeRunning.animation = anim2
    }

    private fun setAnimationForEndViews(anim: Int) {
        val anim2 = AnimationUtils.loadAnimation(this, anim)
        distanceTextView.animation = anim2
        finishTimeRunning.animation = anim2
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, IntentFilter(LOCATION_UPDATE))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_FINISH, isFinish)
        outState.putLong(BEGIN_TIME, beginTime)
        outState.putBoolean(BOOL, startBtn.isVisible)
        outState.putBoolean(TR, timeRunning.isVisible)
        outState.putBoolean(FB, finishBtn.isVisible)
        outState.putBoolean(DTV, distanceTextView.isVisible)
        outState.putBoolean(FTV, finishTimeRunning.isVisible)
        outState.putLong(START, tStart)
        outState.putString(DISTANCE, distanceTextView.text.toString())
        outState.putString(FINISH_TIME, finishTimeRunning.text.toString())
    }

    private fun createAlertDialog(error: Int) {
        builder?.setPositiveButton(R.string.ok_thanks) { _, _ ->
        }
        builder?.setTitle(R.string.error)
        builder?.setMessage(error)
        builder?.setIcon(R.drawable.ic_baseline_error_outline_24)
        builder?.show()
    }

    private fun createAlertDialog(error: String) {
        builder?.setPositiveButton(R.string.ok_thanks) { _, _ ->
        }
        builder?.setTitle(R.string.error)
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
            createAlertDialog(R.string.press_finish)
            return
        }
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onStop() {
        super.onStop()
        startBtn.setOnClickListener(null)
        finishBtn.setOnClickListener(null)
        navDrawer.removeDrawerListener(toggle)
    }

    override fun onDestroy() {
        super.onDestroy()
        builder = null
        handler = null
    }
}