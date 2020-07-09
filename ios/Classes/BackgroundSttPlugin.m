#import "BackgroundSttPlugin.h"
#if __has_include(<background_stt/background_stt-Swift.h>)
#import <background_stt/background_stt-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "background_stt-Swift.h"
#endif

@implementation BackgroundSttPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBackgroundSttPlugin registerWithRegistrar:registrar];
}
@end
