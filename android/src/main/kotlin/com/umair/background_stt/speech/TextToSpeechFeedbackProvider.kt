package com.umair.background_stt.speech

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.umair.background_stt.BackgroundSttPlugin
import com.umair.background_stt.R
import com.umair.background_stt.SpeechListenService
import com.umair.background_stt.adjustSound
import com.umair.background_stt.models.ConfirmIntent
import com.umair.background_stt.models.ConfirmationResult
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
    private var voiceConfirmationRequested = false
    private var maxTries = 20
    private var tries = 0
    private var voiceReplyCount = 0
    private var isListening = false

    private var soundPool = SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0)
    private var soundId = soundPool.load(context, R.raw.bleep, 1)

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
                SpeechListenService.isSpeaking = false
                doOnSpeechComplete()
            }

            override fun onError(utteranceId: String?) {
                SpeechListenService.isSpeaking = false
                doOnSpeechComplete()
            }

            override fun onStart(utteranceId: String?) {
                SpeechListenService.isSpeaking = true
                context.adjustSound(AudioManager.ADJUST_RAISE, forceAdjust = true)
            }

        });


    }

    fun disposeTextToSpeech() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    fun speak(text: String, forceMode: Boolean = false, queue: Boolean = true) {

        if (forceMode) {
            Speech.getInstance().stopListening()
            callTextToSpeech(text, queue)
        } else {
            if (!SpeechListenService.isSpeaking && !textToSpeech?.isSpeaking!!) {
                Speech.getInstance().stopListening()
                isListening = false
                callTextToSpeech(text, queue)
            }
        }
    }

    fun setSpeaker(pitch: Float, rate: Float) {
        textToSpeech?.setPitch(pitch)
        textToSpeech?.setSpeechRate(rate)
    }

    private fun callTextToSpeech(text: String, queue: Boolean) {
        if (queue) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, text)
            } else {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
            } else {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    private fun doOnSpeechComplete() {
        handler.postDelayed({
            if (!textToSpeech?.isSpeaking!!) {
                if (!voiceReplyProvided) {
                    waitingForVoiceInput = false
                }
                resumeSpeechService()
            }
        }, 300)
    }

    fun resumeSpeechService() {
        Log.i(TAG, "Listening to voice commands..")
        isListening = true
        context.adjustSound(AudioManager.ADJUST_MUTE, forceAdjust = true)
        SpeechListenService.startListening()
    }

    fun isConfirmationInProgress(): Boolean {
        return confirmationInProgress
    }

    fun cancelConfirmation(now: Boolean = false) {
        if (now) {
            resetConfirmation()
            Log.i(TAG, "Confirmation cancelled.")
            context.adjustSound(AudioManager.ADJUST_MUTE)
            SpeechListenService.startListening()
        } else {
            if (confirmationProvided) {
                handler.postDelayed({
                    confirmationInProgress = false
                }, 3000)
            }
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
                sendConfirmation("", false, "")
            }
        }
    }

    private fun doForConfirmation(text: String) {
        if (isListening) {
            val reply = text.substringBefore(" ")
            Log.i(TAG, "doForConfirmation: Reply: \"$reply\"")
            if (reply.isNotEmpty()) {
                confirmationIntent?.let { confirmationIntent ->
                    if (confirmationIntent.confirmationIntent.isNotEmpty()) {
                        var isSuccess = false
                        if (confirmationIntent.positiveCommand.matches(Regex(reply)) || confirmationIntent.negativeCommand.matches(Regex(reply))) {
                            Log.i(TAG, "doForConfirmation: Confirmation provided: \"$reply\"")

                            speak("You said: $reply")

                            isSuccess = true

                            if (confirmationIntent.voiceMessage.isNotEmpty()) {
                                sendConfirmation(reply, isSuccess, confirmationIntent.voiceMessage)
                            } else {
                                sendConfirmation(reply, isSuccess, "")
                            }

                        } else {
                            if (tries < maxTries) {
                                Log.e(TAG, "doForConfirmation: No appropriate reply received! Try Count: $tries")
                                tries++
                            } else {
                                Log.i(TAG, "doForConfirmation: Confirmation failed.")
                                speak("Confirmation failed, Retry again.")
                                sendConfirmation(reply, isSuccess, "")
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "doOnConfirmationProvided: No appropriate reply received!")
            }
        }
    }

    private fun doForVoiceInput(text: String) {
        if (!voiceReplyProvided) {
            if (!waitingForVoiceInput) {

                voiceReplyCount++

                Log.i(TAG, "doForVoiceInput: Voice Reply: \"$text\", Count: $voiceReplyCount")
                confirmationIntent?.voiceMessage = text

                handler.postDelayed({
                    if (!voiceReplyProvided) {
                        confirmationIntent?.voiceInputMessage?.let {
                            Log.i(TAG, "doForVoiceInput: Starting Confirmation.\n " +
                                    "Positive Reply: ${confirmationIntent?.positiveCommand}\n " +
                                    "Negative Reply: ${confirmationIntent?.negativeCommand}")

                            speak("You said: ${confirmationIntent?.voiceMessage} $it say: ${confirmationIntent?.positiveCommand} or ${confirmationIntent?.negativeCommand}")
                            voiceConfirmationRequested = true
                        }
                    }
                    voiceReplyProvided = true
                }, 4000)

            }
        }
    }

    private fun sendConfirmation(reply: String, isSuccess: Boolean, voiceInputMessage: String) {
        confirmationIntent?.let {
            BackgroundSttPlugin.eventSink?.success(ConfirmationResult(it.confirmationIntent, reply, voiceInputMessage, isSuccess).toString())
            Log.i(TAG, "sendConfirmation: Confirmation sent.")

            this.confirmationIntent = null
            tries = 0

            handler.postDelayed({
                resetConfirmation()
                Log.i(TAG, "sendConfirmation: Confirmation completed.")
            }, 3000)
        }
    }

    private fun resetConfirmation() {
        confirmationInProgress = false
        confirmationProvided = true
        waitingForVoiceInput = false
        voiceReplyProvided = false
        voiceConfirmationRequested = false
        voiceReplyCount = 0
        soundPool.release()
        SpeechListenService.isSpeaking = false
        isListening = false
    }

    private fun playSound() {
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }
}