package ws.pomelo.flash

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.UUID

class ConfigEditorActivity : AppCompatActivity() {

    private var configId: String? = null
    private lateinit var packageName: String
    private var startTime = "00:00"
    private var endTime = "23:59"

    private lateinit var etFilter: EditText
    private lateinit var etPattern: EditText
    private lateinit var cbFlash: CheckBox
    private lateinit var cbVibration: CheckBox
    private lateinit var cbSound: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_editor)

        val root = findViewById<ViewGroup>(R.id.editor_root)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                bottom = systemBars.bottom
            )
            toolbar.updatePadding(top = systemBars.top)
            insets
        }

        configId = intent.getStringExtra("CONFIG_ID")
        packageName = intent.getStringExtra("PACKAGE_NAME") ?: return finish()
        
        val ivIcon = findViewById<ImageView>(R.id.ivAppIcon)
        val tvName = findViewById<TextView>(R.id.tvAppName)
        val tvPackage = findViewById<TextView>(R.id.tvPackageName)
        etFilter = findViewById(R.id.etFilter)
        etPattern = findViewById(R.id.etPattern)
        cbFlash = findViewById(R.id.cbFlash)
        cbVibration = findViewById(R.id.cbVibration)
        cbSound = findViewById(R.id.cbSound)
        
        val btnStart = findViewById<Button>(R.id.btnStartTime)
        val btnEnd = findViewById<Button>(R.id.btnEndTime)

        val pm = packageManager
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            ivIcon.setImageDrawable(appInfo.loadIcon(pm))
            tvName.text = appInfo.loadLabel(pm).toString()
            tvPackage.text = packageName
        } catch (e: PackageManager.NameNotFoundException) {
            finish()
        }

        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        if (configId != null) {
            etFilter.setText(prefs.getString("${configId}_filter", ""))
            etPattern.setText(prefs.getString("${configId}_pattern", "200,200"))
            startTime = prefs.getString("${configId}_start_time", "00:00") ?: "00:00"
            endTime = prefs.getString("${configId}_end_time", "23:59") ?: "23:59"
            cbFlash.isChecked = prefs.getBoolean("${configId}_use_flash", true)
            cbVibration.isChecked = prefs.getBoolean("${configId}_use_vibration", false)
            cbSound.isChecked = prefs.getBoolean("${configId}_use_sound", false)
        } else {
            etPattern.setText("200,200")
            cbFlash.isChecked = true
            cbVibration.isChecked = false
            cbSound.isChecked = false
        }

        btnStart.text = "Start: $startTime"
        btnEnd.text = "End: $endTime"

        btnStart.setOnClickListener {
            showTimePicker(startTime) { time ->
                startTime = time
                btnStart.text = "Start: $startTime"
            }
        }

        btnEnd.setOnClickListener {
            showTimePicker(endTime) { time ->
                endTime = time
                btnEnd.text = "End: $endTime"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_config_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveConfig()
                true
            }
            R.id.action_delete -> {
                deleteConfig()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveConfig() {
        if (!cbFlash.isChecked && !cbVibration.isChecked && !cbSound.isChecked) {
            Toast.makeText(this, "Please select at least one notification method", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        val filter = etFilter.text.toString()
        val pattern = etPattern.text.toString()
        
        val id = configId ?: UUID.randomUUID().toString()
        
        val configIds = prefs.getStringSet("config_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        configIds.add(id)

        prefs.edit().apply {
            putStringSet("config_ids", configIds)
            putString("${id}_package", packageName)
            putString("${id}_filter", filter)
            putString("${id}_pattern", pattern)
            putString("${id}_start_time", startTime)
            putString("${id}_end_time", endTime)
            putBoolean("${id}_use_flash", cbFlash.isChecked)
            putBoolean("${id}_use_vibration", cbVibration.isChecked)
            putBoolean("${id}_use_sound", cbSound.isChecked)
            if (configId == null) {
                putBoolean("${id}_enabled", true)
            }
            apply()
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun deleteConfig() {
        if (configId == null) return finish()
        
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        val configIds = prefs.getStringSet("config_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        configIds.remove(configId)
        
        prefs.edit().apply {
            putStringSet("config_ids", configIds)
            remove("${configId}_package")
            remove("${configId}_filter")
            remove("${configId}_pattern")
            remove("${configId}_start_time")
            remove("${configId}_end_time")
            remove("${configId}_enabled")
            remove("${configId}_use_flash")
            remove("${configId}_use_vibration")
            remove("${configId}_use_sound")
            apply()
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        TimePickerDialog(this, { _, h, m ->
            onTimeSelected(String.format("%02d:%02d", h, m))
        }, hour, minute, true).show()
    }
}
