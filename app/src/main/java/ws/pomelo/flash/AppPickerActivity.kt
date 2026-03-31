package ws.pomelo.flash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        val rv = findViewById<RecyclerView>(R.id.rvAppList)
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