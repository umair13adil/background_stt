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

  @override
  void initState() {
    _service.startSpeechListenService;
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
                  : Container()
            ],
          ),
        ),
      ),
    );
  }
}
