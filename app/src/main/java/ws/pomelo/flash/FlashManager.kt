package ws.pomelo.flash

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.*

class FlashManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var cameraId: String? = cameraManager.cameraIdList.getOrNull(0)

    fun playPattern(pattern: String, intensity: Int) {
        val id = cameraId ?: return
        val delays = pattern.split(",").mapNotNull { it.trim().toLongOrNull() }
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FlashNotify::Lock")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                //kjhgkjhgwakeLock.acquire(5000)
                var isOn = true
                for (ms in delays) {
                    if (isOn) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            cameraManager.turnOnTorchWithStrengthLevel(id, intensity)
                        } else {
                            cameraManager.setTorchMode(id, true)
                        }
                    } else {
                        cameraManager.setTorchMode(id, false)
                    }
                    delay(ms)
                    isOn = !isOn
                }
            } finally {
                cameraManager.setTorchMode(id, false)
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }
}