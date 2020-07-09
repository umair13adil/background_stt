package com.umair.background_stt.speech;

public interface TextToSpeechCallback {
    void onStart();
    void onCompleted();
    void onError();
}
