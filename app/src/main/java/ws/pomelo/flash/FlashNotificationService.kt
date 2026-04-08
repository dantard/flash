package ws.pomelo.flash

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.*

class FlashNotificationService : NotificationListenerService() {
    private lateinit var flashManager: FlashManager

    override fun onCreate() {
        super.onCreate()
        flashManager = FlashManager(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)
        val configIds = prefs.getStringSet("config_ids", emptySet()) ?: emptySet()

        // Get notification content for filtering
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE, "") ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString()
        val fullContent = "$title $text".lowercase()

        val packageConfigs = mutableListOf<FlashConfigData>()

        for (id in configIds) {
            val configPackage = prefs.getString("${id}_package", "")
            if (configPackage == pkg && prefs.getBoolean("${id}_enabled", true)) {
                packageConfigs.add(FlashConfigData(
                    id = id,
                    filter = prefs.getString("${id}_filter", "")?.lowercase() ?: "",
                    pattern = prefs.getString("${id}_pattern", "200,200") ?: "200,200",
                    start = prefs.getString("${id}_start_time", "00:00") ?: "00:00",
                    end = prefs.getString("${id}_end_time", "23:59") ?: "23:59",
                    useFlash = prefs.getBoolean("${id}_use_flash", true),
                    useVibration = prefs.getBoolean("${id}_use_vibration", false),
                    useSound = prefs.getBoolean("${id}_use_sound", false)
                ))
            }
        }

        if (packageConfigs.isEmpty()) return

        // 1. Try to find a specific filter match
        // 2. Fallback to the "empty" filter (Any) if no specific match found
        val matchedConfig = packageConfigs.filter { it.filter.isNotEmpty() }
            .find { fullContent.contains(it.filter) }
            ?: packageConfigs.find { it.filter.isEmpty() }

        matchedConfig?.let {
            if (isCurrentTimeInRange(it.start, it.end)) {
                flashManager.playPattern(it.pattern, it.useFlash, it.useVibration, it.useSound)
            }
        }
    }

    private fun isCurrentTimeInRange(start: String, end: String): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMin = timeToMinutes(start)
        val endMin = timeToMinutes(end)

        return if (startMin <= endMin) {
            currentMinutes in startMin..endMin
        } else {
            // Overlap midnight (e.g., 22:00 to 07:00)
            currentMinutes >= startMin || currentMinutes <= endMin
        }
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size < 2) return 0
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }

    private data class FlashConfigData(
        val id: String,
        val filter: String,
        val pattern: String,
        val start: String,
        val end: String,
        val useFlash: Boolean,
        val useVibration: Boolean,
        val useSound: Boolean
    )
}
