package ws.pomelo.flash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FlashConfigAdapter
    private val configs = mutableListOf<FlashConfig>()

    private val startAppPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val packageName = result.data?.getStringExtra("PACKAGE_NAME")
            if (packageName != null) {
                openConfigEditor(packageName)
            }
        }
    }

    private val startConfigEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadConfigs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rvConfigs = findViewById<RecyclerView>(R.id.rvConfigs)
        rvConfigs.layoutManager = LinearLayoutManager(this)
        
        adapter = FlashConfigAdapter(configs, { config, isEnabled ->
            saveEnabledState(config.packageName, isEnabled)
        }, { config ->
            openConfigEditor(config.packageName)
        })
        rvConfigs.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val intent = Intent(this, AppPickerActivity::class.java)
            startAppPicker.launch(intent)
        }

        loadConfigs()
    }

    private fun loadConfigs() {
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        val packageList = prefs.getStringSet("configured_packages", emptySet()) ?: emptySet()
        val pm = packageManager

        configs.clear()
        for (pkg in packageList) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name = appInfo.loadLabel(pm).toString()
                val icon = appInfo.loadIcon(pm)
                val pattern = prefs.getString("${pkg}_pattern", "200,200") ?: "200,200"
                val intensity = prefs.getInt("${pkg}_intensity", 10)
                val startTime = prefs.getString("${pkg}_start_time", "00:00") ?: "00:00"
                val endTime = prefs.getString("${pkg}_end_time", "23:59") ?: "23:59"
                val isEnabled = prefs.getBoolean("${pkg}_enabled", true)

                configs.add(FlashConfig(pkg, name, icon, pattern, intensity, startTime, endTime, isEnabled))
            } catch (e: PackageManager.NameNotFoundException) {
                // App uninstalled?
            }
        }
        adapter.updateData(configs)
    }

    private fun saveEnabledState(packageName: String, isEnabled: Boolean) {
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${packageName}_enabled", isEnabled).apply()
    }

    private fun openConfigEditor(packageName: String) {
        val intent = Intent(this, ConfigEditorActivity::class.java)
        intent.putExtra("PACKAGE_NAME", packageName)
        startConfigEditor.launch(intent)
    }
}
