package com.example.fitnesstracker.screens.main.track

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fitnesstracker.R


class TrackFragment : Fragment() {

    companion object{
        private const val CURRENT_TRACK_ID = "CURRENT_TRACK_ID"
        fun newInstance(id:Int): TrackFragment {
            val fragment = TrackFragment()
            val bundle = Bundle()
            bundle.putInt(CURRENT_TRACK_ID, id)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_track, container, false)
    }
}