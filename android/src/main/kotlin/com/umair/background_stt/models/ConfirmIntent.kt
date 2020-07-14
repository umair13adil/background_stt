package com.umair.background_stt.models

data class ConfirmIntent(var confirmationIntent: String = "",
                         var positiveCommand: String = "",
                         var negativeCommand: String,
                         var voiceInputMessage: String,
                         var voiceInput: Boolean,
                         var voiceMessage: String = "") {

}