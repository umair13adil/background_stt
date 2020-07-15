package com.umair.background_stt.models

data class ConfirmationResult(var confirmationIntent: String = "", var confirmedResult: String = "", var voiceInput: String, var isSuccess: Boolean) {

    override fun toString(): String {
        return "{" +
                "  \"confirmationIntent\": \"$confirmationIntent\"," +
                "  \"confirmedResult\": \"$confirmedResult\"," +
                "  \"voiceInput\": \"$voiceInput\"," +
                "  \"isSuccess\": $isSuccess" +
                "}"
    }
}