package com.umair.background_stt

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager.ADJUST_MUTE
import android.os.IBinder
import android.util.Log
import com.umair.background_stt.models.SpeechResult
import com.umair.background_stt.speech.*
import com.umair.background_stt.speech.Speech.stopDueToDelay
import java.util.*

class SpeechListenService : Service(), stopDueToDelay {

    companion object {
        private val TAG = "SpeechListenService"
        private var context: Context? = null
        private var lastSentResult = ""

        @JvmStatic
        private var feedBackProvider: TextToSpeechFeedbackProvider? = null

        fun doOnIntentConfirmation(text: String, positiveText: String, negativeText: String, voiceInputMessage: String, voiceInput: Boolean) {
            Speech.getInstance().stopListening()
            feedBackProvider?.setConfirmationData(text, positiveText, negativeText, voiceInputMessage, voiceInput)
            feedBackProvider?.speak(text)
        }

        fun stopSpeechListener() {
            feedBackProvider?.disposeTextToSpeech()
            Speech.getInstance().shutdown()
        }

        fun startListening() {
            if (Speech.isActive()) {
                Speech.getInstance().startListening(object : SpeechDelegate {
                    override fun onStartOfSpeech() {
                    }

                    override fun onSpeechRmsChanged(value: Float) {
                    }

                    override fun onSpeechPartialResults(results: List<String>) {
                        if (results.isNotEmpty() && results.size > 1) {
                            for (partial in results) {
                                if (partial.isNotEmpty()) {
                                    if (partial.isNotEmpty()) {
                                        sendResults(partial, true)
                                    }
                                }
                            }
                        } else {
                            if (results.first().isNotEmpty()) {
                                sendResults(results.first(), true)
                            }
                        }

                    }

                    override fun onSpeechResult(result: String) {
                        if (result.isNotEmpty()) {
                            sendResults(result, false)
                        }
                    }
                })
            }
        }

        private fun sendResults(result: String, isPartial: Boolean) {

            if (feedBackProvider?.isConfirmationInProgress()!!) {
                feedBackProvider?.doOnConfirmationProvided(result)
            } else {
                if (lastSentResult.isEmpty() || lastSentResult != result) {
                    BackgroundSttPlugin.eventSink?.success(SpeechResult(result, isPartial).toString())
                    lastSentResult = result
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        context = this

        feedBackProvider = TextToSpeechFeedbackProvider(this)

        BackgroundSttPlugin.binaryMessenger?.let {
            Log.i(TAG, "$TAG service running.")
            BackgroundSttPlugin.registerWith(it, this)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Speech.isActive()) {
            Speech.getInstance().setListener(this)
            if (Speech.getInstance().isListening) {
                Speech.getInstance().stopListening()
                muteSounds()
            } else {
                System.setProperty("rx.unsafe-disable", "True")
                try {
                    startListening()
                } catch (exc: SpeechRecognitionNotAvailable) {
                    Log.e(TAG, "${exc.message}")
                } catch (exc: GoogleVoiceTypingDisabledException) {
                    Log.e(TAG, "${exc.message}")
                }
                muteSounds()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onSpecifiedCommandPronounced(event: String) {

        if (Speech.isActive()) {
            if (Speech.getInstance().isListening) {
                Log.i(TAG, "$TAG onSpecifiedCommandPronounced: Still Listening..")
                //muteSounds()
                //Speech.getInstance().stopListening()
            } else {
                try {
                    //Log.i(TAG, "$TAG onSpecifiedCommandPronounced: Restart Listening..")
                    //Speech.getInstance().stopTextToSpeech()
                    startListening()
                } catch (exc: SpeechRecognitionNotAvailable) {
                    Log.e(TAG, "${exc.message}")
                } catch (exc: GoogleVoiceTypingDisabledException) {
                    Log.e(TAG, "${exc.message}")
                }
                muteSounds()
            }
        }
    }

    /**
     * Function to remove the beep sound of voice recognizer.
     */
    private fun muteSounds() {
        adjustSound(ADJUST_MUTE)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.i(TAG, "$TAG onTaskRemoved")
        val service = PendingIntent.getService(applicationContext, Random().nextInt(),
                Intent(applicationContext, SpeechListenService::class.java), PendingIntent.FLAG_ONE_SHOT)
        val alarmManager = (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        alarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000] = service
        super.onTaskRemoved(rootIntent)
    }
}