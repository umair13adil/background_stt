package com.umair.background_stt.speech

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.umair.background_stt.BackgroundSttPlugin
import com.umair.background_stt.SpeechListenService
import com.umair.background_stt.adjustSound
import com.umair.background_stt.models.ConfirmIntent
import com.umair.background_stt.models.ConfirmationResult
import com.umair.background_stt.models.SpeechResult
import java.util.*

class TextToSpeechFeedbackProvider constructor(val context: Context) {

    private val TAG = "TextToSpeechFeedback"
    private var textToSpeech: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private var confirmationIntent: ConfirmIntent? = null
    private var confirmationInProgress = false
    private var waitingForConfirmation = false
    private var confirmationProvided = false

    init {
        setUpTextToSpeech()
    }

    private fun setUpTextToSpeech() {
        textToSpeech = TextToSpeech(context,
                TextToSpeech.OnInitListener { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val ttsLang = textToSpeech?.setLanguage(Locale.US)

                        if (ttsLang == TextToSpeech.LANG_MISSING_DATA || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.i(TAG, "The Language is not supported!")
                        }
                        Log.i(TAG, "Text-to-Speech Initialized.")
                    } else {
                        Log.e(TAG, "Text-to-Speech Initialization failed.")
                    }
                })

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                doOnSpeechComplete()
            }

            override fun onError(utteranceId: String?) {
                doOnSpeechComplete()
            }

            override fun onStart(utteranceId: String?) {
                context.adjustSound(AudioManager.ADJUST_RAISE)
            }

        });


    }

    fun disposeTextToSpeech() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    fun speak(text: String) {

        val speechStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
        } else {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }

        if (speechStatus == TextToSpeech.ERROR) {
            doOnSpeechComplete()
        }
    }

    private fun doOnSpeechComplete() {
        waitingForConfirmation = true
        handler.postDelayed({
            context.adjustSound(AudioManager.ADJUST_MUTE)
            SpeechListenService.startListening()
            waitingForConfirmation = false
        }, 2500)
    }

    fun isConfirmationInProgress(): Boolean {
        return confirmationInProgress
    }

    fun isWaitingForConfirmation(): Boolean {
        return waitingForConfirmation
    }

    fun cancelConfirmation() {
        if (confirmationProvided) {
            handler.postDelayed({
                confirmationInProgress = false
            }, 3000)
        }
    }

    fun setConfirmationData(confirmationText: String, positiveCommand: String, negativeCommand: String) {
        confirmationInProgress = true
        confirmationIntent = ConfirmIntent(confirmationText, positiveCommand, negativeCommand)
        Log.i(TAG, "setConfirmationData: Confirmation data set.")
    }

    fun doOnConfirmationProvided(text: String) {
        if (text.isNotEmpty()) {
            val reply = text.substringBefore("")
            if (reply.isNotEmpty()) {
                if (confirmationInProgress && !waitingForConfirmation) {
                    confirmationIntent?.let { confirmationIntent ->
                        if (confirmationIntent.confirmationIntent.isNotEmpty()) {
                            Log.i(TAG, "doOnConfirmationProvided: Confirmation provided: \"$reply\"")
                            var isSuccess = false
                            if (confirmationIntent.positiveCommand == reply || confirmationIntent.negativeCommand == reply) {
                                isSuccess = true
                            }
                            if (isSuccess) {
                                BackgroundSttPlugin.eventSink?.success(ConfirmationResult(confirmationIntent.confirmationIntent, reply, isSuccess).toString())
                            } else {
                                BackgroundSttPlugin.eventSink?.success(ConfirmationResult(confirmationIntent.confirmationIntent, "", isSuccess).toString())
                            }
                            this.confirmationIntent = null
                            confirmationInProgress = false
                            confirmationProvided = true
                        }
                    }
                    Log.i(TAG, "doOnConfirmationProvided: Confirmation provided: $text")
                }
            }
        }
    }
}