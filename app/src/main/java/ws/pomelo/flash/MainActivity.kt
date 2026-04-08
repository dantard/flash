package ws.pomelo.flash

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FlashConfigAdapter
    private val configs = mutableListOf<FlashConfig>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        }
    }

    private val startAppPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val packageName = result.data?.getStringExtra("PACKAGE_NAME")
            if (packageName != null) {
                openConfigEditor(packageName, null)
            }
        }
    }

    private val startConfigEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadConfigs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<ViewGroup>(R.id.main_root)
        val rvConfigs = findViewById<RecyclerView>(R.id.rvConfigs)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            rvConfigs.updatePadding(
                top = systemBars.top + 8.dpToPx(),
                bottom = systemBars.bottom + 88.dpToPx(),
                left = systemBars.left + 8.dpToPx(),
                right = systemBars.right + 8.dpToPx()
            )
            
            fabAdd.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 24.dpToPx()
                rightMargin = systemBars.right + 24.dpToPx()
            }
            
            insets
        }

        rvConfigs.layoutManager = LinearLayoutManager(this)
        
        adapter = FlashConfigAdapter(configs, { config, isEnabled ->
            config.isEnabled = isEnabled
            saveEnabledState(config.id, isEnabled)
        }, { config ->
            openConfigEditor(config.packageName, config.id)
        })
        rvConfigs.adapter = adapter

        fabAdd.setOnClickListener {
            val intent = Intent(this, AppPickerActivity::class.java)
            startAppPicker.launch(intent)
        }

        loadConfigs()
        checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        if (!isNotificationServiceEnabled()) {
            showNotificationPermissionDialog()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat != null && flat.isNotEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    return true
                }
            }
        }
        return false
    }

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Notification Access Required")
            .setMessage("To notify when you receive a message, this app needs access to your notifications. Please enable it in the next screen.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun loadConfigs() {
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        val configIds = prefs.getStringSet("config_ids", emptySet()) ?: emptySet()
        val pm = packageManager

        configs.clear()
        for (id in configIds) {
            val pkg = prefs.getString("${id}_package", null) ?: continue
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name = appInfo.loadLabel(pm).toString()
                val icon = appInfo.loadIcon(pm)
                val filterText = prefs.getString("${id}_filter", "") ?: ""
                val pattern = prefs.getString("${id}_pattern", "200,200") ?: "200,200"
                val startTime = prefs.getString("${id}_start_time", "00:00") ?: "00:00"
                val endTime = prefs.getString("${id}_end_time", "23:59") ?: "23:59"
                val isEnabled = prefs.getBoolean("${id}_enabled", true)
                val useFlash = prefs.getBoolean("${id}_use_flash", true)
                val useVibration = prefs.getBoolean("${id}_use_vibration", false)
                val useSound = prefs.getBoolean("${id}_use_sound", false)

                configs.add(FlashConfig(id, pkg, name, icon, pattern, filterText, startTime, endTime, isEnabled, useFlash, useVibration, useSound))
            } catch (e: PackageManager.NameNotFoundException) {
                // App uninstalled
            }
        }
        adapter.updateData(configs)
    }

    private fun saveEnabledState(configId: String, isEnabled: Boolean) {
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${configId}_enabled", isEnabled).apply()
    }

    private fun openConfigEditor(packageName: String, configId: String?) {
        val intent = Intent(this, ConfigEditorActivity::class.java)
        intent.putExtra("PACKAGE_NAME", packageName)
        intent.putExtra("CONFIG_ID", configId)
        startConfigEditor.launch(intent)
    }
}
