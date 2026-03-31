package ws.pomelo.flash

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConfigEditorActivity : AppCompatActivity() {

    private lateinit var packageName: String
    private var startTime = "00:00"
    private var endTime = "23:59"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_editor)

        packageName = intent.getStringExtra("PACKAGE_NAME") ?: return finish()
        
        val ivIcon = findViewById<ImageView>(R.id.ivAppIcon)
        val tvName = findViewById<TextView>(R.id.tvAppName)
        val tvPackage = findViewById<TextView>(R.id.tvPackageName)
        val etPattern = findViewById<EditText>(R.id.etPattern)
        val sbIntensity = findViewById<SeekBar>(R.id.sbIntensity)
        val btnStart = findViewById<Button>(R.id.btnStartTime)
        val btnEnd = findViewById<Button>(R.id.btnEndTime)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

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
        sbIntensity.progress = prefs.getInt("${packageName}_intensity", 10)
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

        btnSave.setOnClickListener {
            val pattern = etPattern.text.toString()
            val intensity = sbIntensity.progress
            
            val configuredPackages = prefs.getStringSet("configured_packages", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            configuredPackages.add(packageName)

            prefs.edit().apply {
                putStringSet("configured_packages", configuredPackages)
                putString("${packageName}_pattern", pattern)
                putInt("${packageName}_intensity", intensity)
                putString("${packageName}_start_time", startTime)
                putString("${packageName}_end_time", endTime)
                putBoolean("${packageName}_enabled", true)
                apply()
            }
            setResult(Activity.RESULT_OK)
            finish()
        }

        btnDelete.setOnClickListener {
            val configuredPackages = prefs.getStringSet("configured_packages", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            configuredPackages.remove(packageName)
            
            prefs.edit().apply {
                putStringSet("configured_packages", configuredPackages)
                remove("${packageName}_pattern")
                remove("${packageName}_intensity")
                remove("${packageName}_start_time")
                remove("${packageName}_end_time")
                remove("${packageName}_enabled")
                apply()
            }
            setResult(Activity.RESULT_OK)
            finish()
        }
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
