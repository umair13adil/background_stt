package com.umair.background_stt.speech

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.umair.background_stt.SpeechListenService
import com.umair.background_stt.adjustSound
import java.util.*

class TextToSpeechFeedbackProvider constructor(val context: Context) {

    private val TAG = "TextToSpeechFeedback"
    private var textToSpeech: TextToSpeech? = null

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
        context.adjustSound(AudioManager.ADJUST_MUTE)
        SpeechListenService.startListening()
        SpeechListenService.confirmationInProgress = false
    }
}