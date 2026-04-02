package ws.pomelo.flash

import android.graphics.drawable.Drawable
import java.util.UUID

data class FlashConfig(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var pattern: String,
    var filterText: String = "",
    var startTime: String, // e.g., "22:00"
    var endTime: String,   // e.g., "07:00"
    var isEnabled: Boolean
)
