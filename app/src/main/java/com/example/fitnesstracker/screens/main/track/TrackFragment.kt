package com.example.fitnesstracker.screens.main.track

import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.database.FitnessDatabase
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.CURRENT_TRACK
import com.example.fitnesstracker.data.database.FitnessDatabase.Companion.LATITUDE
import com.example.fitnesstracker.data.database.helpers.InsertDBHelper
import com.example.fitnesstracker.models.points.PointForData
import com.example.fitnesstracker.models.points.PointsRequest
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.text.SimpleDateFormat
import java.util.*


class TrackFragment : Fragment() {

    companion object {
        private const val CURRENT_TRACK_ID = "CURRENT_TRACK_ID"
        private const val CURRENT_BEGIN_TIME = "CURRENT_BEGIN_TIME"
        private const val CURRENT_RUNNING_TIME = "CURRENT_RUNNING_TIME"
        private const val CURRENT_DISTANCE = "CURRENT_DISTANCE"
        private const val CURRENT_DB_ID = "CURRENT_ID"

        fun newInstance(
            id: Int,
            serverId: Int,
            beginTime: Long,
            runningTime: Long,
            distance: Int,
            token: String,
        ): TrackFragment {
            val trackFragment = TrackFragment()
            val bundle = Bundle()
            bundle.putInt(CURRENT_DB_ID, id)
            bundle.putInt(CURRENT_TRACK_ID, serverId)
            bundle.putLong(CURRENT_BEGIN_TIME, beginTime)
            bundle.putLong(CURRENT_RUNNING_TIME, runningTime)
            bundle.putInt(CURRENT_DISTANCE, distance)
            bundle.putString(CURRENT_TOKEN, token)
            trackFragment.arguments = bundle
            return trackFragment
        }
    }

    private lateinit var runningTime: TextView
    private lateinit var distance: TextView

    private var googleMap: GoogleMap? = null
    private var trackFragment: SupportMapFragment? = null
    private val allDistanceOfTrack = mutableListOf<PointForData>()
    private val allPointsInLatLng = mutableListOf<LatLng>()
    private val repo = App.INSTANCE.repositoryImpl

    private val callback = OnMapReadyCallback { map ->
        googleMap = map
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track, container, false)
        initAll(view)
        return view
    }

    private fun initAll(view: View) {
        runningTime = view.findViewById(R.id.current_track_running_time)
        distance = view.findViewById(R.id.current_track_distance)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackFragment = childFragmentManager.findFragmentById(R.id.track_map) as SupportMapFragment
        trackFragment?.getMapAsync(callback)
        val format = SimpleDateFormat("HH:mm:ss,SS", Locale.getDefault())
        val timeZone = SimpleTimeZone.getTimeZone("UTC")
        format.timeZone = timeZone
        runningTime.text = format.format(arguments?.getLong(CURRENT_RUNNING_TIME))
        distance.text = arguments?.getInt(CURRENT_DISTANCE).toString()
    }

    override fun onResume() {
        super.onResume()
        if (!checkThisPointIntoDb(arguments?.getInt(CURRENT_DB_ID)!!)) {
            getTrackFromServer()
        } else {
            getListOfPointsFromDb(arguments?.getInt(CURRENT_DB_ID)!!)
        }
    }

    private fun getListOfPointsFromDb(id: Int) {
        repo.getPointsForCurrentTrackFromDb(id)
            .continueWith ({ listFromDb ->
                listFromDb.result.forEach {
                    allDistanceOfTrack.add(PointForData(it.lng, it.lat))
                }
                processResult(allDistanceOfTrack, allPointsInLatLng)
            },Task.UI_THREAD_EXECUTOR)
    }


    private fun getTrackFromServer() {
        repo.getPointsForCurrentTrack(createPointsRequest())
            .continueWith({
                when {
                    it.error != null -> {
                        Log.e("key", "it.error")
                    }
                    it.result.status == "error" -> {
                        Log.e("key", "${it.result.error}")
                    }
                    else -> {
                        allDistanceOfTrack.addAll(it.result.pointForData)
                        insertPointsIntoDb(allDistanceOfTrack = allDistanceOfTrack)
                        processResult(allDistanceOfTrack, allPointsInLatLng)
                    }
                }
            }, Task.UI_THREAD_EXECUTOR)
    }

    private fun insertPointsIntoDb(allDistanceOfTrack: List<PointForData>) {
        allDistanceOfTrack.forEach {
            InsertDBHelper()
                .setTableName("allPoints")
                .addFieldsAndValuesToInsert(FitnessDatabase.ID_FROM_SERVER, arguments?.getInt(CURRENT_TRACK_ID)!!.toString())
                .addFieldsAndValuesToInsert(CURRENT_TRACK, arguments?.getInt(CURRENT_DB_ID)!!.toString())
                .addFieldsAndValuesToInsert(LATITUDE, it.lat.toString())
                .addFieldsAndValuesToInsert(FitnessDatabase.LONGITUDE, it.lng.toString())
                .insertTheValues(App.INSTANCE.db)
        }
    }

    private fun processResult(
        allDistanceOfTrack: List<PointForData>,
        allPointsInLatLng: MutableList<LatLng>
    ) {
        val startCoordinate = allDistanceOfTrack[0]
        val startLatLng = LatLng(startCoordinate.lat, startCoordinate.lng)
        val finishCoordinate = allDistanceOfTrack[allDistanceOfTrack.lastIndex]
        val finishLatLng = LatLng(finishCoordinate.lat, finishCoordinate.lng)
        allDistanceOfTrack.forEach { points ->
            allPointsInLatLng.add(LatLng(points.lat, points.lng))
        }
        addMarker(BitmapDescriptorFactory.HUE_RED, startLatLng, "Start")
        addPolyline(allPointsInLatLng = allPointsInLatLng)
        addMarker(BitmapDescriptorFactory.HUE_BLUE, finishLatLng, "Finish")
        val builder = LatLngBounds.Builder()
        builder.include(startLatLng).include(finishLatLng)
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                builder.build(),
                500
            )
        )
    }

    private fun addMarker(iconColor: Float, position: LatLng, title: String) {
        googleMap?.addMarker(
            MarkerOptions().icon(
                BitmapDescriptorFactory.defaultMarker(
                    iconColor
                )
            ).position(position).title(title)
        )
    }

    private fun addPolyline(allPointsInLatLng: MutableList<LatLng>) {
        googleMap?.addPolyline(
            PolylineOptions()
                .clickable(false)
                .addAll(allPointsInLatLng)
        )
    }


    private fun checkThisPointIntoDb(id: Int): Boolean {
        var cursor: Cursor? = null
        val haveData: Boolean?
        try {
            cursor = App.INSTANCE.db.rawQuery("SELECT * FROM allPoints WHERE currentTrack=$id", null)
            haveData = cursor.moveToFirst()
        } finally {
            cursor?.close()
        }
        return haveData!!
    }

    private fun createPointsRequest() = PointsRequest(
        arguments?.getString(CURRENT_TOKEN)!!, arguments?.getInt(
            CURRENT_TRACK_ID
        )!!
    )
}