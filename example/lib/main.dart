import 'package:flutter/material.dart';
import 'package:background_stt/background_stt.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  var _service = BackgroundStt();
  var result = "Say something!";
  var confirmation = "";
  var confirmationReply = "";
  var voiceReply = "";
  var isListening = false;

  double _currentPitchValue = 100;
  double _currentRateValue = 100;

  @override
  void initState() {
    _service.startSpeechListenService;

    setState(() {
      if (mounted) isListening = true;
    });
    _service.getSpeechResults().onData((data) {
      print("getSpeechResults: ${data.result} , ${data.isPartial} [STT Mode]");

      _doOnSpeechCommandMatch(data.result);

      setState(() {
        confirmation = "";
        confirmationReply = "";
        voiceReply = "";
        result = data.result;
      });
    });

    _service.getConfirmationResults().onData((data) {
      print(
          "getConfirmationResults: Confirmation Text: ${data.confirmationIntent} , "
          "User Replied: ${data.confirmedResult} , "
          "Voice Input Message: ${data.voiceInput} , "
          "Is Confirmation Success?: ${data.isSuccess}");

      setState(() {
        confirmation = data.confirmationIntent;
        confirmationReply = data.confirmedResult;
      });
    });
    super.initState();
  }

  void _doOnSpeechCommandMatch(String command) {
    if (command == "start") {
      _service.confirmIntent(
          confirmationText: "Do you want to start?",
          positiveCommand: "yes",
          negativeCommand: "no");
    } else if (command == "stop") {
      _service.confirmIntent(
          confirmationText: "Do you want to stop?",
          positiveCommand: "yes",
          negativeCommand: "no");
    } else if (command == "hello") {
      _service.confirmIntent(
          confirmationText: "Hello to you!",
          positiveCommand: "hi",
          negativeCommand: "bye");
    } else if (command == "address") {
      _service.confirmIntent(
          confirmationText: "What is the address?",
          positiveCommand: "yes",
          negativeCommand: "no",
          voiceInputMessage: "Is the address correct?",
          voiceInput: true);
    }

    setState(() {
      confirmation = "$command [Confirmation Mode]";
    });
  }

  void updateSpeaker() {
    print("setSpeaker: pitch($_currentPitchValue) rate($_currentRateValue)");
    _service.setSpeaker(_currentPitchValue / 100, _currentRateValue / 100);
  }

  @override
  void dispose() {
    super.dispose();
    _service.stopSpeechListenService;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Background Speech-to-Text'),
        ),
        body: Center(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text('$result\n\n'),
              confirmation.isNotEmpty
                  ? Text('Confirmation: $confirmation')
                  : Container(),
              confirmationReply.isNotEmpty
                  ? Text('Reply: $confirmationReply')
                  : Container(),
              voiceReply.isNotEmpty
                  ? Text('Voice Reply: $voiceReply')
                  : Container(),
              confirmation.isNotEmpty
                  ? RaisedButton(
                      child: Text("Cancel Confirmation"),
                      onPressed: () async {
                        await _service.cancelConfirmation;

                        setState(() {
                          result = "Say something!";
                          confirmation = "";
                          confirmationReply = "";
                          voiceReply = "";
                        });
                      },
                    )
                  : Container(),
              Visibility(
                child: RaisedButton(
                  child: Text("Pause Listening"),
                  onPressed: () async {
                    await _service.pauseListening();

                    setState(() {
                      result = "Speech listener Paused!";
                      confirmation = "";
                      confirmationReply = "";
                      voiceReply = "";
                      isListening = false;
                    });
                  },
                ),
                replacement: RaisedButton(
                  child: Text("Resume Listening"),
                  onPressed: () async {
                    await _service.resumeListening();

                    setState(() {
                      result = "Speech listener Resumed!";
                      confirmation = "";
                      confirmationReply = "";
                      voiceReply = "";
                      isListening = true;
                    });
                  },
                ),
                visible: isListening,
              ),
              RaisedButton(
                child: Text("Speak"),
                onPressed: () async {
                  var t = DateTime.now();
                  await _service.speak(
                      "Hello, time is ${t.hour}:${t.minute}:${t.second}",
                      false);

                  setState(() {
                    result = "Speech listener Paused!";
                    confirmation = "";
                    confirmationReply = "";
                    voiceReply = "";
                    isListening = false;
                  });
                },
              ),
              Slider(
                value: _currentPitchValue,
                min: 0,
                max: 100,
                divisions: 10,
                label: "Pitch: ${_currentPitchValue.round().toString()}",
                onChanged: (double value) {
                  setState(() {
                    _currentPitchValue = value;
                  });
                  updateSpeaker();
                },
              ),
              Slider(
                value: _currentRateValue,
                min: 0,
                max: 100,
                divisions: 10,
                label: "Rate: ${_currentRateValue.round().toString()}",
                onChanged: (double value) {
                  setState(() {
                    _currentRateValue = value;
                  });
                  updateSpeaker();
                },
              )
            ],
          ),
        ),
      ),
    );
  }
}
