package com.example.fitnesstracker.screens.main.running

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.CheckLocationService
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.models.points.Point
import com.example.fitnesstracker.models.save.SaveTrackRequest
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import java.text.SimpleDateFormat
import java.util.*

class RunningFragment : Fragment() {

    companion object {
        fun newInstance(token: String): RunningFragment {
            val runningFragment = RunningFragment()
            val bundle = Bundle()
            bundle.putString(CURRENT_TOKEN, token)
            runningFragment.arguments = bundle
            return runningFragment
        }
    }

    private lateinit var startBtn: Button
    private lateinit var finishBtn: Button
    private lateinit var timeRunning:TextView
    private lateinit var distanceTextView: TextView
    private lateinit var finishTimeRunning: TextView

    private var builder: AlertDialog.Builder? = null
    private var handler: Handler? = null
    private val coordinationList = mutableListOf<Point>()
    private val repo = App.INSTANCE.repositoryImpl
    private var distance = 0
    private var beginTime = 0L
    private var endTime = 0L

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
            coordinationList.addAll(intent.getParcelableArrayListExtra<Point>("allCoordinates")!!
                .toMutableList())
            repo.saveTrack(createSaveTrackRequest())
                .continueWith ({ saveTrackResponse ->
                    when {
                        saveTrackResponse.error != null -> {
                            insertTheTrack(null)
                            insertThePoints(null)
                            createAlertDialog("Lost Internet Connection")
                        }
                        saveTrackResponse.result.status == "error" -> {
                            if(saveTrackResponse.result.error == "NO_POINTS"){
                                createAlertDialog(saveTrackResponse.result.error)
                            }
                            createAlertDialog(saveTrackResponse.result.error)
                        }
                        else -> {
                            insertTheTrack(saveTrackResponse.result.serverId)
                            insertThePoints(saveTrackResponse.result.serverId)
                        }
                    }
                }, Task.UI_THREAD_EXECUTOR)
            Log.e("key", "${coordinationList.size}")
        }
    }

    private val timer = object :Runnable {
        override fun run() {
            tMilliSec = SystemClock.elapsedRealtime() - tStart
            tUpdate = tBuff + tMilliSec
            sec = (tUpdate/1000).toInt()
            min = sec/60
            hours = sec/3600
            sec %= 60
            millis = (tUpdate%100).toInt()
            timeRunning.text = "${String.format("%02d", hours)}: ${String.format("%02d", min)}: ${String.format("%02d", sec)}: ${String.format("%02d",millis)}"
            endTime = SystemClock.elapsedRealtime()
            Log.e("key", "milis $tMilliSec")
            Log.e("key", "end $endTime")
            Log.e("key", "update $tUpdate")
            handler?.postDelayed(this, 60)
        }
    }

    private var currentChronometerTime = 345847652L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_running, container, false)
        initAll(view)
        return view
    }


    private fun initAll(view: View) {
        distanceTextView = view.findViewById(R.id.distance_running)
        startBtn = view.findViewById(R.id.start_btn)
        finishBtn = view.findViewById(R.id.finish_btn)
        timeRunning = view.findViewById(R.id.time_text)
        finishTimeRunning = view.findViewById(R.id.finish_trunning_time)
        builder = AlertDialog.Builder(requireContext())
        handler = Handler()
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(broadcastReceiver, IntentFilter("location_update"))
    }

    private fun setButtonsClickListeners() {
        startBtn.setOnClickListener {
            val intent = Intent(requireContext(), CheckLocationService::class.java)
                .putExtra("Bool", true)
            beginTime = System.currentTimeMillis()
            activity?.startService(intent)
            finishBtn.isVisible = true
            startBtn.isVisible = false
            timeRunning.isVisible = true
            tStart = SystemClock.elapsedRealtime()
            handler?.postDelayed(timer, 0)
        }
        finishBtn.setOnClickListener {
            val intent = Intent(requireContext(), CheckLocationService::class.java)
                .putExtra("Bool", false)
            activity?.startService(intent)
            finishBtn.isVisible = false
            distanceTextView.isVisible = true
            timeRunning.isVisible = false
            finishTimeRunning.isVisible = true
            handler?.removeCallbacks(timer)
            endTime -= tStart
            finishTimeRunning.text = "${String.format("%02d", hours)}: ${String.format("%02d", min)}: ${String.format("%02d", sec)}: ${String.format("%02d",millis)}"
        }
    }

    private fun insertTheTrack(id: Int?) {
        InsertDBHelper()
            .setTableName("trackers")
            .addFieldsAndValuesToInsert(FitnessDatabase.ID_FROM_SERVER, id.toString())
            .addFieldsAndValuesToInsert(FitnessDatabase.BEGIN_TIME, beginTime.toString())
            .addFieldsAndValuesToInsert(FitnessDatabase.RUNNING_TIME,
                currentChronometerTime.toString())
            .addFieldsAndValuesToInsert(FitnessDatabase.DISTANCE, distance.toString())
            .insertTheValues(App.INSTANCE.db)
    }

    private fun insertThePoints(id: Int?) {
        coordinationList.forEach {
            InsertDBHelper()
                .setTableName("allPoints")
                .addFieldsAndValuesToInsert(FitnessDatabase.ID_FROM_SERVER,
                    id.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.LATITUDE,
                    it.lat.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.LONGITUDE,
                    it.lng.toString())
                .insertTheValues(App.INSTANCE.db)
        }
    }

    private fun createSaveTrackRequest() = SaveTrackRequest(arguments?.getString(CURRENT_TOKEN)!!,
        null,
        beginTime,
        endTime,
        distance,
        coordinationList)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissions()
        setButtonsClickListeners()
        if (savedInstanceState != null) {
            currentChronometerTime = savedInstanceState.getLong("current_time")
            distanceTextView.text = savedInstanceState.getString("DISTANCE")
            finishTimeRunning.text = savedInstanceState.getString("FINISH_TIME")
            startBtn.isVisible = savedInstanceState.getBoolean("BOOL")
            timeRunning.isVisible = savedInstanceState.getBoolean("TR")
            finishBtn.isVisible = savedInstanceState.getBoolean("FB")
            distanceTextView.isVisible = savedInstanceState.getBoolean("DTV")
            finishTimeRunning.isVisible = savedInstanceState.getBoolean("FTV")
            tStart = savedInstanceState.getLong("start")
            handler?.postDelayed(timer, 0)
        } else {
            currentChronometerTime = SystemClock.elapsedRealtime()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
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

    private fun checkPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
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

    override fun onDestroy() {
        super.onDestroy()
        Log.e("key", "DESTROY")
    }
}