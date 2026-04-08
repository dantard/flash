package ws.pomelo.flash

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.*

class FlashManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    @Suppress("DEPRECATION")
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    } catch (e: Exception) {
        null
    }
    
    private var cameraId: String? = try { cameraManager.cameraIdList.getOrNull(0) } catch (e: Exception) { null }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private var activeJob: Job? = null

    fun playPattern(pattern: String, useFlash: Boolean, useVibration: Boolean, useSound: Boolean) {
        Log.d("FlashManager", "playPattern: $pattern, flash=$useFlash, vib=$useVibration, sound=$useSound")
        val id = cameraId
        val delays = pattern.split(",").mapNotNull { it.trim().toLongOrNull() }
        if (delays.isEmpty()) return

        synchronized(this) {
            activeJob?.cancel()
            activeJob = scope.launch {
                val wakeLock = try {
                    powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FlashNotify::Lock")
                } catch (e: Exception) {
                    null
                }
                
                try {
                    wakeLock?.acquire(30000)
                    
                    var isOn = true
                    for (ms in delays) {
                        if (!isActive) break
                        
                        if (isOn) {
                            if (useFlash && id != null) {
                                try { cameraManager.setTorchMode(id, true) } catch (e: Exception) {
                                    Log.e("FlashManager", "Torch error", e)
                                }
                            }
                            if (useVibration) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(ms)
                                }
                            }
                            if (useSound) {
                                try {
                                    // Use TONE_PROP_BEEP or TONE_SUP_NOTIFICATION
                                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, ms.toInt())
                                } catch (e: Exception) {
                                    Log.e("FlashManager", "Sound error", e)
                                }
                            }
                        } else {
                            if (useFlash && id != null) {
                                try { cameraManager.setTorchMode(id, false) } catch (e: Exception) {}
                            }
                        }
                        delay(ms)
                        isOn = !isOn
                    }
                } catch (e: Exception) {
                    Log.e("FlashManager", "Error playing pattern", e)
                } finally {
                    try {
                        if (id != null) cameraManager.setTorchMode(id, false)
                    } catch (e: Exception) {}
                    if (wakeLock?.isHeld == true) {
                        try { wakeLock.release() } catch (e: Exception) {}
                    }
                }
            }
        }
    }
}
