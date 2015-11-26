#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreLocation/CoreLocation.h>

@interface UrboCameraView : UIView <AVCaptureVideoDataOutputSampleBufferDelegate>

- (void) unFreeze;
- (void) freeze;
- (void) setFrontCamera:(BOOL) useFront;

@end