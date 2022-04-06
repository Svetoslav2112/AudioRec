package com.example.audiorec

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class WaveformsVoiceView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()
    private var amplitudes = ArrayDeque<Float>()
    private var spikes = ArrayList<RectF>()

    private var radius = 6f
    private var spikeWidth = 9f
    private var distance = 6f

    private var screenWidth = 0f
    private var screenMid = 0f
    private var screenHeight = 400f

    private var maxSpikes = 0

    init {
        paint.color = Color.rgb(244, 81, 30)
        screenWidth = resources.displayMetrics.widthPixels.toFloat()
        screenMid = screenWidth / 2 + spikeWidth / 2
        maxSpikes = (screenMid / (spikeWidth + distance)).toInt()
    }

    fun addAmplitude(amplitude: Float) {
        var normalized = Math.min(amplitude.toInt() / 25, screenHeight.toInt()).toFloat()
        amplitudes.addFirst(normalized)

        spikes.clear()
        var amps = amplitudes.take(maxSpikes)

        for (i in amps.indices) {
            var descaleCoefficient = i * 4
            var amp = amps[i] / 2
            var descaledAmplitude = Math.max(amp - descaleCoefficient, 2f)

            var left1 = screenMid - i * (spikeWidth + distance)
            var right1 = left1 - spikeWidth
            var top1 = screenHeight / 2 -  descaledAmplitude
            var bottom1 = screenHeight / 2 + descaledAmplitude

            var left2 = screenMid + i * (spikeWidth + distance)
            var right2 = left2 + spikeWidth
            var top2 = screenHeight / 2 -  descaledAmplitude
            var bottom2 = screenHeight / 2 + descaledAmplitude

            spikes.add(RectF(left1, top1, right1, bottom1))
            spikes.add(RectF(left2, top2, right2, bottom2))

        }

        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        spikes.forEach() {
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
    }
}