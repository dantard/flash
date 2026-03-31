package ws.pomelo.flash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout

class AppPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        val root = findViewById<ViewGroup>(R.id.picker_root)
        val appBarLayout = findViewById<AppBarLayout>(R.id.appBarLayout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvAppList)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                bottom = systemBars.bottom
            )
            appBarLayout.updatePadding(top = systemBars.top)
            insets
        }

        val apps = getInstalledApps()

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = AppAdapter(apps) { selectedApp ->
            // Prepare the result to send back
            val resultIntent = Intent()
            resultIntent.putExtra("PACKAGE_NAME", selectedApp.packageName)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Close this activity
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppInfo(it.loadLabel(pm).toString(), it.packageName, it.loadIcon(pm)) }
            .sortedBy { it.name }
    }
}
