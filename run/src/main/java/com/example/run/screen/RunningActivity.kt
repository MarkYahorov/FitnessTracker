package com.example.run.screen

import android.Manifest
import android.animation.AnimatorInflater
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.example.base.screen.base.activity.BaseActivity
import com.example.core.Constants.CURRENT_TOKEN
import com.example.core.Constants.FITNESS_SHARED
import com.example.core.Constants.IS_FROM_NOTIFICATION
import com.example.core.Constants.MAIN_ACTIVITY_MARKER
import com.example.core.Constants.PATTERN_WITH_SECONDS
import com.example.core.Constants.RUNNING_ACTIVITY_MARKER
import com.example.core.Constants.UTC
import com.example.core.models.points.PointForData
import com.example.core.models.save.SaveTrackRequest
import com.example.core.models.save.SaveTrackResponse
import com.example.core.provideBaseComponent
import com.example.run.R
import com.example.run.di.DaggerRunningComponent
import com.example.run.presenter.RunningContract
import com.example.run.screen.calculate.TimeCalculator
import com.example.run.screen.service.CheckLocationService
import java.text.SimpleDateFormat
import java.util.*

class RunningActivity :
    BaseActivity<RunningContract.RunningPresenter, RunningContract.RunningView>(),
    RunningContract.RunningView, ServiceConnection, GPSListener {

    companion object {
        const val CURRENT_ACTIVITY = "CURRENT_ACTIVITY"
        const val ERROR = "error"
        private const val BEGIN_TIME = "BEGIN_TIME"
        private const val IS_FINISH = "IS_FINISH"
        private const val DISTANCE = "DISTANCE"
        private const val RUNNING_VISIBLE = "RUNNING_VISIBLE"
        private const val FINISH_VISIBLE = "FINISH_VISIBLE"
        private const val FINISH_TIME = "FINISH_TIME"
        private const val DEFAULT_REQUEST_CODE = 100
        private const val HANDLER_DELAY = 0L
        private const val MIN_GOOD_VALUE_SIZE_LIST = 1
    }

    private var isServiceConnected = false
    private var startBtn: Button? = null
    private var finishBtn: Button? = null
    private var timeRunningTextView: TextView? = null
    private var distanceTextView: TextView? = null
    private var finishTimeRunningTextView: TextView? = null
    private var toolbar: Toolbar? = null
    private var navDrawer: DrawerLayout? = null
    private var toggle: ActionBarDrawerToggle? = null
    private var startBtnLayout: ConstraintLayout? = null
    private var runningLayout: ConstraintLayout? = null
    private var finishLayout: ConstraintLayout? = null
    private var calculator: TimeCalculator? = null
    private var alertDialog: AlertDialog.Builder? = null
    private var handler: Handler? = null
    private var timer: Runnable? = null

    private val coordinateList = mutableListOf<PointForData>()
    private var calendar = Calendar.getInstance()
    private val timeZone = SimpleTimeZone.getTimeZone(UTC)
    private var isFinish = true
    private var distance = 0
    private var beginTime = 0L
    private var service: CheckLocationService? = null
    private val runningComponent by lazy {
        DaggerRunningComponent.factory().create(provideBaseComponent(applicationContext))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        getPresenter().enabledPermissions(checkPermissions())
        initAll()
        calendar.timeZone = timeZone
        if (savedInstanceState != null) {
            beginTime = savedInstanceState.getLong(BEGIN_TIME)
            isFinish = savedInstanceState.getBoolean(IS_FINISH)
            distanceTextView?.text = savedInstanceState.getString(DISTANCE)
            finishTimeRunningTextView?.text = savedInstanceState.getString(FINISH_TIME)
            runningLayout?.isVisible = savedInstanceState.getBoolean(RUNNING_VISIBLE)
            finishLayout?.isVisible = savedInstanceState.getBoolean(FINISH_VISIBLE)
            timer = getPresenter().startTimer(beginTime, calendar, handler!!)
            handler?.postDelayed(timer!!, HANDLER_DELAY)
            checkLayoutsVisibility()
        } else if (getMarkerActivity() == 1) {
            startBtnLayout?.isVisible = false
            runningLayout?.isVisible = true
            beginTime =
                getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE).getLong(
                    BEGIN_TIME, 0L
                )
            startTimer()
        }
    }

    private fun checkLayoutsVisibility() {
        if (finishLayout?.isVisible == true) {
            runningLayout?.isVisible = false
            startBtnLayout?.isVisible = false
        } else if (runningLayout?.isVisible == true) {
            startBtnLayout?.isVisible = false
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
        finishTimeRunningTextView = findViewById(R.id.finish_trunning_time)
        toolbar = findViewById(R.id.running_toolbar)
        navDrawer = findViewById(R.id.running_drawer)
        startBtnLayout = findViewById(R.id.start_btn_layout)
        runningLayout = findViewById(R.id.running_layout)
        finishLayout = findViewById(R.id.finish_running_layout)
        alertDialog = AlertDialog.Builder(this)
        handler = Handler(Looper.getMainLooper())
        calculator = TimeCalculator()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()

        setStartBtnClickListener()
        setFinishBtnListener()
        setToolbar()
        createDrawer()
        toggle?.isDrawerIndicatorEnabled = false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setStartBtnClickListener() {
        startBtn?.setOnClickListener {
            if (!checkPermissions()) {
                getPresenter().bindService(beginTime)
            } else {
                createAlertDialog(message = R.string.permissions_enabled)
            }
        }
    }

    private fun startAnimation(view: ConstraintLayout?, idAnimatorRes: Int) {
        val flipLestOutAnimator = AnimatorInflater.loadAnimator(this, idAnimatorRes)
        flipLestOutAnimator.setTarget(view)
        flipLestOutAnimator.start()
    }

    private fun putMarkActivity(mark: Int) {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit {
                this.putInt(CURRENT_ACTIVITY, mark)
                this.apply()
            }
    }

    private fun startTimer() {
        timer = getPresenter().startTimer(beginTime, calendar, handler!!)
        handler?.postDelayed(timer!!, HANDLER_DELAY)
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE).edit {
            this.putLong(BEGIN_TIME, beginTime)
            this.apply()
        }
    }

    private fun setFinishBtnListener() {
        finishBtn?.setOnClickListener {
            getPresenter().unBindService()
        }
    }

    private fun createFinishTimeText() {
        val dateFormat = SimpleDateFormat(PATTERN_WITH_SECONDS, Locale.getDefault())
        dateFormat.timeZone = timeZone
        finishTimeRunningTextView?.text = dateFormat.format(calendar.time.time)
    }

    private fun setToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindService() {
        val intent = Intent(this, CheckLocationService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        if (!isServiceConnected) {
            bindService()
            unbindService(this)
        } else {
            coordinateList.addAll(service?.getList()!!)
            distance = service?.getDistanceList()?.sum()?.toInt()!!
            distanceTextView?.text = distance.toString()
            unbindService(this)
            if (coordinateList.size > MIN_GOOD_VALUE_SIZE_LIST) {
                getPresenter().saveTrack(
                    createSaveTrackRequest(),
                    beginTime,
                    calendar,
                    distance,
                    coordinateList
                )
            } else {
                createAlertDialog(message = R.string.not_moving)
            }
        }
    }

    private fun createDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            navDrawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        toggle?.setToolbarNavigationClickListener {
            onBackPressed()
        }
    }

    private fun setIsFromNotificationInSharedPref() {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit {
                this.putBoolean(IS_FROM_NOTIFICATION, false)
                this.apply()
            }
    }

    private fun getTokenFromSharedPref(): String? {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, null)
    }

    private fun createSaveTrackRequest(): SaveTrackRequest? {
        val token = getTokenFromSharedPref()
        return if (token != null) {
            SaveTrackRequest(
                token = token,
                serverId = null,
                beginTime = beginTime,
                time = calendar.time.time,
                distance = distance,
                pointForData = coordinateList
            )
        } else {
            return null
        }
    }

    private fun showDialogs(message: SaveTrackResponse) {
        when (message.status) {
            ERROR -> {
                createAlertDialog(message = message.error!!)
            }
            else -> {
                createAlertDialog(message = getString(R.string.successfully))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(IS_FINISH, isFinish)
        outState.putLong(BEGIN_TIME, beginTime)
        runningLayout?.let { outState.putBoolean(RUNNING_VISIBLE, it.isVisible) }
        finishLayout?.let { outState.putBoolean(FINISH_VISIBLE, it.isVisible) }
        outState.putString(DISTANCE, distanceTextView?.text.toString())
        outState.putString(FINISH_TIME, finishTimeRunningTextView?.text.toString())
    }

    private fun createAlertDialog(message: Int) {
        alertDialog?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog?.setTitle(R.string.error)
        alertDialog?.setMessage(message)
        alertDialog?.setIcon(R.drawable.ic_baseline_error_outline_24)
        alertDialog?.show()
    }

    private fun createAlertDialog(message: String?) {
        alertDialog?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
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
                // startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else {
            createAlertDialog(message = R.string.press_finish)
            return
        }
        super.onBackPressed()
    }

    override fun onStop() {
        super.onStop()

        startBtn?.setOnClickListener(null)
        finishBtn?.setOnClickListener(null)
        toggle?.let { navDrawer?.removeDrawerListener(it) }
    }

    private fun getMarkerActivity() =
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE).getInt(
            CURRENT_ACTIVITY,
            MAIN_ACTIVITY_MARKER
        )

    override fun onDestroy() {
        calculator = null
        alertDialog = null
        handler = null
        timer = null
        distanceTextView = null
        startBtn = null
        finishBtn = null
        timeRunningTextView = null
        finishTimeRunningTextView = null
        toolbar = null
        navDrawer = null
        startBtnLayout = null
        runningLayout = null
        finishLayout = null

        super.onDestroy()
    }

    override fun showMessage(message: String?) {
        createAlertDialog(message)
    }

    override fun showMessage(message: SaveTrackResponse) {
        showDialogs(message)
    }

    override fun bind() {
        bindService()
        setIsFromNotificationInSharedPref()
    }

    override fun unbind() {
        finishBtn?.isEnabled = false
        isFinish = true
        startAnimation(view = runningLayout, idAnimatorRes = R.animator.anim_close)
        startAnimation(view = finishLayout, idAnimatorRes = R.animator.anim_open)
        finishLayout?.isVisible = true
        unbindService()
        handler?.removeCallbacks(timer!!)
        createFinishTimeText()
        putMarkActivity(mark = MAIN_ACTIVITY_MARKER)
    }

    override fun setTextFromCalculator(text: String) {
        timeRunningTextView?.text = text
    }

    override fun showLoading() {
        TODO("Not yet implemented")
    }

    override fun hideLoading() {
        TODO("Not yet implemented")
    }

    override fun createPresenter(): RunningContract.RunningPresenter {
        return runningComponent.presenter()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        this.service = (service as CheckLocationService.MyBinder).getService()
        val binder = this.service?.MyBinder()
        if (binder != null) {
            binder.setListener(this)
            if (binder.start()) {
                startBtn?.isEnabled = false
                startAnimation(startBtnLayout, R.animator.anim_close)
                startAnimation(runningLayout, R.animator.anim_open)
                runningLayout?.isVisible = true
                isFinish = false
                beginTime = System.currentTimeMillis()
                startTimer()
                putMarkActivity(mark = RUNNING_ACTIVITY_MARKER)
            } else {
                unbindService(this)
            }
        }
        isServiceConnected = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        isServiceConnected = false
    }

    override fun disabledGPS() {
        createAlertDialog(R.string.gps_enabled)
    }
}