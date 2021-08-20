package com.example.fitnesstracker.screens.running.calculate

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView
import java.util.*

class TimeCalculator {

    companion object {
        private const val FORMAT = "%02d"
        private const val MILLIS_IN_SEC = 1000
        private const val SEC_IN_MIN = 60
        private const val SEC_IN_HOUR = 3600
        private const val HANDLER_DELAY = 40L
        private const val SECONDS_CALCULATE = 60
        private const val MILLIS_CALCULATE = 100
    }

    private var tMilliSec = 0L
    private var tBuff = 0L
    private var tUpdate = 0L
    private var sec = 0
    private var min = 0
    private var millis = 0
    private var hours = 0
    private val handler = Handler(Looper.getMainLooper())
    private var view: TextView? = null

    fun setView(view: TextView?): TimeCalculator {
        this.view = view
        return this
    }

    fun createTimer(tStart: Long, calendar: Calendar) = object : Runnable {

        override fun run() {
            calculateTime(tStart)
            setCalendarTimeForTimer(calendar)
            view?.text = createString()
            handler.postDelayed(this, HANDLER_DELAY)
        }
    }

    fun clearView(){
        this.view = null
    }

    private fun formatString(time: Int) = String.format(
        FORMAT,
        time
    )

    private fun createString() =
        "${formatString(hours)}: ${formatString(min)}: ${formatString(sec)}: ${
            formatString(millis)
        }"

    private fun setCalendarTimeForTimer(calendar: Calendar) {
        calendar[Calendar.HOUR_OF_DAY] = hours
        calendar[Calendar.MINUTE] = min
        calendar[Calendar.SECOND] = sec
        calendar[Calendar.MILLISECOND] = millis
    }

    private fun calculateTime(tStart: Long) {
        tMilliSec = Calendar.getInstance().timeInMillis - tStart
        tUpdate = tBuff + tMilliSec
        sec = (tUpdate / MILLIS_IN_SEC).toInt()
        min = sec / SEC_IN_MIN
        hours = sec / SEC_IN_HOUR
        sec %= SECONDS_CALCULATE
        millis = (tUpdate % MILLIS_CALCULATE).toInt()
    }
}