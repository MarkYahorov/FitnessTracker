package com.example.fitnesstracker.screens.main.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.tracks.Track
import java.text.SimpleDateFormat
import java.util.*

class TrackListAdapter(
    private val listOfTracks: MutableList<Track>,
    private val goToCurrentTrack: (Track) -> Unit,
) : RecyclerView.Adapter<TrackListAdapter.ViewHolder>() {

    class ViewHolder(private val item: View, private val goToCurrentTrack: (Track) -> Unit) :
        RecyclerView.ViewHolder(item) {
        private val beginTime: TextView = item.findViewById(R.id.begin_time)
        private val time: TextView = item.findViewById(R.id.time_running)
        private val distance: TextView = item.findViewById(R.id.distance)

        fun bind(track: Track) {
            beginTime.text = track.beginTime.toString()
            val date = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
            time.text = date.format(Date(track.time))
            distance.text = track.distance.toString()
            item.setOnClickListener {
                goToCurrentTrack(track)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.track_recycler_item, parent, false)
        return ViewHolder(view, goToCurrentTrack = goToCurrentTrack)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listOfTracks[position])
    }

    override fun getItemCount(): Int = listOfTracks.size
}