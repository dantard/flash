package ws.pomelo.flash

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class ConfigEditorActivity : AppCompatActivity() {

    private lateinit var packageName: String
    private var startTime = "00:00"
    private var endTime = "23:59"

    private lateinit var etPattern: EditText

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

        packageName = intent.getStringExtra("PACKAGE_NAME") ?: return finish()
        
        val ivIcon = findViewById<ImageView>(R.id.ivAppIcon)
        val tvName = findViewById<TextView>(R.id.tvAppName)
        val tvPackage = findViewById<TextView>(R.id.tvPackageName)
        etPattern = findViewById(R.id.etPattern)
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
        etPattern.setText(prefs.getString("${packageName}_pattern", "200,200"))
        startTime = prefs.getString("${packageName}_start_time", "00:00") ?: "00:00"
        endTime = prefs.getString("${packageName}_end_time", "23:59") ?: "23:59"

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
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        val pattern = etPattern.text.toString()
        
        val configuredPackages = prefs.getStringSet("configured_packages", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        configuredPackages.add(packageName)

        prefs.edit().apply {
            putStringSet("configured_packages", configuredPackages)
            putString("${packageName}_pattern", pattern)
            putString("${packageName}_start_time", startTime)
            putString("${packageName}_end_time", endTime)
            putBoolean("${packageName}_enabled", true)
            apply()
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun deleteConfig() {
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        val configuredPackages = prefs.getStringSet("configured_packages", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        configuredPackages.remove(packageName)
        
        prefs.edit().apply {
            putStringSet("configured_packages", configuredPackages)
            remove("${packageName}_pattern")
            remove("${packageName}_start_time")
            remove("${packageName}_end_time")
            remove("${packageName}_enabled")
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
