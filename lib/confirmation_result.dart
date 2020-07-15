class ConfirmationResult {
  String confirmationIntent;
  String confirmedResult;
  String voiceInput;
  bool isSuccess;

  ConfirmationResult(
      {this.confirmationIntent,
      this.confirmedResult,
      this.voiceInput,
      this.isSuccess});

  ConfirmationResult.fromJson(Map<String, dynamic> json) {
    confirmationIntent = json['confirmationIntent'];
    confirmedResult = json['confirmedResult'];
    voiceInput = json['voiceInput'];
    isSuccess = json['isSuccess'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['confirmationIntent'] = this.confirmationIntent;
    data['confirmedResult'] = this.confirmedResult;
    data['voiceInput'] = this.voiceInput;
    data['isSuccess'] = this.isSuccess;
    return data;
  }

  @override
  String toString() {
    return 'ConfirmationResult{confirmationIntent: $confirmationIntent, confirmedResult: $confirmedResult, voiceInput: $voiceInput, isSuccess: $isSuccess}';
  }
}
