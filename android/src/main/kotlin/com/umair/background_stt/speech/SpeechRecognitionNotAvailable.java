package com.umair.background_stt.speech;


public class SpeechRecognitionNotAvailable extends Exception {
    public SpeechRecognitionNotAvailable() {
        super("Speech recognition not available");
    }
}
