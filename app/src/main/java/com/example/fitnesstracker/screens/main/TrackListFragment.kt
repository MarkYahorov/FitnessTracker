package com.example.fitnesstracker.screens.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.tracks.Track
import com.example.fitnesstracker.models.tracks.TrackRequest
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.google.android.material.floatingactionbutton.FloatingActionButton


class TrackListFragment : Fragment() {

    companion object{
        fun newInstance(token:String) =
            TrackListFragment().apply {
                val bundle = Bundle()
                bundle.putString(CURRENT_TOKEN, token)
                arguments = bundle
            }
    }

    interface Navigator{
        fun goToRunningScreen()
        fun goToTrackScreen(id:Int)
    }

    private lateinit var trackRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    private val trackList = mutableListOf<Track>()
    private val repositoryImpl = App.INSTANCE.repositoryImpl
    private var navigator: Navigator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as Navigator
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_list, container, false)
        initAllViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTrackRecycler()
        getTacksFromServer()
        setFABListener()
    }

    private fun getTacksFromServer(){
        repositoryImpl.getTracks(createTrackRequest())
            .continueWith ({ response ->
                when {
                    response.error!= null -> {
                        Log.e("key", "bolts error")
                    }
                    response.result.status == "error" -> {
                        Log.e("key", "error in request")
                    }
                    else -> {
                        val sortedList = response.result.tracks.sortedBy { it.beginTime }
                        trackList.addAll(sortedList)
                        trackRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            },Task.UI_THREAD_EXECUTOR)
    }

    private fun setFABListener(){
        fab.setOnClickListener {
            navigator?.goToRunningScreen()
        }
    }

    private fun createTrackRequest() = TrackRequest(arguments?.getString(CURRENT_TOKEN,"")!!)

    private fun initAllViews(view: View){
        trackRecyclerView = view.findViewById(R.id.track_recycler)
        fab = view.findViewById(R.id.open_screen_running_btn)
    }

    private fun initTrackRecycler(){
        with(trackRecyclerView){
            adapter = TrackListAdapter(listOfTracks = trackList, goToCurrentTrack = {
                navigator?.goToTrackScreen(it.id)
            })
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }
}