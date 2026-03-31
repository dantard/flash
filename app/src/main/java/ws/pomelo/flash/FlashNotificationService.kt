package ws.pomelo.flash

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.*

class FlashNotificationService : NotificationListenerService() {
    private lateinit var flashManager: FlashManager
    private val seenNotifications = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        flashManager = FlashManager(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val prefs = getSharedPreferences("FlashPrefs", Context.MODE_PRIVATE)

        if (seenNotifications.contains(sbn.key)) return

        if (prefs.getBoolean("${pkg}_enabled", false)) {
            val startTime = prefs.getString("${pkg}_start_time", "00:00") ?: "00:00"
            val endTime = prefs.getString("${pkg}_end_time", "23:59") ?: "23:59"

            if (isCurrentTimeInRange(startTime, endTime)) {
                val pattern = prefs.getString("${pkg}_pattern", "200,200") ?: "200,200"
                val intensity = prefs.getInt("${pkg}_intensity", 10)
                flashManager.playPattern(pattern, intensity)
            }

            seenNotifications.add(sbn.key)
            if (seenNotifications.size > 100) {
                seenNotifications.remove(seenNotifications.first())
            }
        }
    }

    private fun isCurrentTimeInRange(start: String, end: String): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val startParts = start.split(":")
        val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()

        val endParts = end.split(":")
        val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overlap midnight (e.g., 22:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        seenNotifications.remove(sbn.key)
    }
}
