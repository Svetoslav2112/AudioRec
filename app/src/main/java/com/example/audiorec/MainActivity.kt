package com.example.audiorec

import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.appsearch.GlobalSearchSession
import android.content.Context
import android.media.MediaRecorder
import android.os.*
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {
    private var filename: String = ""
    private var directoryPath: String = ""
    private var isPaused: Boolean = false
    private var isRecording: Boolean = false
    private var permissionsGranted: Boolean = false
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var amplitudes: ArrayDeque<Float>

    private var duration = ""
    private lateinit var timer: Timer
    private lateinit var vibrator: Vibrator

    private lateinit var database: AppDatabase

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

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

        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_layout)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        button_record.setOnClickListener {
            when {
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        button_recordings_list.setOnClickListener {
            //TODO
            Toast.makeText(this, "List button", Toast.LENGTH_SHORT).show()

        }
        button_save_recording.setOnClickListener {
            stopRecording()
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottom_sheet_background.visibility = View.VISIBLE
            filename_input.setText(filename)
        }
        button_cancel.setOnClickListener {
            File("$directoryPath$filename.mp3").delete()
            dismiss()
        }
        button_save.setOnClickListener {
            dismiss()
            save()
        }
        bottom_sheet_background.setOnClickListener {
            File("$directoryPath$filename.mp3").delete()
            dismiss()
        }
        button_cancel_recording.setOnClickListener {
            stopRecording()
            File("$directoryPath$filename.mp3")
            Toast.makeText(this, "Recording canceled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun save() {
        val newFilename = filename_input.text.toString()

        if (newFilename != filename) {
            var newFile = File("$directoryPath$newFilename.mp3")
            File("$directoryPath$filename.mp3").renameTo(newFile)
        }

        var filePath = "$directoryPath$newFilename.mp3"
        var timestamp = Date().time
        var ampsPath = "$directoryPath$newFilename"

        try {
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        } catch (e: IOException) {}

        var record = AudioRecord(newFilename, filePath, timestamp, duration, ampsPath)

        GlobalScope.launch {
            database.audioRecordDao().insert(record)
        }
    }

    private fun dismiss() {
        bottom_sheet_background.visibility = View.GONE
        hideKeyboard(filename_input)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
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

            button_cancel_recording.isClickable = true
            button_recordings_list.visibility = View.GONE
            button_save_recording.visibility = View.VISIBLE
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
        mediaRecorder.apply {
            stop()
            release()
        }
        isPaused = false
        isRecording = false

        button_recordings_list.visibility = View.VISIBLE
        button_save_recording.visibility = View.GONE

        button_cancel_recording.isClickable = false
        //button_cancel_recording.setImageResource(R.drawable.)
        button_record.setImageResource(R.drawable.ic_btn_play)

        recording_timer.text = "00:00.0"
        amplitudes = waveFormsVoiceView.clear()
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onTimerTick(duration: String) {
        recording_timer.text = duration
        this.duration = duration.dropLast(3)
        waveFormsVoiceView.addAmplitude(mediaRecorder.maxAmplitude.toFloat())
    }

}