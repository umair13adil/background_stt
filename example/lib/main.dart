import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
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
  var result = "";

  @override
  void initState() {
    _service.startSpeechListenService;
    _service.getSpeechResults().onData((data) {
      print("getSpeechResults: ${data.result} , ${data.isPartial}");

      setState(() {
        result = data.result;
      });
    });
    super.initState();
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
