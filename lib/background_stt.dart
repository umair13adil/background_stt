import 'dart:async';
import 'dart:convert';

import 'package:background_stt/confirmation_result.dart';
import 'package:background_stt/speech_result.dart';
import 'package:flutter/services.dart';

class BackgroundStt {
  var _tag = "BackgroundStt";
  static const _channel = const MethodChannel('background_stt');
  static const _eventChannel = EventChannel('background_stt_stream');

  static SpeechResult _speechResultSaved = SpeechResult();
  static ConfirmationResult _confirmationResultSaved = ConfirmationResult();

  static Stream<SpeechResult> get speechResult =>
      _speechListenerController.stream;

  static StreamController<SpeechResult> _speechListenerController =
      StreamController<SpeechResult>();

  static Stream<ConfirmationResult> get confirmationResult =>
      _confirmationListenerController.stream;

  static StreamController<ConfirmationResult> _confirmationListenerController =
      StreamController<ConfirmationResult>();

  StreamSubscription<SpeechResult> speechSubscription =
      _speechListenerController.stream.asBroadcastStream().listen(
          (data) {
            print("DataReceived: " + data.result);
          },
          onDone: () {},
          onError: (error) {
            print("Some Error");
          });

  StreamSubscription<ConfirmationResult> confirmationSubscription =
      _confirmationListenerController.stream.asBroadcastStream().listen(
          (data) {
            print("DataReceived: " + data.confirmedResult);
          },
          onDone: () {},
          onError: (error) {
            print("Some Error");
          });

  Future<String> get startSpeechListenService async {
    final String result = await _channel.invokeMethod('startService');
    print('[$_tag] Received: $result');
    _init();
    return result;
  }

  Future<String> get cancelConfirmation async {
    final String result = await _channel.invokeMethod('cancelConfirmation');
    print('[$_tag] $result');
    return result;
  }

  Future<String> confirmIntent(
      {String confirmationText,
      String positiveCommand,
      String negativeCommand,
      String voiceInputMessage,
      bool voiceInput}) async {
    final String result =
        await _channel.invokeMethod('confirmIntent', <String, dynamic>{
      'confirmationText': confirmationText,
      'positiveCommand': positiveCommand,
      'negativeCommand': negativeCommand,
      'voiceInputMessage': voiceInputMessage,
      'voiceInput': voiceInput,
    });
    print('[$_tag] confirmIntent: $result');
    return result;
  }

  Future<String> speak(String speechText, bool queue) async {
    final String result =
        await _channel.invokeMethod('speak', <String, dynamic>{
      'speechText': speechText,
      'queue': queue,
    });
    print('[$_tag] speak: $result');
    return result;
  }

  Future<String> pauseListening() async {
    final String result = await _channel.invokeMethod('pauseListening');
    print('[$_tag] pauseListening: $result');
    return result;
  }

  Future<String> resumeListening() async {
    final String result = await _channel.invokeMethod('resumeListening');
    print('[$_tag] resumeListening: $result');
    return result;
  }

  Future<String> get stopSpeechListenService async {
    _stopSpeechListener();
    final String result = await _channel.invokeMethod('stopService');
    print('[$_tag] Received: $result');
    return result;
  }

  void _init() {
    _eventChannel.receiveBroadcastStream().listen((dynamic event) {
      if (event.toString().contains("isPartial") &&
          !event.toString().contains("confirmationIntent")) {
        Map result = jsonDecode(event);
        _speechResultSaved = SpeechResult.fromJson(result);
        _speechListenerController.add(_speechResultSaved);
      } else if (!event.toString().contains("isPartial") &&
          event.toString().contains("confirmationIntent")) {
        Map result = jsonDecode(event);
        _confirmationResultSaved = ConfirmationResult.fromJson(result);
        if (_confirmationResultSaved.confirmationIntent != null &&
            _confirmationResultSaved.confirmationIntent.isNotEmpty) {
          _confirmationListenerController.add(_confirmationResultSaved);
        }
      }
    }, onError: (dynamic error) {});
  }

  StreamSubscription<SpeechResult> getSpeechResults() {
    return speechSubscription;
  }

  StreamSubscription<ConfirmationResult> getConfirmationResults() {
    return confirmationSubscription;
  }

  void _stopSpeechListener() {
    _speechListenerController.close();
    _confirmationListenerController.close();
    speechSubscription.cancel();
    confirmationSubscription.cancel();
  }
}
