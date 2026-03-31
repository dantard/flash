package ws.pomelo.flash

import android.graphics.drawable.Drawable

data class FlashConfig(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var pattern: String,
    var startTime: String, // e.g., "22:00"
    var endTime: String,   // e.g., "07:00"
    var isEnabled: Boolean
)
