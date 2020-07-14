package com.umair.background_stt.models

data class ConfirmationResult(var confirmationIntent: String = "", var confirmedResult: String = "", var isSuccess: Boolean) {

    override fun toString(): String {
        return "{" +
                "  \"confirmationIntent\": \"$confirmationIntent\"," +
                "  \"confirmedResult\": \"$confirmedResult\"," +
                "  \"isSuccess\": $isSuccess" +
                "}"
    }
}