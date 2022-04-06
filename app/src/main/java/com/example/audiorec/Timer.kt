package com.example.audiorec

import android.os.Handler
import android.os.Looper
import kotlin.math.min

class Timer(listener: OnTimerTickListener) {
    interface OnTimerTickListener {
        fun onTimerTick(duration: String)
    }

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var duration: Long = 0L
    private var delay: Long = 100L

    init {
        runnable = Runnable {
            duration += delay
            handler.postDelayed(runnable, delay)
            listener.onTimerTick(format())
        }
    }

    fun start() {
        handler.postDelayed(runnable, delay)
    }

    fun pause() {
        handler.removeCallbacks(runnable)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
        duration = 0L

    }

    fun format(): String {
        val milliseconds = (duration % 1000) / 100
        val seconds = (duration / 1000 ) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = duration / (1000 * 60 * 60)

        var formatted =
            if (hours > 0)
                "%02d:%02d:%02d.%d".format(hours, minutes, seconds, milliseconds)
            else
                "%02d:%02d.%d".format(minutes, seconds, milliseconds)

        return formatted
    }
}