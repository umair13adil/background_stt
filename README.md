# background_stt
#### [Android Support Only]

[![pub package](https://img.shields.io/pub/v/background_stt)](https://pub.dev/packages/background_stt)

An flutter plugin that runs Speech-to-Text continously in background. Speech results will be delivered in real-time to the flutter app.

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

#### Stop Speech-to-Text Service
__________________________________

```dart
 _service.stopSpeechListenService;
```

# Author

background_stt plugin is developed by Umair Adil. You can email me at <m.umair.adil@gmail.com> for any queries.
