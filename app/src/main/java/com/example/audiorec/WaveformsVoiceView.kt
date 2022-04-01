package com.example.audiorec

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class WaveformsVoiceView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()

    init {
        paint.color = Color.rgb(244, 81, 30)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas?.drawRoundRect(RectF(20f, 30f, 20+30f, 20+30f), 6f, 6f, paint)
    }
}