import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:background_stt/background_stt.dart';

void main() {
  const MethodChannel channel = MethodChannel('background_stt');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {

  });
}
