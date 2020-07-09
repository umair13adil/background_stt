class SpeechResult {
  String result;
  bool isPartial;

  SpeechResult({this.result, this.isPartial});

  SpeechResult.fromJson(Map<String, dynamic> json) {
    result = json['result'];
    isPartial = json['isPartial'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['result'] = this.result;
    data['isPartial'] = this.isPartial;
    return data;
  }

  @override
  String toString() {
    return 'SpeechResult{result: $result, isPartial: $isPartial}';
  }
}
