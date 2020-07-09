package com.example.background_tts_stt

data class SpeechResult(var result: String = "", var isPartial: Boolean) {

    override fun toString(): String {
        return "{" +
                "  \"result\": \"$result\"," +
                "  \"isPartial\": $isPartial" +
                "}"
    }
}