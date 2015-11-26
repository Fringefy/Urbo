
#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>
#import <CoreMotion/CoreMotion.h>
#import "PexesoBridge.h"

@class POI;

@protocol UrboDelegate;

@interface Urbo : NSObject <CLLocationManagerDelegate,PexesoDelegate>

@property (strong, nonatomic) PexesoBridge *pexeso;
@property (nonatomic) CLLocationManager *locationManager;
@property (nonatomic) NSMutableArray *poisArray;
@property (nonatomic) id<UrboDelegate> delegate;
@property (nonatomic) CMMotionManager *manager;
@property (nonatomic) NSString *deviceId;

+ (Urbo *) sharedInstance;

@end

@protocol UrboDelegate <NSObject>

- (void) urbo:(Urbo *)urbo didChangeState:(State) state withPoi:(POI *) poi;

@end