package com.example.fitnesstracker.screens.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fitnesstracker.R


class TrackListFragment : Fragment() {

    companion object{
        fun newInstance(token:String) =
            TrackListFragment().apply {
                val bundle = Bundle()
                bundle.putString("CURRENT_TOKEN", token)
                arguments = bundle
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_track_list, container, false)
    }
}