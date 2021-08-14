package com.example.fitnesstracker.screens.main.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.App.Companion.PATTERN_WITHOUT_SECONDS
import com.example.fitnesstracker.App.Companion.PATTERN_WITH_SECONDS
import com.example.fitnesstracker.App.Companion.UTC
import com.example.fitnesstracker.R
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
        private var date: SimpleDateFormat? = null
        private var currentTimeFormat: SimpleDateFormat? = null
        private var timeZone: TimeZone? = null

        fun bind(trackForData: Tracks) {
            date = SimpleDateFormat(PATTERN_WITHOUT_SECONDS, Locale.getDefault())
            currentTimeFormat = SimpleDateFormat(PATTERN_WITH_SECONDS, Locale.getDefault())
            timeZone = SimpleTimeZone.getTimeZone(UTC)
            currentTimeFormat?.timeZone = timeZone!!
            beginTime.text = date?.format(trackForData.beginTime)
            time.text = currentTimeFormat?.format(trackForData.time)
            distance.text = trackForData.distance.toString()
            item.setOnClickListener {
                goToCurrentTrack(trackForData)
            }
        }

        fun unbind() {
            item.setOnClickListener(null)
            date = null
            currentTimeFormat = null
            timeZone = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.track_recycler_item, parent, false
        )
        return ViewHolder(item = view, goToCurrentTrack = goToCurrentTrack)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(trackForData = listOfTrackForData[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun getItemCount(): Int = listOfTrackForData.size
}