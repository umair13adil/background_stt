package com.umair.background_stt.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class Speech {

    private static final String LOG_TAG = "Speech";

    private static Speech instance = null;

    private SpeechRecognizer mSpeechRecognizer;
    private String mCallingPackage;
    private boolean mPreferOffline = false;
    private boolean mGetPartialResults = true;
    private SpeechDelegate mDelegate;
    private boolean mIsListening = false;

    private final List<String> mPartialData = new ArrayList<>();
    private String mUnstableData;

    private DelayedOperation mDelayedStopListening;
    private Context mContext;

    private final Map<String, TextToSpeechCallback> mTtsCallbacks = new HashMap<>();
    private Locale mLocale = Locale.getDefault();
    private long mStopListeningDelayInMs = 10000;
    private long mTransitionMinimumDelay = 1200;
    private long mLastActionTimestamp;
    private List<String> mLastPartialResults = null;

    private final TextToSpeech.OnInitListener mTttsInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(final int status) {
            switch (status) {
                case TextToSpeech.SUCCESS:
                    Logger.info(LOG_TAG, "TextToSpeech engine successfully started");
                    break;

                case TextToSpeech.ERROR:
                    Logger.error(LOG_TAG, "Error while initializing TextToSpeech engine!");
                    break;

                default:
                    Logger.error(LOG_TAG, "Unknown TextToSpeech status: " + status);
                    break;
            }
        }
    };

    private final RecognitionListener mListener = new RecognitionListener() {

        @Override
        public void onReadyForSpeech(final Bundle bundle) {
            mPartialData.clear();
            mUnstableData = null;
        }

        @Override
        public void onBeginningOfSpeech() {
            mDelayedStopListening.start(new DelayedOperation.Operation() {
                @Override
                public void onDelayedOperation() {
                    //returnPartialResultsAndRecreateSpeechRecognizer();
                }

                @Override
                public boolean shouldExecuteDelayedOperation() {
                    return true;
                }
            });
        }

        @Override
        public void onRmsChanged(final float v) {
            try {
                if (mDelegate != null)
                    mDelegate.onSpeechRmsChanged(v);
            } catch (final Throwable exc) {
                Logger.error(Speech.class.getSimpleName(),
                        "Unhandled exception in delegate onSpeechRmsChanged", exc);
            }
        }

        @Override
        public void onPartialResults(final Bundle bundle) {
            mDelayedStopListening.resetTimer();

            final List<String> partialResults = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            final List<String> unstableData = bundle.getStringArrayList("android.speech.extra.UNSTABLE_TEXT");

            if (partialResults != null && !partialResults.isEmpty()) {
                mPartialData.clear();
                mPartialData.addAll(partialResults);
                mUnstableData = unstableData != null && !unstableData.isEmpty()
                        ? unstableData.get(0) : null;
                try {
                    if (mLastPartialResults == null || !mLastPartialResults.equals(partialResults)) {
                        if (mDelegate != null)
                            mDelegate.onSpeechPartialResults(partialResults);
                        mLastPartialResults = partialResults;
                    }
                } catch (final Throwable exc) {
                    Logger.error(Speech.class.getSimpleName(),
                            "Unhandled exception in delegate onSpeechPartialResults", exc);
                }
            }
        }

        @Override
        public void onResults(final Bundle bundle) {
            mDelayedStopListening.cancel();

            final List<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            final String result;

            if (results != null && !results.isEmpty()
                    && results.get(0) != null && !results.get(0).isEmpty()) {
                result = results.get(0);
            } else {
                Logger.info(Speech.class.getSimpleName(), "No speech results, getting partial");
                result = getPartialResultsAsString();
            }

            mIsListening = false;

            try {
                if (mDelegate != null)
                    mDelegate.onSpeechResult(result.trim());
            } catch (final Throwable exc) {
                Logger.error(Speech.class.getSimpleName(),
                        "Unhandled exception in delegate onSpeechResult", exc);
            }

            initSpeechRecognizer(mContext);
        }

        @Override
        public void onError(final int code) {
            //Logger.error(LOG_TAG, "Speech recognition error", new SpeechRecognitionException(code));
            returnPartialResultsAndRecreateSpeechRecognizer();
        }

        @Override
        public void onBufferReceived(final byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onEvent(final int i, final Bundle bundle) {

        }
    };

    private Speech(final Context context, final String callingPackage) {
        initSpeechRecognizer(context);
        mCallingPackage = callingPackage;
    }

    private void initSpeechRecognizer(final Context context) {
        if (context == null)
            throw new IllegalArgumentException("context must be defined!");

        mContext = context;

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            if (mSpeechRecognizer != null) {
                try {
                    mSpeechRecognizer.destroy();
                } catch (final Throwable exc) {
                    Logger.debug(Speech.class.getSimpleName(),
                            "Non-Fatal error while destroying speech. " + exc.getMessage());
                } finally {
                    mSpeechRecognizer = null;
                }
            }

            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            mSpeechRecognizer.setRecognitionListener(mListener);
            initDelayedStopListening(context);

        } else {
            mSpeechRecognizer = null;
        }

        mPartialData.clear();
        mUnstableData = null;
    }

    private void initDelayedStopListening(final Context context) {
        if (mDelayedStopListening != null) {
            mDelayedStopListening.cancel();
            mDelayedStopListening = null;
        }

        if (mListenerDelay != null) {
            mListenerDelay.onSpecifiedCommandPronounced("1");
        }
        mDelayedStopListening = new DelayedOperation(context, "delayStopListening", mStopListeningDelayInMs);
    }

    /**
     * Initializes speech recognition.
     *
     * @param context        application context
     * @param callingPackage The extra key used in an intent to the speech recognizer for
     *                       voice search. Not generally to be used by developers.
     *                       The system search dialog uses this, for example, to set a calling
     *                       package for identification by a voice search API.
     *                       If this extra is set by anyone but the system process,
     *                       it should be overridden by the voice search implementation.
     *                       By passing null or empty string (which is the default) you are
     *                       not overriding the calling package
     * @return speech instance
     */
    public static Speech init(final Context context, final String callingPackage, final Long stopListeningDelay, final Long transitionDelay) {
        Log.d(LOG_TAG, "init");
        if (instance == null) {
            instance = new Speech(context, callingPackage);
        }
        instance.mStopListeningDelayInMs = stopListeningDelay;
        instance.mTransitionMinimumDelay = transitionDelay;
        return instance;
    }

    /**
     * Must be called inside Activity's onDestroy.
     */
    public synchronized void shutdown() {
        if (instance != null) {
            Log.i(LOG_TAG, "Shutting down speech-to-text.");
            if (mSpeechRecognizer != null) {
                try {
                    mSpeechRecognizer.stopListening();
                } catch (final Exception exc) {
                    Logger.error(getClass().getSimpleName(), "Warning while de-initing speech recognizer", exc);
                }
            }

            unregisterDelegate();
            instance = null;
        }
    }

    /**
     * Gets speech recognition instance.
     *
     * @return SpeechRecognition instance
     */
    public static Speech getInstance() {

        if (instance == null) {
            throw new IllegalStateException("Speech recognition has not been initialized! call init method first!");
        }

        return instance;
    }

    public static boolean isActive() {
        return instance != null;
    }

    /**
     * Starts voice recognition.
     *
     * @param delegate delegate which will receive speech recognition events and status
     * @throws SpeechRecognitionNotAvailable      when speech recognition is not available on the device
     * @throws GoogleVoiceTypingDisabledException when google voice typing is disabled on the device
     */
    public void startListening(SpeechDelegate delegate) throws SpeechRecognitionNotAvailable, GoogleVoiceTypingDisabledException {
        if (mIsListening) return;

        if (mSpeechRecognizer == null)
            throw new SpeechRecognitionNotAvailable();

        if (delegate == null)
            throw new IllegalArgumentException("delegate must be defined!");

        /*if (throttleAction()) {
            Logger.debug(getClass().getSimpleName(), "Hey man calm down! Throttling start to prevent disaster!");
            return;
        }*/

        mDelegate = delegate;

        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, mGetPartialResults)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, mLocale.getLanguage())
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        if (mCallingPackage != null && !mCallingPackage.isEmpty()) {
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, mCallingPackage);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, mPreferOffline);
        }

        try {
            mSpeechRecognizer.startListening(intent);
        } catch (final SecurityException exc) {
            throw new GoogleVoiceTypingDisabledException();
        }

        mIsListening = true;
        updateLastActionTimestamp();

        try {
            if (mDelegate != null)
                mDelegate.onStartOfSpeech();
        } catch (final Throwable exc) {
            Logger.error(Speech.class.getSimpleName(),
                    "Unhandled exception in delegate onStartOfSpeech", exc);
        }

    }

    private void unregisterDelegate() {
        mDelegate = null;
    }

    private void updateLastActionTimestamp() {
        mLastActionTimestamp = new Date().getTime();
    }

    private boolean throttleAction() {
        return (new Date().getTime() <= (mLastActionTimestamp + mTransitionMinimumDelay));
    }

    /**
     * Stops voice recognition listening.
     * This method does nothing if voice listening is not active
     */
    public void stopListening() {
        if (!mIsListening) {
            Log.i(LOG_TAG,"stopListening: Already listening");
            return;
        }

       /* if (throttleAction()) {
            //Logger.debug(getClass().getSimpleName(), "Hey man calm down! Throttling stop to prevent disaster!");
            return;
        }*/

        mIsListening = false;
        updateLastActionTimestamp();
        returnPartialResultsAndRecreateSpeechRecognizer();
    }

    private String getPartialResultsAsString() {
        final StringBuilder out = new StringBuilder("");

        for (final String partial : mPartialData) {
            out.append(partial).append(" ");
        }

        if (mUnstableData != null && !mUnstableData.isEmpty())
            out.append(mUnstableData);

        return out.toString().trim();
    }

    private void returnPartialResultsAndRecreateSpeechRecognizer() {
        mIsListening = false;
        try {
            if (mDelegate != null)
                mDelegate.onSpeechResult(getPartialResultsAsString());
        } catch (final Throwable exc) {
            Logger.error(Speech.class.getSimpleName(),
                    "Unhandled exception in delegate onSpeechResult", exc);
        }

        // recreate the speech recognizer
        initSpeechRecognizer(mContext);
    }

    /**
     * Check if voice recognition is currently active.
     *
     * @return true if the voice recognition is on, false otherwise
     */
    public boolean isListening() {
        return mIsListening;
    }

    private Speech.stopDueToDelay mListenerDelay;

    // define listener
    public interface stopDueToDelay {
        void onSpecifiedCommandPronounced(final String event);
    }

    // set the listener. Must be called from the fragment
    public void setListener(Speech.stopDueToDelay listener) {
        this.mListenerDelay = listener;
    }

}
