package com.example.fitnesstracker.screens.main.list

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.tracks.TrackForData
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.models.tracks.TrackFromDb
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.*


class TrackListFragment : Fragment() {

    companion object {
        const val IS_FIRST = "IS_FIRST"
        private const val OLD_LIST_SIZE = "OLD_LIST_SIZE"
        private const val LIST_START_POSITION = 0
        private const val TRACK_LIST = "TRACK_LIST"
        private const val ERROR = "error"
        private const val POSITION = "POSITION"
        private const val ONE_FOR_ID = 1

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
            serverId: Int?,
            beginTime: Long,
            runningTime: Long,
            distance: Int,
            token: String,
        )
    }

    private var trackRecyclerView: RecyclerView? = null
    private var addTrackBtn: FloatingActionButton? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var progressBar: CircularProgressIndicator? = null
    private var navigator: Navigator? = null
    private var builder: AlertDialog.Builder? = null

    private var trackList = mutableListOf<TrackFromDb>()
    private var oldListSize = 0
    private val serverRepository = App.INSTANCE.repositoryFromServerImpl
    private val dbRepository = App.INSTANCE.repositoryForDbImpl
    private var isFirstTimeInApp = true
    private var isLoading = false
    private var position = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as Navigator
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_list, container, false)
        initAll(view = view)
        return view
    }

    private fun setIsFirstTimeInApp() {
        isFirstTimeInApp = activity?.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            ?.getBoolean(IS_FIRST, true)!!
    }

    private fun initAll(view: View) {
        trackRecyclerView = view.findViewById(R.id.track_recycler)
        addTrackBtn = view.findViewById(R.id.open_screen_running_btn)
        swipeRefreshLayout = view.findViewById(R.id.swipe_layout)
        progressBar = view.findViewById(R.id.loading_progress)
        builder = AlertDialog.Builder(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTrackRecycler()
        setIsFirstTimeInApp()
        if (savedInstanceState != null) {
            oldListSize = savedInstanceState.getInt(OLD_LIST_SIZE)
            position = savedInstanceState.getInt(POSITION)
            trackRecyclerView?.adapter?.notifyItemRangeInserted(LIST_START_POSITION, oldListSize)
        }
    }

    private fun initTrackRecycler() {
        with(trackRecyclerView) {
            val token = arguments?.getString(
                CURRENT_TOKEN
            )
            if (token != null) {
                this?.adapter = TrackListAdapter(listOfTrackForData = trackList, goToCurrentTrack = {
                    navigator?.goToTrackScreen(
                        id = it.id,
                        serverId = it.serverId,
                        beginTime = it.beginTime.toString().toLong(),
                        runningTime = it.time,
                        distance = it.distance,
                        token = token
                    )
                })
            }
            this?.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun onStart() {
        super.onStart()
        if (isFirstTimeInApp) {
            progressBar?.isVisible = true
            putIsFirstValueInSharedPref()
            createAlertDialogToDisableBatterySaver()
            getTracksFromServer()
        } else {
            getTracksFromDb()
        }
        setFABListener()
        setSwipeLayoutListener()
        addScrollListener()
    }

    private fun putIsFirstValueInSharedPref() {
        activity?.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(IS_FIRST, false)
            ?.apply()
    }

    private fun createAlertDialogToDisableBatterySaver() {
        builder?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
        }
        builder?.setTitle(R.string.attention)
        builder?.setMessage(R.string.waring)
        builder?.setIcon(R.drawable.ic_baseline_error_outline_24)
        builder?.show()
    }

    private fun getTracksFromDb() {
        dbRepository.getListOfTrack()
            .continueWith({ listOfTracks ->
                oldListSize = listOfTracks.result.size - trackList.size
                if (trackList.size < listOfTracks.result.size) {
                    trackList.clear()
                    trackList.addAll(listOfTracks.result)
                    trackList.sortByDescending { it.beginTime }
                }
                trackRecyclerView?.adapter?.notifyItemRangeInserted(LIST_START_POSITION, oldListSize)
                trackRecyclerView?.scrollToPosition(position)
                getTracksFromServer()
            }, Task.UI_THREAD_EXECUTOR)
    }

    private fun getTracksFromServer() {
        if (!isLoading) {
            isLoading = true
            serverRepository.getTracks(createTrackRequest())
                .continueWith({ response ->
                    when {
                        response.error != null -> {
                            createAlertDialog(error = response.error.message)
                        }
                        response.result.status == ERROR -> {
                            createAlertDialog(error = response.result.error)
                        }
                        else -> {
                            val sortedList =
                                response.result.trackForData.sortedByDescending { it.beginTime }
                            addTracksIfDbIsEmpty(listOfTrack = sortedList)
                        }
                    }
                    isLoading = false
                    progressBar?.isVisible = false
                    swipeRefreshLayout?.isRefreshing = false
                }, Task.UI_THREAD_EXECUTOR)
        } else {
            swipeRefreshLayout?.isRefreshing = false
        }
    }

    private fun addTracksIfDbIsEmpty(listOfTrack: List<TrackForData>) {
        if (listOfTrack.size > trackList.size) {
            oldListSize = listOfTrack.size - trackList.size
            trackList.clear()
            trackRecyclerView?.adapter?.notifyItemRangeRemoved(LIST_START_POSITION, trackList.size)
            var id = listOfTrack.size
            listOfTrack.forEach {
                trackList.add(
                    TrackFromDb(
                        id = id,
                        serverId = it.serverId,
                        beginTime = it.beginTime,
                        time = it.time,
                        distance = it.distance
                    )
                )
                id -= ONE_FOR_ID
            }
            trackRecyclerView?.adapter?.notifyItemRangeInserted(LIST_START_POSITION, oldListSize)
            trackRecyclerView?.scrollToPosition(LIST_START_POSITION)
            oldListSize = trackList.size
        }
    }

    private fun addScrollListener() {
        trackRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                position = layoutManager.findFirstVisibleItemPosition()
            }
        })
    }

    private fun setSwipeLayoutListener() {
        swipeRefreshLayout?.setOnRefreshListener {
            if (!isLoading) {
                getTracksFromServer()
            } else {
                swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private fun setFABListener() {
        addTrackBtn?.setOnClickListener {
            val token = arguments?.getString(CURRENT_TOKEN)
            if (token != null) {
                navigator?.goToRunningScreen(
                    token = token,
                    trackId = trackList.size + ONE_FOR_ID
                )
            } else {
                createAlertDialog()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(OLD_LIST_SIZE, oldListSize)
        outState.putParcelableArrayList(TRACK_LIST, trackList as ArrayList<TrackFromDb>)
        outState.putInt(POSITION, position)
    }

    private fun createTrackRequest(): TrackRequest? {
        val token = arguments?.getString(CURRENT_TOKEN, null)
        return if (token != null) {
            TrackRequest(token = token)
        } else {
            null
        }
    }

    private fun createAlertDialog(error: String?) {
        builder?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
        }
        builder?.setTitle(R.string.error)
        builder?.setMessage(error)
        builder?.setIcon(R.drawable.ic_baseline_error_outline_24)
        builder?.show()
    }

    private fun createAlertDialog() {
        builder?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
        }
        builder?.setTitle(R.string.error)
        builder?.setMessage(R.string.re_login)
        builder?.setIcon(R.drawable.ic_baseline_error_outline_24)
        builder?.show()
    }

    override fun onStop() {
        super.onStop()
        trackRecyclerView?.clearOnScrollListeners()
        addTrackBtn?.setOnClickListener(null)
        swipeRefreshLayout?.setOnRefreshListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        builder = null
        trackRecyclerView = null
        addTrackBtn = null
        swipeRefreshLayout = null
        progressBar = null
    }
}