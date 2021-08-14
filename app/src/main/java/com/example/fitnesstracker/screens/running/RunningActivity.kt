package com.example.fitnesstracker.screens.running

import android.Manifest
import android.animation.AnimatorInflater
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.App.Companion.MAIN_ACTIVITY_MARKER
import com.example.fitnesstracker.App.Companion.PATTERN_WITH_SECONDS
import com.example.fitnesstracker.App.Companion.RUNNING_ACTIVITY_MARKER
import com.example.fitnesstracker.App.Companion.UTC
import com.example.fitnesstracker.screens.running.service.CheckLocationService
import com.example.fitnesstracker.R
import com.example.fitnesstracker.screens.running.calculate.TimeCalculator
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
        const val IS_START = "Bool"
        const val CURRENT_ACTIVITY = "CURRENT_ACTIVITY"
        const val TRACK_ID = "track_id"
        const val ERROR = "error"
        private const val BEGIN_TIME = "BEGIN_TIME"
        private const val IS_FINISH = "IS_FINISH"
        private const val DISTANCE = "DISTANCE"
        private const val RUNNING_VISIBLE = "RUNNING_VISIBLE"
        private const val FINISH_VISIBLE = "FINISH_VISIBLE"
        private const val START = "start"
        private const val FINISH_TIME = "FINISH_TIME"
        private const val EMPTY_VALUE = ""
        private const val DEFAULT_REQUEST_CODE = 100
        private const val HANDLER_DELAY = 0L
    }

    private lateinit var startBtn: Button
    private lateinit var finishBtn: Button
    private lateinit var timeRunningTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var finishTimeRunning: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var navDrawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var startBtnLayout: ConstraintLayout
    private lateinit var runningLayout: ConstraintLayout
    private lateinit var finishLayout: ConstraintLayout

    private var alertDialog: AlertDialog.Builder? = null
    private var handler: Handler? = null
    private var timer: Runnable? = null
    private val coordinateList = mutableListOf<PointForData>()
    private val repo = App.INSTANCE.repositoryImpl
    private var calendar = Calendar.getInstance()
    private val timeZone = SimpleTimeZone.getTimeZone(UTC)
    private var isFinish = true
    private var distance = 0
    private var beginTime = 0L

    private var tStart = 0L

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val distanceFromBroadcast = intent?.getFloatArrayExtra(DISTANCE_FROM_SERVICE)
            distance = distanceFromBroadcast?.sum()!!.toInt()
            distanceTextView.text = distanceFromBroadcast.sum().toLong().toString()
            coordinateList.addAll(
                intent.getParcelableArrayListExtra<PointForData>(ALL_COORDINATES)!!
                    .toMutableList()
            )
            if (coordinateList.size > 1) {
                repo.saveTrack(saveTrackRequest = createSaveTrackRequest())
                    .continueWith({ saveTrackResponse ->
                        repo.insertTrackAndPointsInDbAfterSavingInServer(
                            saveTrackResponse = saveTrackResponse,
                            beginTime = beginTime,
                            calendar = calendar,
                            distance = distance,
                            listOfPoints = coordinateList
                        ).continueWith({
                            showDialogs(message = saveTrackResponse)
                        }, Task.UI_THREAD_EXECUTOR)
                    }, Task.UI_THREAD_EXECUTOR)
            } else {
                createAlertDialog(message = R.string.not_moving)
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        checkPermissions()
        initAll()
        calendar.timeZone = timeZone
        if (savedInstanceState != null) {
            beginTime = savedInstanceState.getLong(BEGIN_TIME)
            isFinish = savedInstanceState.getBoolean(IS_FINISH)
            distanceTextView.text = savedInstanceState.getString(DISTANCE)
            finishTimeRunning.text = savedInstanceState.getString(FINISH_TIME)
            runningLayout.isVisible = savedInstanceState.getBoolean(RUNNING_VISIBLE)
            finishLayout.isVisible = savedInstanceState.getBoolean(FINISH_VISIBLE)
            tStart = savedInstanceState.getLong(START)
            timer = TimeCalculator().createTimer(
                view = timeRunningTextView,
                tStart = tStart,
                calendar = calendar
            )
            handler?.postDelayed(timer!!, HANDLER_DELAY)
            checkLayoutsVisibility()
        }
    }

    private fun checkLayoutsVisibility() {
        if (runningLayout.isVisible) {
            startBtnLayout.isVisible = false
        } else if (finishLayout.isVisible) {
            runningLayout.isVisible = false
            startBtnLayout.isVisible = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                DEFAULT_REQUEST_CODE
            )
            true
        } else {
            false
        }
    }

    private fun initAll() {
        distanceTextView = findViewById(R.id.distance_running)
        startBtn = findViewById(R.id.start_btn)
        finishBtn = findViewById(R.id.finish_btn)
        timeRunningTextView = findViewById(R.id.time_text)
        finishTimeRunning = findViewById(R.id.finish_trunning_time)
        toolbar = findViewById(R.id.running_toolbar)
        navDrawer = findViewById(R.id.running_drawer)
        startBtnLayout = findViewById(R.id.start_btn_layout)
        runningLayout = findViewById(R.id.running_layout)
        finishLayout = findViewById(R.id.finish_running_layout)
        alertDialog = AlertDialog.Builder(this)
        handler = Handler(Looper.getMainLooper())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        setStartBtnClickListener()
        setFinishBtnListener()
        setToolbar()
        createDrawer()
        toggle.isDrawerIndicatorEnabled = false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setStartBtnClickListener() {
        startBtn.setOnClickListener {
            if (!checkPermissions() && isGpsEnabled()) {
                startBtn.isEnabled = false
                isFinish = false
                startTimer()
                startAnimation(startBtnLayout, R.animator.anim_close)
                startAnimation(runningLayout, R.animator.anim_open)
                runningLayout.isVisible = true
                putMarkActivity(mark = RUNNING_ACTIVITY_MARKER)
                startService(value = true)
                setIsFromNotificationInSharedPref()
            } else if (checkPermissions()) {
                createAlertDialog(message = R.string.permissions_enabled)
            }
        }
    }

    private fun startAnimation(view: ConstraintLayout, idAnimatorRes: Int) {
        val flipLestOutAnimator = AnimatorInflater.loadAnimator(this, idAnimatorRes)
        flipLestOutAnimator.setTarget(view)
        flipLestOutAnimator.start()
    }

    private fun putMarkActivity(mark: Int) {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putInt(CURRENT_ACTIVITY, mark)
            .apply()
    }

    private fun startTimer() {
        tStart = SystemClock.elapsedRealtime()
        timer = TimeCalculator().createTimer(timeRunningTextView, tStart, calendar)
        beginTime = System.currentTimeMillis()
        handler?.postDelayed(timer!!, HANDLER_DELAY)
    }

    private fun setFinishBtnListener() {
        finishBtn.setOnClickListener {
            if (isGpsEnabled()) {
                finishBtn.isEnabled = false
                isFinish = true
                startAnimation(runningLayout, R.animator.anim_close)
                startAnimation(finishLayout, R.animator.anim_open)
                finishLayout.isVisible = true
                startService(value = false)
                handler?.removeCallbacks(timer!!)
                createFinishTimeText()
                putMarkActivity(mark = MAIN_ACTIVITY_MARKER)
            }
        }
    }

    private fun createFinishTimeText() {
        val format = SimpleDateFormat(PATTERN_WITH_SECONDS, Locale.getDefault())
        format.timeZone = timeZone
        finishTimeRunning.text = format.format(calendar.time.time)
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
    }

    private fun setIsFromNotificationInSharedPref() {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(IS_FROM_NOTIFICATION, false)
            .apply()
    }

    private fun getTokenFromSharedPref(): String {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, EMPTY_VALUE).toString()
    }

    private fun createSaveTrackRequest() = SaveTrackRequest(
        token = getTokenFromSharedPref(),
        serverId = null,
        beginTime = beginTime,
        time = calendar.time.time,
        distance = distance,
        pointForData = coordinateList
    )

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            true
        } else {
            createAlertDialog(message = R.string.gps_enabled)
            false
        }
    }

    private fun showDialogs(message: Task<SaveTrackResponse>) {
        when {
            message.error != null -> {
                createAlertDialog(message = R.string.not_internet)
            }
            message.result.status == ERROR -> {
                createAlertDialog(message = message.result.error!!)
            }
            else -> {
                createAlertDialog(message = getString(R.string.successfully))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, IntentFilter(LOCATION_UPDATE))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_FINISH, isFinish)
        outState.putLong(BEGIN_TIME, beginTime)
        outState.putBoolean(RUNNING_VISIBLE, runningLayout.isVisible)
        outState.putBoolean(FINISH_VISIBLE, finishLayout.isVisible)
        outState.putLong(START, tStart)
        outState.putString(DISTANCE, distanceTextView.text.toString())
        outState.putString(FINISH_TIME, finishTimeRunning.text.toString())
    }

    private fun createAlertDialog(message: Int) {
        alertDialog?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
            dialog.cancel()
        }
        alertDialog?.setTitle(R.string.error)
        alertDialog?.setMessage(message)
        alertDialog?.setIcon(R.drawable.ic_baseline_error_outline_24)
        alertDialog?.show()
    }

    private fun createAlertDialog(message: String) {
        alertDialog?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
            dialog.cancel()
        }
        alertDialog?.setTitle(R.string.error)
        alertDialog?.setMessage(message)
        alertDialog?.setIcon(R.drawable.ic_baseline_error_outline_24)
        alertDialog?.show()
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
            createAlertDialog(message = R.string.press_finish)
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
        alertDialog = null
        handler = null
        timer = null
    }
}