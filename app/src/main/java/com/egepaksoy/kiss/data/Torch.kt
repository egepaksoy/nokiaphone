package com.egepaksoy.kiss.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Torch(context: Context) {
    private val cm = context.getSystemService(CameraManager::class.java)
    private val cameraId: String? = runCatching {
        cm.cameraIdList.firstOrNull { id ->
            val c = cm.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on
    val available get() = cameraId != null

    init {
        if (cameraId != null) {
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(id: String, enabled: Boolean) {
                    if (id == cameraId) _on.value = enabled
                }
                override fun onTorchModeUnavailable(id: String) {
                    if (id == cameraId) _on.value = false
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    fun toggle() {
        val id = cameraId ?: return
        runCatching { cm.setTorchMode(id, !_on.value) }
    }

    fun off() {
        val id = cameraId ?: return
        if (_on.value) runCatching { cm.setTorchMode(id, false) }
    }
}
