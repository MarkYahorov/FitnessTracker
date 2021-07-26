package com.example.fitnesstracker.screens.main.track

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
import com.example.fitnesstracker.models.points.Point
import com.example.fitnesstracker.models.points.PointsRequest
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*


class TrackFragment : Fragment() {

    companion object {
        private const val CURRENT_TRACK_ID = "CURRENT_TRACK_ID"
        private const val CURRENT_BEGIN_TIME = "CURRENT_BEGIN_TIME"
        private const val CURRENT_RUNNING_TIME = "CURRENT_RUNNING_TIME"
        private const val CURRENT_DISTANCE = "CURRENT_DISTANCE"

        fun newInstance(
            id: Int,
            beginTime: Long,
            runningTime: Long,
            distance: Int,
            token: String,
        ): TrackFragment {
            val trackFragment = TrackFragment()
            val bundle = Bundle()
            bundle.putInt(CURRENT_TRACK_ID, id)
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
    private val allDistanceOfTrack = mutableListOf<Point>()
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
        runningTime.text = arguments?.getLong(CURRENT_RUNNING_TIME).toString()
        distance.text =  arguments?.getInt(CURRENT_DISTANCE).toString()
    }

    override fun onResume() {
        super.onResume()
        getTrackFromServer()
    }

    private fun getTrackFromServer() {
        repo.getPointsForCurrentTrack(createPointsRequest())
            .continueWith ({
                when {
                    it.error!=null -> {
                        Log.e("key", "it.error")
                    }
                    it.result.status == "error" -> {
                        Log.e("key", "status == error")
                    }
                    else -> {
                        allDistanceOfTrack.addAll(it.result.points)
                        allDistanceOfTrack.forEach { point ->
                            allPointsInLatLng.add(LatLng(point.lat,point.lng))
                        }
                        val startCoordinate = allDistanceOfTrack[0]
                        val startLatLng = LatLng(startCoordinate.lat,startCoordinate.lng)
                        val finishCoordinate = allDistanceOfTrack[allDistanceOfTrack.lastIndex]
                        val finishLatLng = LatLng(finishCoordinate.lat,finishCoordinate.lng)
                        googleMap?.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED)).position(startLatLng).title("Start"))
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(startLatLng))
                        googleMap?.addPolyline(PolylineOptions()
                            .clickable(false)
                            .addAll(allPointsInLatLng))
                        googleMap?.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_BLUE)).position(finishLatLng).title("Finish"))

                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds(startLatLng,finishLatLng),25))
                    }
                }
            }, Task.UI_THREAD_EXECUTOR)
    }

    private fun createPointsRequest() = PointsRequest(arguments?.getString(CURRENT_TOKEN)!!, arguments?.getInt(
        CURRENT_TRACK_ID)!!)
}