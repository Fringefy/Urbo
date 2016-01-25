
#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>
#import <CoreMotion/CoreMotion.h>
#import "PexesoBridge.h"
#import "RecoEvent.h"

@class POI;

@protocol UrboDelegate;

@interface Urbo : NSObject <CLLocationManagerDelegate,PexesoDelegate>

@property (strong, nonatomic) PexesoBridge *pexeso;
@property (nonatomic) CLLocationManager *locationManager;
@property (nonatomic) NSMutableArray *poisArray;
@property (nonatomic) id<UrboDelegate> delegate;
@property (nonatomic) RecoEvent *recoEvent;
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
+ (void) startWithApiKey:(NSString *) apiKey;
- (BOOL) takeSnapshot;
- (BOOL) getSnapshot:(long) snapshotId;
- (void) tagSnapshot:(Snapshot*)snapshot poi:(POI *)poi;
- (NSArray *) getPoiShortlist;
- (BOOL) confirmRecognition:(long) snapshotId;
- (BOOL) rejectRecognition:(long) snapshotId;
- (void) restartLiveFeed;
- (void) forceCacheRefresh;

@end

@protocol UrboDelegate <NSObject>

- (void) urboDidChangeState:(StateId) state
                    withPoi:(POI *)poi
              andSnapshotId:(int) snapshotId;
- (void) urboOnSnapshot:(Snapshot *)snapshot;
@optional
- (void) urboApiMessage:(int)type response:(id) response;

@end