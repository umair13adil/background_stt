# background_stt
#### Speech-to-Text, Text-to-speech, Interactive Voice commands
#### [Android Support Only]

[![pub package](https://img.shields.io/pub/v/background_stt)](https://pub.dev/packages/background_stt)

An flutter plugin that runs Speech-to-Text continously in background. This plugin operates in 2 modes:

#### 1. STT Mode
  Voice commands will be recognized in background and sent to Flutter app.

#### 2. Confirmation Mode
  Voice commands will be recognized and if confirmation was requested for the command, confirmation flow will begin. Once user has provided with "Positive" or "Negative" reply, confirmation mode will end delivering voice input results to flutter app.

Speech results will be delivered in real-time to the flutter app.

![Demo](pictures/picture1.gif)

## Install
In your pubspec.yaml

```yaml
dependencies:
  background_stt: [LATEST_VERSION]
```

```dart
import 'package:background_stt/background_stt.dart';
```

## How to use it?

#### Start Speech-to-Text Service
__________________________________

Note: Service will keep on running once it is started and can only be stopped by calling stop service method.

```dart
 var _service = BackgroundStt();

 _service.startSpeechListenService;
    _service.getSpeechResults().onData((data) {
      print("getSpeechResults: ${data.result} , ${data.isPartial}");
    });
```

#### Setup Intent for Simple Confirmation
__________________________________________

This can be useful for simple decisions.

```dart
    _service.getSpeechResults().onData((data) {
          if (command == "start") {
          _service.confirmIntent(
              confirmationText: "Do you want to start?",
              positiveCommand: "yes",
              negativeCommand: "no");
        }
    });

  _service.getConfirmationResults().onData((data) {
      print(
          "getConfirmationResults: Confirmation Text: ${data.confirmationIntent} , "
          "User Replied: ${data.confirmedResult} , "
          "Is Confirmation Success?: ${data.isSuccess}");
    });
```

#### Setup Intent for Voice-Input Confirmation
______________________________________________

This can be useful for taking voice input from user and then confirming that input from user by voice command verification.

```dart
    _service.getSpeechResults().onData((data) {
          if (command == "address") {
            _service.confirmIntent(
                confirmationText: "What is the address?",
                positiveCommand: "yes",
                negativeCommand: "no",
                voiceInputMessage: "Is the address correct?",
                voiceInput: true);
          }
    });

  _service.getConfirmationResults().onData((data) {
      print(
          "getConfirmationResults: Confirmation Text: ${data.confirmationIntent} , "
          "User Replied: ${data.confirmedResult} , "
          "Voice Input Message: ${data.voiceInput} , "
          "Is Confirmation Success?: ${data.isSuccess}");
    });
```

#### Cancel Confirmation in progress
_____________________________________

```dart
  await _service.cancelConfirmation;
```

#### Stop Speech-to-Text Service
__________________________________

```dart
  _service.stopSpeechListenService;
```

#### Pause Speech-to-Text Listener
__________________________________

```dart
  await _service.pauseListening();
```

#### Resume Speech-to-Text Listener
__________________________________

```dart
  await _service.resumeListening();
```

#### Speak Text and Interupt Listener
__________________________________

If you want to queue voice messages, send queue value 'true':

```dart
  await _service.speak("Hello",true)
```

To interupt and speak current playing message, send queue value 'false':

```dart
  await _service.speak("Hello",false)
```

Set speaker pitch and speech rate:

```dart
  _service.setSpeaker(0.2, 0.5)
```

# Author

background_stt plugin is developed by Umair Adil. You can email me at <m.umair.adil@gmail.com> for any queries.
