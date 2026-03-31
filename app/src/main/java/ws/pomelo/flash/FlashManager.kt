package ws.pomelo.flash

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.PowerManager
import kotlinx.coroutines.*

class FlashManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var cameraId: String? = cameraManager.cameraIdList.getOrNull(0)

    fun playPattern(pattern: String) {
        val id = cameraId ?: return
        val delays = pattern.split(",").mapNotNull { it.trim().toLongOrNull() }
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FlashNotify::Lock")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                //if (!wakeLock.isHeld) wakeLock.acquire(10000)
                
                var isOn = true
                for (ms in delays) {
                    cameraManager.setTorchMode(id, isOn)
                    delay(ms)
                    isOn = !isOn
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    cameraManager.setTorchMode(id, false)
                } catch (e: Exception) {
                    // Ignore
                }
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }
}
