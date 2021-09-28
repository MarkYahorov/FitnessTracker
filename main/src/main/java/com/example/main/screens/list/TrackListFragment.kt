package com.example.main.screens.list

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.base.screen.base.fragment.BaseFragment
import com.example.core.Constants.CURRENT_TOKEN
import com.example.core.Constants.FITNESS_SHARED
import com.example.core.Constants.IS_FIRST
import com.example.core.models.tracks.TrackFromDb
import com.example.core.provideBaseComponent
import com.example.main.R
import com.example.main.di.list.DaggerListComponent
import com.example.main.presenter.list.ListContract
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.*


class TrackListFragment : BaseFragment<ListContract.ListPresenter, ListContract.ListView>(),
    ListContract.ListView {

    companion object {
        private const val RANGE_FOR_INSERT = 1
        private const val LIST_START_POSITION = 0
        private const val TRACK_LIST = "TRACK_LIST"
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
    private var alertDialog: AlertDialog.Builder? = null
    private val listComponent by lazy {
        DaggerListComponent.factory()
            .create(provideBaseComponent(requireContext().applicationContext))
    }

    private var trackList = mutableListOf<TrackFromDb>()
    private var scrollPositionOfRecycler = 0

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
        listComponent.inject(this)
        return view
    }

    private fun initAll(view: View) {
        trackRecyclerView = view.findViewById(R.id.track_recycler)
        addTrackBtn = view.findViewById(R.id.open_screen_running_btn)
        swipeRefreshLayout = view.findViewById(R.id.swipe_layout)
        progressBar = view.findViewById(R.id.loading_progress)
        alertDialog = AlertDialog.Builder(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTrackRecycler()
        getPresenter().setIsFirstInApp()

        if (savedInstanceState != null) {
            scrollPositionOfRecycler = savedInstanceState.getInt(POSITION)
            trackRecyclerView?.adapter?.notifyItemRangeInserted(
                LIST_START_POSITION,
                RANGE_FOR_INSERT
            )
        }
    }

    private fun initTrackRecycler() {
        with(trackRecyclerView) {
            val token = arguments?.getString(CURRENT_TOKEN)
            if (token != null) {
                this?.adapter =
                    TrackListAdapter(listOfTrackForData = trackList, goToCurrentTrack = {
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

        getPresenter().loadTracks(getToken())
        setFABListener()
        setSwipeLayoutListener()
        addScrollListener()
    }

    override fun setData(listOfTracks: List<TrackFromDb>) {
        if (listOfTracks.size > trackList.size) {
            val oldListSize = listOfTracks.size - trackList.size
            trackList.clear()
            trackRecyclerView?.adapter?.notifyItemRangeRemoved(LIST_START_POSITION, trackList.size)
            var id = listOfTracks.size
            listOfTracks.forEach {
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
        }
    }

    override fun showError(error: String?) {
        createAlertDialog(error)
    }

    override fun endLoading() {
        progressBar?.isVisible = false
        swipeRefreshLayout?.isRefreshing = false
    }

    override fun setIsFirst(): Boolean {
        return activity?.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            ?.getBoolean(IS_FIRST, true)!!
    }

    override fun changeIsFirst() {
        activity?.getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            ?.edit {
                this.putBoolean(IS_FIRST, false)
                this.apply()
            }
    }

    override fun showStartDialog() {
        alertDialog?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog?.setTitle(R.string.attention)
        alertDialog?.setMessage(R.string.waring)
        alertDialog?.setIcon(R.drawable.ic_baseline_error_outline_24)
        alertDialog?.show()
    }

    override fun showLoading() {
    }

    override fun hideLoading() {
    }

    private fun addScrollListener() {
        trackRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                scrollPositionOfRecycler = layoutManager.findFirstVisibleItemPosition()
            }
        })
    }

    private fun setSwipeLayoutListener() {
        swipeRefreshLayout?.setOnRefreshListener {
            getPresenter().getTracks(true, getToken())
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

        outState.putParcelableArrayList(
            TRACK_LIST,
            trackList as ArrayList<TrackFromDb>
        )
        outState.putInt(POSITION, scrollPositionOfRecycler)
    }

    private fun getToken(): String {
        return arguments?.getString(CURRENT_TOKEN, null)!!
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

    private fun createAlertDialog() {
        alertDialog?.setPositiveButton(R.string.ok_thanks) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog?.setTitle(R.string.error)
        alertDialog?.setMessage(R.string.re_login)
        alertDialog?.setIcon(R.drawable.ic_baseline_error_outline_24)
        alertDialog?.show()
    }

    override fun onStop() {
        super.onStop()

        trackRecyclerView?.clearOnScrollListeners()
        addTrackBtn?.setOnClickListener(null)
        swipeRefreshLayout?.setOnRefreshListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        alertDialog = null
        trackRecyclerView = null
        addTrackBtn = null
        swipeRefreshLayout = null
        progressBar = null
    }

    override fun createPresenter(): ListContract.ListPresenter {
        return listComponent.presenter()
    }
}