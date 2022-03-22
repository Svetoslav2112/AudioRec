package com.example.audiorec

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private var recordingStopped: Boolean? = false

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        output = Environment.getExternalStorageDirectory().absolutePath + "/recording.mp3"
        mediaRecorder = MediaRecorder()

        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)

        val btnStart: Button = findViewById(R.id.button_start_recording)
        val btnPause: Button = findViewById(R.id.button_pause_recording)
        val btnStop: Button = findViewById(R.id.button_stop_recording)

        btnStart.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {

                val permissions = arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions, 0)
            } else {
                startRecording()
            }
        }
        btnPause.setOnClickListener { pauseRecording() }
        btnStop.setOnClickListener { stopRecording() }
    }

    private fun startRecording() {
        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
            Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun pauseRecording() {
        if (state) {
            if (!recordingStopped!!) {
                Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
                mediaRecorder?.pause()
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
        mediaRecorder?.resume()
        val btnPause: Button = findViewById(R.id.button_pause_recording)
        btnPause.text = "Pause"
        recordingStopped = true
    }

    private fun stopRecording() {
        if (state) {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            state = false
        } else {
            Toast.makeText(this, "You are not recording right now.", Toast.LENGTH_SHORT).show()
        }
    }
}