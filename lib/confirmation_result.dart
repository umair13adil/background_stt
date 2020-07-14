class ConfirmationResult {
  String confirmationIntent;
  String confirmedResult;
  bool isSuccess;

  ConfirmationResult(
      {this.confirmationIntent, this.confirmedResult, this.isSuccess});

  ConfirmationResult.fromJson(Map<String, dynamic> json) {
    confirmationIntent = json['confirmationIntent'];
    confirmedResult = json['confirmedResult'];
    isSuccess = json['isSuccess'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['confirmationIntent'] = this.confirmationIntent;
    data['confirmedResult'] = this.confirmedResult;
    data['isSuccess'] = this.isSuccess;
    return data;
  }

  @override
  String toString() {
    return 'ConfirmationResult{confirmationIntent: $confirmationIntent, confirmedResult: $confirmedResult, isSuccess: $isSuccess}';
  }
}
