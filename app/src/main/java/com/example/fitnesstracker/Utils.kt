package com.example.fitnesstracker

import android.annotation.SuppressLint
import android.os.Handler
import android.os.SystemClock
import android.widget.TextView
import java.util.*
import java.util.concurrent.Executor

class Utils {

    companion object {
        private const val FORMAT = "%02d"
    }

    private var tMilliSec = 0L
    private var tBuff = 0L
    private var tUpdate = 0L
    private var sec = 0
    private var min = 0
    private var millis = 0
    private var hours = 0
    private val handler = Handler()

    fun createTimer(view: TextView, tStart: Long, calendar: Calendar) = object : Runnable {

        override fun run() {
            calculateTime(tStart)
            setCalendarTimeForTimer(calendar)
            view.text = createString()
            handler.postDelayed(this, 40)
        }
    }

    private fun formatString(time: Int) = String.format(
        FORMAT,
        time
    )

    private fun createString() = "${formatString(hours)}: ${formatString(min)}: ${formatString(sec)}: ${
        formatString(millis)
    }"

    private fun setCalendarTimeForTimer(calendar: Calendar) {
        calendar[Calendar.HOUR_OF_DAY] = hours
        calendar[Calendar.MINUTE] = min
        calendar[Calendar.SECOND] = sec
        calendar[Calendar.MILLISECOND] = millis
    }

    private fun calculateTime(tStart: Long) {
        tMilliSec = SystemClock.elapsedRealtime() - tStart
        tUpdate = tBuff + tMilliSec
        sec = (tUpdate / 1000).toInt()
        min = sec / 60
        hours = sec / 3600
        sec %= 60
        millis = (tUpdate % 100).toInt()
    }
}