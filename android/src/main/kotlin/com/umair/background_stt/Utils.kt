package com.umair.background_stt

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import io.flutter.plugin.common.MethodCall
import java.io.ByteArrayInputStream
import java.io.InputStream

private var isLoud = false
private var audioValue = -1000

fun Activity.enableAutoStart() {
    for (intent in Constants.AUTO_START_INTENTS) {
        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            MaterialDialog.Builder(this).title(R.string.enable_auto_start)
                    .content(R.string.ask_permission)
                    .theme(Theme.LIGHT)
                    .positiveText(getString(R.string.allow))
                    .onPositive { dialog: MaterialDialog?, which: DialogAction? ->
                        try {
                            for (intent1 in Constants.AUTO_START_INTENTS) if (packageManager.resolveActivity(intent1, PackageManager.MATCH_DEFAULT_ONLY)
                                    != null) {
                                startActivity(intent1)
                                break
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .show()
            break
        }
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