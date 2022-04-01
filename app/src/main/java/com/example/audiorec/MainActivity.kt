package com.example.audiorec

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {
    private var filename: String = ""
    private var directoryPath: String = ""
    private var isPaused: Boolean = false
    private var isRecording: Boolean = false
    private var permissionsGranted: Boolean = false
    private lateinit var mediaRecorder: MediaRecorder

    private lateinit var timer: Timer
    private lateinit var vibrator: Vibrator

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {   permissions ->
        if (permissions.all { it.value }) {
            permissionsGranted = true
            startRecording()
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mediaRecorder = MediaRecorder()

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


        button_record.setOnClickListener {
            when {
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        button_cancel_recording.setOnClickListener { stopRecording() }
    }

    private fun startRecording() {
        if (!permissionsGranted) {
            requestPermissions.launch(permissions)
        }
        else {
            directoryPath = "${externalCacheDir?.absolutePath}/"

            var simpleDateFormat = SimpleDateFormat("yyyy.mm.dd_hh.mm.ss")
            var date = simpleDateFormat.format(Date())
            filename = "audio_record_$date"

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile("$directoryPath$filename.mp3")
                try {
                    prepare()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                start()
            }
            button_record.setImageResource(R.drawable.ic_btn_pause)
            isRecording = true
            isPaused = false
            timer.start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pauseRecording() {
        mediaRecorder.pause()
        isPaused = true
        isRecording = false
        button_record.setImageResource(R.drawable.ic_btn_play)

        timer.pause()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun resumeRecording() {
        mediaRecorder.resume()
        isPaused = false
        isRecording = true
        button_record.setImageResource(R.drawable.ic_btn_pause)

        timer.start()
    }

    private fun stopRecording() {
        timer.stop()
    }

    override fun onTimerTick(duration: String) {
        recording_timer.text = duration
    }

}