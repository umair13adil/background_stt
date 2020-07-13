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

  @override
  void initState() {
    _service.startSpeechListenService;
    _service.getSpeechResults().onData((data) {
      print("getSpeechResults: ${data.result} , ${data.isPartial}");

      _doOnSpeechCommandMatch(data.result);

      setState(() {
        result = data.result;
      });
    });
    super.initState();
  }

  void _doOnSpeechCommandMatch(String command) {
    if (command == "start") {
      _service.confirmIntent("Do you want to start?");
    } else if (command == "stop") {
      _service.confirmIntent("Do you want to stop?");
    } else if (command == "hello") {
      _service.confirmIntent("Did you say hello?");
    }
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
          child: Text('$result'),
        ),
      ),
    );
  }
}
