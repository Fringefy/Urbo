
#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>
#import <CoreMotion/CoreMotion.h>
#import "PexesoBridge.h"

@class POI;

@protocol UrboDelegate;

@interface Urbo : NSObject <CLLocationManagerDelegate>

@property (strong, nonatomic) PexesoBridge *pexeso;
@property (nonatomic) CLLocationManager *locationManager;
@property (nonatomic) id<UrboDelegate> delegate;
@property (nonatomic) CMMotionManager *manager;
@property (nonatomic) NSString *deviceId;
@property (nonatomic) NSString *apiKey;
@property (nonatomic) NSString *baseUrl;
@property (nonatomic) NSString *sharepageUrl;
@property (nonatomic) NSString *tabpageUrl;
@property (nonatomic) NSString *s3Bucket;
@property (nonatomic) NSString *s3Folder;
@property (nonatomic) NSString *mappageUrl;
@property (nonatomic) NSString *downsizedUrl;

+ (Urbo *) getInstance;
+ (void) start:(id<UrboDelegate>)delegate withApiKey:(NSString *) apiKey;
- (BOOL) takeSnapshot;
- (void) tagSnapshot:(Snapshot*)snapshot poi:(POI *)poi;
- (NSArray *) getPoiShortlist;
- (CLLocation *) getCurrentLocation;
- (void) confirmRecognition:(Snapshot*) snapshot;
- (void) rejectRecognition:(Snapshot*) snapshot;
- (void) restartLiveFeed;
- (void) forceCacheRefresh;

- (void) sendRecoEvent:(Snapshot *)snapshot;
- (void) sendCacheRequest:(int)requestId withLocation:(CLLocation *)location;

@end

@protocol UrboDelegate <NSObject>

- (void) onError:(SeverityCode) errorCode message:(NSString *) message;
- (void) onStateChanged:(StateId)stateId withSnapshot:(Snapshot *)snapshot;
- (void) onSnapshot:(Snapshot *)snapshot;

@end