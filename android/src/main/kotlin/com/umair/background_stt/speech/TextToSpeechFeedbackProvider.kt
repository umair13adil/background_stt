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
    private var confirmationProvided = false
    private var waitingForVoiceInput = false
    private var voiceReplyProvided = false
    private var maxTries = 10
    private var tries = 0

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
        handler.postDelayed({
            if (!voiceReplyProvided) {
                waitingForVoiceInput = false
            }
            Log.i(TAG, "Listening to voice commands..")
            context.adjustSound(AudioManager.ADJUST_MUTE)
            SpeechListenService.startListening()
        }, 2500)
    }

    fun isConfirmationInProgress(): Boolean {
        return confirmationInProgress
    }

    fun cancelConfirmation() {
        if (confirmationProvided) {
            handler.postDelayed({
                confirmationInProgress = false
            }, 3000)
        }
    }

    fun setConfirmationData(confirmationText: String, positiveCommand: String, negativeCommand: String, voiceInputMessage: String, voiceInput: Boolean) {
        tries = 0
        confirmationInProgress = true
        confirmationIntent = ConfirmIntent(confirmationText, positiveCommand, negativeCommand, voiceInputMessage, voiceInput)
        waitingForVoiceInput = voiceInput
    }

    fun doOnConfirmationProvided(text: String) {
        if (text.isNotEmpty()) {
            if (confirmationInProgress) {
                confirmationIntent?.let { confirmationIntent ->
                    if (confirmationIntent.voiceInput && !voiceReplyProvided) {
                        doForVoiceInput(text)
                    } else {
                        doForConfirmation(text)
                    }
                }
            } else {
                Log.e(TAG, "doOnConfirmationProvided: No confirmation in progress!")
                sendConfirmation("", false)
            }
        }
    }

    private fun doForConfirmation(text: String) {
        val reply = text.substringBefore(" ")
        Log.i(TAG, "doForConfirmation: Reply: \"$reply\"")
        if (reply.isNotEmpty()) {
            confirmationIntent?.let { confirmationIntent ->
                if (confirmationIntent.confirmationIntent.isNotEmpty()) {
                    var isSuccess = false
                    if (confirmationIntent.positiveCommand.matches(Regex(reply)) || confirmationIntent.negativeCommand.matches(Regex(reply))) {
                        Log.i(TAG, "doForConfirmation: Confirmation provided: \"$reply\"")
                        isSuccess = true
                        sendConfirmation(reply, isSuccess)
                    } else {
                        if (tries < maxTries) {
                            Log.e(TAG, "doForConfirmation: No appropriate reply received! Try Count: $tries")
                            tries++
                        } else {
                            Log.i(TAG, "doForConfirmation: Confirmation failed.")
                            sendConfirmation(reply, isSuccess)
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "doOnConfirmationProvided: No appropriate reply received!")
        }
    }

    private fun doForVoiceInput(text: String) {
        if (!voiceReplyProvided) {
            if (!waitingForVoiceInput) {

                Log.i(TAG, "doForVoiceInput: Voice Reply: \"$text\"")
                confirmationIntent?.voiceMessage = text

                handler.postDelayed({
                    voiceReplyProvided = true
                    Log.i(TAG, "doForVoiceInput: Starting Confirmation.\n " +
                            "Positive Reply: ${confirmationIntent?.positiveCommand}\n " +
                            "Negative Reply: ${confirmationIntent?.negativeCommand}")
                    confirmationIntent?.voiceInputMessage?.let {
                        Speech.getInstance().stopListening()
                        speak(it)
                    }
                }, 4000)
            }
        }
    }

    private fun sendConfirmation(reply: String, isSuccess: Boolean) {
        confirmationIntent?.let {
            BackgroundSttPlugin.eventSink?.success(ConfirmationResult(it.confirmationIntent, reply, isSuccess).toString())
            Log.i(TAG, "sendConfirmation: Confirmation sent.")

            this.confirmationIntent = null
            tries = 0

            handler.postDelayed({
                confirmationInProgress = false
                confirmationProvided = true
                waitingForVoiceInput = false
                voiceReplyProvided = false
                Log.i(TAG, "sendConfirmation: Confirmation completed.")
            }, 2000)
        }
    }
}