package com.example.fitnesstracker.screens.main.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.tracks.TrackForData
import com.example.fitnesstracker.models.tracks.Tracks
import java.text.SimpleDateFormat
import java.util.*

class TrackListAdapter(
    private val listOfTrackForData: MutableList<Tracks>,
    private val goToCurrentTrack: (Tracks) -> Unit,
) : RecyclerView.Adapter<TrackListAdapter.ViewHolder>() {

    class ViewHolder(private val item: View, private val goToCurrentTrack: (Tracks) -> Unit) :
        RecyclerView.ViewHolder(item) {
        private val beginTime: TextView = item.findViewById(R.id.begin_time)
        private val time: TextView = item.findViewById(R.id.time_running)
        private val distance: TextView = item.findViewById(R.id.distance)

        fun bind(trackForData: Tracks) {
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val currentTimeFormat = SimpleDateFormat("HH:mm:ss,SS", Locale.getDefault())
            val timeZone = SimpleTimeZone.getTimeZone("UTC")
            currentTimeFormat.timeZone = timeZone
            beginTime.text = date.format(trackForData.beginTime)
            time.text = currentTimeFormat.format(trackForData.time)
            distance.text = trackForData.distance.toString()
            item.setOnClickListener {
                goToCurrentTrack(trackForData)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.track_recycler_item, parent, false)
        return ViewHolder(view, goToCurrentTrack = goToCurrentTrack)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listOfTrackForData[position])
    }

    override fun getItemCount(): Int = listOfTrackForData.size
}