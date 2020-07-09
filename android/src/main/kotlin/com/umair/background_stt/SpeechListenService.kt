package com.umair.background_stt

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.ADJUST_MUTE
import android.media.AudioManager.ADJUST_RAISE
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import com.example.background_tts_stt.SpeechResult
import com.umair.background_stt.speech.GoogleVoiceTypingDisabledException
import com.umair.background_stt.speech.Speech
import com.umair.background_stt.speech.Speech.stopDueToDelay
import com.umair.background_stt.speech.SpeechDelegate
import com.umair.background_stt.speech.SpeechRecognitionNotAvailable
import java.util.*

class SpeechListenService : Service(), SpeechDelegate, stopDueToDelay {

    private val TAG = "SpeechListenService"
    private val adjustMute = ADJUST_MUTE
    private val adjustFull = ADJUST_RAISE


    override fun onCreate() {
        super.onCreate()

        BackgroundSttPlugin.binaryMessenger?.let {
            Log.i(TAG, "$TAG service running.")
            BackgroundSttPlugin.registerWith(it, this)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        delegate = this
        Speech.getInstance().setListener(this)
        if (Speech.getInstance().isListening) {
            Speech.getInstance().stopListening()
            muteSounds()
        } else {
            System.setProperty("rx.unsafe-disable", "True")
            try {
                Speech.getInstance().stopTextToSpeech()
                startListening()
            } catch (exc: SpeechRecognitionNotAvailable) {
                Log.e(TAG, "${exc.message}")
            } catch (exc: GoogleVoiceTypingDisabledException) {
                Log.e(TAG, "${exc.message}")
            }
            muteSounds()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartOfSpeech() {}
    override fun onSpeechRmsChanged(value: Float) {}
    override fun onSpeechPartialResults(results: List<String>) {
        for (partial in results) {
            if (partial.isNotEmpty())
                Log.i(TAG, "$TAG $partial")
            if (partial.isNotEmpty()) {
                //resetSounds()
                BackgroundSttPlugin.eventSink?.success(SpeechResult(partial, true).toString())
            }
        }
    }

    override fun onSpeechResult(result: String) {
        if (result.isNotEmpty()) {
            resetSounds()
            BackgroundSttPlugin.eventSink?.success(SpeechResult(result, false).toString())
        }
    }

    override fun onSpecifiedCommandPronounced(event: String) {
        if (Speech.getInstance().isListening) {
            muteSounds()
            Speech.getInstance().stopListening()
        } else {
            try {
                Speech.getInstance().stopTextToSpeech()
                startListening()
            } catch (exc: SpeechRecognitionNotAvailable) {
                Log.e(TAG, "${exc.message}")
            } catch (exc: GoogleVoiceTypingDisabledException) {
                Log.e(TAG, "${exc.message}")
            }
            muteSounds()
        }
    }

    /**
     * Function to remove the beep sound of voice recognizer.
     */
    private fun muteSounds() {
        //Log.i(TAG, "$TAG Muting all sounds..")
        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).let { audioManager ->
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, adjustMute, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, adjustMute, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, adjustMute, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, adjustMute, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, adjustMute, 0)
        }
    }

    private fun resetSounds() {
        object : CountDownTimer(500, 500) {
            override fun onFinish() {
                //Log.i(TAG, "$TAG Un-mute all sounds..")
                (getSystemService(Context.AUDIO_SERVICE) as AudioManager).let { audioManager ->
                    audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, adjustFull, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, adjustFull, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, adjustFull, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_RING, adjustFull, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, adjustFull, 0)
                }
            }

            override fun onTick(millisUntilFinished: Long) {
            }

        }.start()
    }

    override fun onTaskRemoved(rootIntent: Intent) { //Restarting the service if it is removed.
        val service = PendingIntent.getService(applicationContext, Random().nextInt(),
                Intent(applicationContext, SpeechListenService::class.java), PendingIntent.FLAG_ONE_SHOT)
        val alarmManager = (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        alarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000] = service
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        var delegate: SpeechDelegate? = null
    }

    private fun startListening() {
//        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).let { audioManager ->
//            if (!audioManager.isMusicActive && !audioManager.isSpeakerphoneOn && audioManager.isMicrophoneMute)
        Speech.getInstance().startListening(null, this)
//        }
    }
}