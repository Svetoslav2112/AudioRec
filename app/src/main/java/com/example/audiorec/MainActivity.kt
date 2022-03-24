package com.example.audiorec

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var output: String = ""
    private var state: Boolean = false
    private lateinit var mediaRecorder: MediaRecorder
    private var recordingStopped: Boolean = false

    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private var permissionsGranted: Boolean = false
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {   permissions ->
        if (permissions.all { it.value }) {
            permissionsGranted = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.button_start_recording)
        btnPause = findViewById(R.id.button_pause_recording)
        btnStop = findViewById(R.id.button_stop_recording)

        btnStart.setOnClickListener { startRecording() }
        btnPause.setOnClickListener { pauseRecording() }
        btnStop.setOnClickListener { stopRecording() }
    }

    private fun startRecording() {
        if (!permissionsGranted) {
            requestPermissions.launch(permissions)
        }
        if (permissionsGranted) {
            output = Environment.getExternalStorageDirectory().absolutePath + "/myRecording.mp3"

            mediaRecorder = MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setOutputFile(output)

            try {
                mediaRecorder.prepare()
                mediaRecorder.start()
                state = true
                Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun pauseRecording() {
        if (state) {
            if (!recordingStopped) {
                Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
                mediaRecorder.pause()
                recordingStopped = true
                val btnPause: Button = findViewById(R.id.button_pause_recording)
                btnPause.text = "Resume"
            } else {
                resumeRecording()
            }
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun resumeRecording() {
        Toast.makeText(this, "Resumed", Toast.LENGTH_SHORT).show()
        mediaRecorder.resume()
        val btnPause: Button = findViewById(R.id.button_pause_recording)
        btnPause.text = "Pause"
        recordingStopped = true
    }

    private fun stopRecording() {
        if (state) {
            mediaRecorder.stop()
            mediaRecorder.release()
            state = false
        } else {
            Toast.makeText(this, "You are not recording right now.", Toast.LENGTH_SHORT).show()
        }
    }
}