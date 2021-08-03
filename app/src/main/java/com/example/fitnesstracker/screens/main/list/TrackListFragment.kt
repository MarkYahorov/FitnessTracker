package com.example.fitnesstracker.screens.main.list

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.Tracks
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*


class TrackListFragment : Fragment() {

    companion object {
        fun newInstance(token: String) =
            TrackListFragment().apply {
                val bundle = Bundle()
                bundle.putString(CURRENT_TOKEN, token)
                arguments = bundle
            }
    }

    interface Navigator {
        fun goToRunningScreen(token: String, trackId: Int)
        fun goToTrackScreen(
            id: Int,
            serverId: Int,
            beginTime: Long,
            runningTime: Long,
            distance: Int,
            token: String,
        )
    }

    private lateinit var trackRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var trackList = mutableListOf<Tracks>()
    private var oldListSize = 0
    private val repositoryImpl = App.INSTANCE.repositoryImpl
    private var navigator: Navigator? = null
    private var builder: AlertDialog.Builder? = null
    private var isFirstInApp: Boolean = true
    private var isLoading = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as Navigator
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_list, container, false)
        isFirstInApp = activity?.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            ?.getBoolean("IS_FIRST", true)!!
        initAll(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            if (isFirstInApp) {
                getTracksFromServer()
            } else {
                getTracksFromDb()
            }
        } else {
            oldListSize = savedInstanceState.getInt("OLD_LIST_SIZE")
            trackList = savedInstanceState.getParcelableArrayList<Tracks>("TRACK_LIST")!!.toMutableList()
        }
        initTrackRecycler()
        setFABListener()
        setSwipeLayoutListener()
        activity?.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("IS_FIRST", false)
            ?.apply()
    }

    private fun createAlertDialog(error: String?) {
        builder?.setPositiveButton("Ok, thanks") { _, _ ->
        }
        builder?.setTitle("ERROR")
        builder?.setMessage(error)
        builder?.setIcon(R.drawable.ic_baseline_error_outline_24)
        builder?.show()
    }

    private fun getTracksFromServer() {
        if (!isLoading) {
            isLoading = true
            repositoryImpl.getTracks(createTrackRequest())
                .continueWith({ response ->
                    when {
                        response.error != null -> {
                            createAlertDialog(response.error.message)
                        }
                        response.result.status == "error" -> {
                            createAlertDialog(response.result.status)
                        }
                        else -> {
                            val sortedList =
                                response.result.trackForData.sortedByDescending { it.beginTime }
                            if (sortedList.size > trackList.size) {
                                oldListSize = sortedList.size - trackList.size
                                trackList.clear()
                                trackRecyclerView.adapter?.notifyItemRangeRemoved(0, trackList.size)
                                sortedList.forEach {
                                    var id = sortedList.size
                                    trackList.add(
                                        Tracks(
                                            id,
                                            it.serverId!!,
                                            it.beginTime,
                                            it.time,
                                            it.distance
                                        )
                                    )
                                    id-=1
                                    Log.e("key", "$it")
                                }
                                trackRecyclerView.adapter?.notifyItemRangeInserted(0, oldListSize)
                                trackRecyclerView.scrollToPosition(0)
                                oldListSize = trackList.size
                            }
                        }
                    }
                    isLoading = false
                    swipeRefreshLayout.isRefreshing = false
                }, Task.UI_THREAD_EXECUTOR)
        } else {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("OLD_LIST_SIZE", oldListSize)
        outState.putParcelableArrayList("TRACK_LIST", trackList as ArrayList<Tracks>)
    }

    private fun getTracksFromDb() {
        repositoryImpl.getListOfTrack()
            .continueWith({ listOfTracks ->
                oldListSize = listOfTracks.result.size - trackList.size
                if (trackList.size<listOfTracks.result.size) {
                    trackList.addAll(listOfTracks.result)
                    trackList.sortByDescending { it.beginTime }
                }
                trackRecyclerView.adapter?.notifyItemRangeInserted(0,oldListSize)
                getTracksFromServer()
            }, Task.UI_THREAD_EXECUTOR)
    }

    private fun setFABListener() {
        fab.setOnClickListener {
            navigator?.goToRunningScreen(arguments?.getString(CURRENT_TOKEN)!!, trackList.size + 1)
        }
    }

    private fun createTrackRequest() =
        TrackRequest(token = arguments?.getString(CURRENT_TOKEN, "")!!)

    private fun initAll(view: View) {
        trackRecyclerView = view.findViewById(R.id.track_recycler)
        fab = view.findViewById(R.id.open_screen_running_btn)
        swipeRefreshLayout = view.findViewById(R.id.swipe_layout)
        builder = AlertDialog.Builder(requireContext())
    }

    private fun setSwipeLayoutListener() {
        swipeRefreshLayout.setOnRefreshListener {
            if (!isLoading) {
                getTracksFromServer()
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun initTrackRecycler() {
        with(trackRecyclerView) {
            adapter = TrackListAdapter(listOfTrackForData = trackList, goToCurrentTrack = {
                navigator?.goToTrackScreen(
                    it.id!!,
                    it.serverId,
                    it.beginTime.toString().toLong(),
                    it.time,
                    it.distance,
                    arguments?.getString(
                        CURRENT_TOKEN
                    )!!
                )
            })
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        builder = null
    }
}