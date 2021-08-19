package com.example.fitnesstracker.screens.main.track

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.App.Companion.PATTERN_WITH_SECONDS
import com.example.fitnesstracker.App.Companion.UTC
import com.example.fitnesstracker.R
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
        private const val MAP_PADDING = 200
        private const val ZERO_RESULT = 0

        fun newInstance(
            id: Int,
            serverId: Int?,
            beginTime: Long,
            runningTime: Long,
            distance: Int,
            token: String,
        ): TrackFragment {
            val trackFragment = TrackFragment()
            val bundle = Bundle()
            bundle.putInt(CURRENT_DB_ID, id)
            bundle.putInt(CURRENT_TRACK_ID, serverId ?: ZERO_RESULT)
            bundle.putLong(CURRENT_BEGIN_TIME, beginTime)
            bundle.putLong(CURRENT_RUNNING_TIME, runningTime)
            bundle.putInt(CURRENT_DISTANCE, distance)
            bundle.putString(CURRENT_TOKEN, token)
            trackFragment.arguments = bundle
            return trackFragment
        }
    }

    private var runningTime: TextView? = null
    private var distance: TextView? = null
    private var googleMap: GoogleMap? = null
    private var alertDialog: AlertDialog.Builder? = null
    private var mapFragment: SupportMapFragment? = null

    private val allDistanceOfTrack = mutableListOf<PointForData>()
    private val allPointsInLatLng = mutableListOf<LatLng>()
    private val repo = App.INSTANCE.repositoryFromServerImpl
    private val callback = OnMapReadyCallback { map ->
        googleMap = map
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track, container, false)
        initAll(view = view)
        return view
    }

    private fun initAll(view: View) {
        runningTime = view.findViewById(R.id.current_track_running_time)
        distance = view.findViewById(R.id.current_track_distance)
        mapFragment = childFragmentManager.findFragmentById(R.id.track_map) as SupportMapFragment
        alertDialog = AlertDialog.Builder(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapFragment?.getMapAsync(callback)
        val format = SimpleDateFormat(PATTERN_WITH_SECONDS, Locale.getDefault())
        val timeZone = SimpleTimeZone.getTimeZone(UTC)
        format.timeZone = timeZone
        runningTime?.text = format.format(arguments?.getLong(CURRENT_RUNNING_TIME))
        distance?.text = arguments?.getInt(CURRENT_DISTANCE).toString()
    }

    override fun onResume() {
        super.onResume()

        getTrackPoints()
    }

    private fun getTrackPoints() {
        repo.getPointsForCurrentTrack(
            pointsRequest = createPointsRequest(),
            idInDb = arguments?.getInt(CURRENT_DB_ID) ?: ZERO_RESULT,
            serverId = arguments?.getInt(CURRENT_TRACK_ID) ?: ZERO_RESULT
        ).continueWith({
            processResult(taskOfPoints = it)
        }, Task.UI_THREAD_EXECUTOR)
    }

    private fun processResult(taskOfPoints: Task<List<PointForData>>) {
        when {
            taskOfPoints.error != null -> {
                createAlertDialog(error = taskOfPoints.error.message)
            }
            taskOfPoints.result.isEmpty() -> {
                createAlertDialog(error = getString(R.string.track_fragment_error))
            }
            else -> {
                allDistanceOfTrack.addAll(taskOfPoints.result)
                showTrackOnMap(
                    listOfPoints = allDistanceOfTrack,
                    listOfLatLng = allPointsInLatLng
                )
            }
        }
    }

    private fun createAlertDialog(error: String?) {
        alertDialog?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog?.setTitle(R.string.error)
        alertDialog?.setMessage(error)
        alertDialog?.setIcon(R.drawable.ic_baseline_error_outline_24)
        alertDialog?.show()
    }

    private fun showTrackOnMap(
        listOfPoints: List<PointForData>,
        listOfLatLng: MutableList<LatLng>
    ) {
        val startCoordinate = listOfPoints.first()
        val startLatLng = LatLng(startCoordinate.lat, startCoordinate.lng)
        val finishCoordinate = listOfPoints.last()
        val finishLatLng = LatLng(finishCoordinate.lat, finishCoordinate.lng)
        listOfPoints.forEach { points ->
            listOfLatLng.add(LatLng(points.lat, points.lng))
        }
        addMarker(
            iconColor = BitmapDescriptorFactory.HUE_RED,
            position = startLatLng,
            title = getString(R.string.start)
        )
        addPolyline(allPointsInLatLng = listOfLatLng)
        addMarker(
            iconColor = BitmapDescriptorFactory.HUE_BLUE,
            position = finishLatLng,
            title = getString(R.string.finish)
        )
        val runningWayBuilder = LatLngBounds.Builder()
        allPointsInLatLng.forEach {
            runningWayBuilder.include(it)
        }
        val track = CameraUpdateFactory.newLatLngBounds(
            runningWayBuilder.build(),
            MAP_PADDING
        )
        googleMap?.animateCamera(track)
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

    private fun createPointsRequest(): PointsRequest? {
        val token = arguments?.getString(CURRENT_TOKEN)
        val trackId = arguments?.getInt(
            CURRENT_TRACK_ID
        )
        return if (token != null && trackId != null) {
            PointsRequest(token, trackId)
        } else {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        googleMap = null
        alertDialog = null
        mapFragment = null
        runningTime = null
        distance = null
    }
}