package com.example.audiorec

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.activity_gallery.bottom_sheet_background
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.rename_record_bottom_sheet.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.security.AccessController.getContext
import java.util.*
import kotlin.collections.ArrayList

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db: AppDatabase

    private var allChecked = false

    private lateinit var toolbar: MaterialToolbar

    private lateinit var editBar: View
    private lateinit var btnClose: ImageButton
    private lateinit var btnSelectAll: ImageButton
    private lateinit var btnRename: ImageButton
    private lateinit var btnDelete: ImageButton

    private lateinit var database: AppDatabase

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private var checkedRecords = 0

    private lateinit var searchInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).allowMainThreadQueries().build()

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_rename_record_layout)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        editBar = findViewById(R.id.edit_bar)
        btnClose = findViewById(R.id.button_close)
        btnSelectAll = findViewById(R.id.select_all)
        btnRename = findViewById(R.id.btn_rename)
        btnDelete = findViewById(R.id.btn_delete)

        records = ArrayList()
        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()
        mAdapter = Adapter(records, this)

        recyclerview.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
        }

        fetchAll()

        searchInput = findViewById(R.id.search_input)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                var query = p0.toString()
                searchDatabase(query)
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        btnClose.setOnClickListener {
            closeRecordSelection()
        }

        btnSelectAll.setOnClickListener {
            allChecked = !allChecked
            records.map {
                it.isChecked = allChecked
            }
            if (allChecked) {
                checkedRecords = records.size
            } else {
                checkedRecords = 0
            }
            if (checkedRecords == 1) {
                btnRename.isClickable = true
                btnRename.setImageResource(R.drawable.ic_rename)
            } else {
                btnRename.isClickable = false
                btnRename.setImageResource(R.drawable.ic_rename_disabled)
            }
            btnRename.isClickable = checkedRecords == 1
            mAdapter.notifyDataSetChanged()
        }

        btnRename.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottom_sheet_rename_record_layout.visibility = View.VISIBLE
            filename_rename_input.setText(records[indexOfCheckedRecord()].filename)
        }

        btnDelete.setOnClickListener {
            deleteRecords()
        }

        bottom_sheet_rename_record_layout.setOnClickListener {
            dismiss()
        }

        button_rename.setOnClickListener {
            dismiss()
            save(indexOfCheckedRecord())
        }

        button_rename_cancel.setOnClickListener {
            dismiss()
        }
    }

    private fun closeRecordSelection() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        editBar.visibility = View.GONE
        btnDelete.visibility = View.GONE
        btnRename.visibility = View.GONE
        records.map { it.isChecked = false }
        checkedRecords = 0
        mAdapter.setEditMode(false)
    }

    private fun searchDatabase(query: String) {
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().searchDatabase("%$query%")
            records.addAll(queryResult)

            runOnUiThread {
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchAll() {
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().getAll()
            records.addAll(queryResult)

            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onItemClickListener(position: Int) {
        var audioRecord = records[position]
        var intent = Intent(this, AudioPlayerActivity::class.java)

        if (mAdapter.isEditMode()) {
            records[position].isChecked = !records[position].isChecked
            if (records[position].isChecked) {
                checkedRecords++
            } else {
                checkedRecords--
            }
            if (checkedRecords == 1) {
                btnRename.isClickable = true
                btnRename.setImageResource(R.drawable.ic_rename)
            } else {
                btnRename.isClickable = false
                btnRename.setImageResource(R.drawable.ic_rename_disabled)
            }
            mAdapter.notifyItemChanged(position)
        } else {
            intent.putExtra("filepath", audioRecord.filePath)
            intent.putExtra("filename", audioRecord.filename)
            intent.putExtra("ampsPath", audioRecord.ampsPath)
            startActivity(intent)
        }
    }

    override fun onItemLongClickListener(position: Int) {
        mAdapter.setEditMode(true)
        records[position].isChecked = !records[position].isChecked
        mAdapter.notifyItemChanged(position)

        btnDelete.visibility = View.VISIBLE
        btnRename.visibility = View.VISIBLE
        if (records[position].isChecked) {
            checkedRecords++
        } else {
            checkedRecords--
        }
        if (checkedRecords == 1) {
            btnRename.isClickable = true
            btnRename.setImageResource(R.drawable.ic_rename)
        } else {
            btnRename.isClickable = false
            btnRename.setImageResource(R.drawable.ic_rename_disabled)
        }

        if (mAdapter.isEditMode() && editBar.visibility == View.GONE) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)

            editBar.visibility = View.VISIBLE
        }
    }

    private fun indexOfCheckedRecord(): Int {
        return (records.indices)
            .first { i: Int ->  records[i].isChecked }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun dismiss() {
        bottom_sheet_background.visibility = View.GONE
        hideKeyboard(filename_rename_input)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    private fun save(position: Int) {
        val newFilename = filename_rename_input.text.toString()
        var recordDoesNotExist = database.audioRecordDao().selectByName(newFilename) == null
            if (recordDoesNotExist) {
                val directoryPath = "${externalCacheDir?.absolutePath}/"
                var ampsPath = "$directoryPath$newFilename"
                var filePath = "$directoryPath$newFilename.mp3"

                if (newFilename != records[position].filename) {
                    var newFile = File("$filePath")
                    File("${records[position].filePath}").renameTo(newFile)

                    var newAmpsFile = File("$ampsPath")
                    File("${records[position].ampsPath}").renameTo(newAmpsFile)
                }

                var record = AudioRecord(
                    newFilename,
                    filePath,
                    records[position].timestamp,
                    records[position].duration,
                    ampsPath,
                    records[position].id
                )

                GlobalScope.launch {
                    database.audioRecordDao().update(record)
                    fetchAll()
                }
                runOnUiThread {
                    closeRecordSelection()
                }
            } else {
                Toast.makeText( this, "Record with that name already exists", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteRecords() {
        records.filter { it.isChecked }.forEach {
            File(it.filePath).delete()
            File(it.ampsPath).delete()
        }
        GlobalScope.launch {
            database.audioRecordDao().delete(records.filter { it.isChecked }.toTypedArray())
            fetchAll()
        }
        runOnUiThread {
            closeRecordSelection()
        }
    }
}