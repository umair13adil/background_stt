package com.umair.background_stt

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import io.flutter.plugin.common.MethodCall
import java.io.ByteArrayInputStream
import java.io.InputStream


private var isLoud = false
private var audioValue = -1000

fun Activity.enableAutoStart() {
    try {
        val intent = Intent()
        val manufacturer = Build.MANUFACTURER
        when {
            "xiaomi".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            }
            "oppo".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            }
            "vivo".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            }
            "Letv".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")
            }
            "Honor".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            }
        }
        val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (list.size > 0) {
            this.startActivity(intent)
        }
    } catch (e: java.lang.Exception) {
        Log.e("exc", e.toString())
    }
}

fun getListOfStringById(key: String, call: MethodCall): ArrayList<String> {
    val logTypesList = arrayListOf<String>()
    call.argument<String>(key)?.let {
        it.split(",").forEach {
            logTypesList.add(it)
        }
        return logTypesList
    }
    return arrayListOf()
}

fun getStringValueById(key: String, call: MethodCall): String {
    call.argument<String>(key)?.let {
        return it
    }
    return ""
}

fun getIntValueById(key: String, call: MethodCall): Int? {
    call.argument<Int>(key)?.let {
        return it
    }
    return null
}

fun getBoolValueById(key: String, call: MethodCall): Boolean {
    call.argument<Boolean>(key)?.let {
        return it
    }
    return false
}

fun getInputStreamValueById(key: String, call: MethodCall): InputStream? {
    call.argument<ByteArray>(key)?.let {
        return ByteArrayInputStream(it)
    }
    return null
}

fun Context.adjustSound(adjust: Int, forceAdjust: Boolean = false) {
    if (SpeechListenService.isListening) {
        if (adjust == AudioManager.ADJUST_MUTE && SpeechListenService.isSpeaking()) {
            return
        } else {
            if (audioValue != adjust) {
                adjustSoundValues(adjust)
                audioValue = adjust
            } else {
                if (forceAdjust) {
                    adjustSoundValues(adjust)
                }
            }
        }
    } else {
        if (adjust == AudioManager.ADJUST_MUTE) {
            if (!isLoud) {
                adjustSoundValues(AudioManager.ADJUST_RAISE)
                isLoud = true
            }
        } else {
            isLoud = false
        }
    }
}

private fun Context.adjustSoundValues(adjust: Int) {
    (getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager).let { audioManager ->
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, adjust, 0)
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_ALARM, adjust, 0)
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, adjust, 0)
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_RING, adjust, 0)
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_SYSTEM, adjust, 0)
    }
}