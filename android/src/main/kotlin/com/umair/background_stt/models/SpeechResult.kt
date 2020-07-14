package com.umair.background_stt.models

data class SpeechResult(var result: String = "", var isPartial: Boolean) {

    override fun toString(): String {
        return "{" +
                "  \"result\": \"$result\"," +
                "  \"isPartial\": $isPartial" +
                "}"
    }
}