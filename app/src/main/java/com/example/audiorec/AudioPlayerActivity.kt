package com.example.audiorec

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_audio_player.*
import java.io.*
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.ArrayDeque

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvFilename: TextView

    private lateinit var tvTrackProgress: TextView
    private lateinit var tvTrackDuration: TextView

    private lateinit var btnPlay: ImageButton
    private lateinit var btnBackward: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var speedChip: Chip

    private lateinit var amplitudes: ArrayDeque<Float>

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 100L
    private var jumpValue = 5000

    private var playbackSpeed = 1.0f

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        var filePath = intent.getStringExtra("filepath")
        var fileName = intent.getStringExtra("filename")
        var ampsPath = intent.getStringExtra("ampsPath")

        try {
            var fis = FileInputStream(ampsPath)
            var inStream = ObjectInputStream(fis)
            amplitudes = inStream.readUnshared() as ArrayDeque<Float>
            fis.close()
            inStream.close()
        } catch (e: IOException) {}

        toolbar = findViewById(R.id.toolbar)
        tvFilename = findViewById(R.id.tv_fileName)

        tvTrackProgress = findViewById(R.id.tv_track_progress)
        tvTrackDuration = findViewById(R.id.tv_track_duration)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        tvFilename.text = fileName
        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        tvTrackDuration.text = dateFormat(mediaPlayer.duration)

        btnBackward = findViewById(R.id.button_backward)
        btnForward = findViewById(R.id.button_forward)
        btnPlay = findViewById(R.id.button_play)
        speedChip = findViewById(R.id.chip)
        seekBar = findViewById(R.id.seekBar)

        handler = Handler(Looper.getMainLooper())

        runnable = Runnable {
            seekBar.progress = mediaPlayer.currentPosition
            tvTrackProgress.text = dateFormat(mediaPlayer.currentPosition)
            handler.postDelayed(runnable, delay)
            waveFormsRecordedVoiceView.addAmplitude(amplitudes.elementAt((mediaPlayer.currentPosition / delay).toInt()))
        }

        btnPlay.setOnClickListener {
            playPausePlayer()
        }

        seekBar.max = mediaPlayer.duration
        playPausePlayer()

        mediaPlayer.setOnCompletionListener {
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_audio_play, theme)
            seekBar.progress = mediaPlayer.currentPosition
            handler.removeCallbacks(runnable)
            waveFormsRecordedVoiceView.clear()
        }

        button_forward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
            seekBar.progress += jumpValue
        }
        button_backward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
            seekBar.progress -= jumpValue
        }

        chip.setOnClickListener {
            if (playbackSpeed != 2f) {
                playbackSpeed += 0.5f
            }
            else {
                playbackSpeed = 0.5f
            }

            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            chip.text = "x $playbackSpeed"
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) {
                    mediaPlayer.seekTo(p1)
                    waveFormsRecordedVoiceView.clear()
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })
    }

    private fun playPausePlayer() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_audio_pause, theme)
            handler.postDelayed(runnable, 0)
        } else {
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_audio_play, theme)
            handler.removeCallbacks(runnable)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }

    private fun dateFormat(duration: Int): String {
        var d = duration / 1000
        var sec = d % 60
        var min = (d / 60) % 60
        var h = d / 3600

        val f: NumberFormat = DecimalFormat("00")
        var str = "$min:${f.format(sec)}"

        if (h > 0) {
            str = "$h:$str"
        }

        return str
    }
}