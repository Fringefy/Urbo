#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreLocation/CoreLocation.h>
#ifdef __cplusplus
#include "IPexeso.hpp"
#endif

@class POI;
@class Snapshot;

@protocol PexesoDelegate;

@interface PexesoBridge : NSObject

- (void) initPexeso:(id<PexesoDelegate>) delegate;
- (void) initLiveFeed:(int) width  height:(int) height;
- (void) pushFrame:(CMSampleBufferRef) byteBuff;
- (void) pushPitch:(float) pitch;
- (void) pushLocation:(CLLocation*) location;
- (void) pushHeading:(float) heading;
- (void) poiCacheRequestCallback:(int) requestId
                        location:(CLLocation *) location
                            pois:(NSArray*) poisList;
- (void) stopLiveFeed;
- (BOOL) tagSnapshot:(Snapshot *)oSnap poi:(POI *)oPoi;
- (NSArray *) getPoiShortlist;
- (BOOL) confirmRecognition:(long) snapshotId;
- (BOOL) rejectRecognition:(long) snapshotId;
- (BOOL) getSnapshot:(long) snapshotId;
- (BOOL) takeSnapshot;
- (void) forceCacheRefresh;
- (void) poiCacheUpdateCallback:(NSDictionary *)response;
- (void) restartLiveFeed;

typedef enum StateId StateId;

typedef enum {
    CODE_DBG = 0,
    CODE_INFORMATION = 1,
    CODE_WARNING = 2,
    CODE_ERROR = 3,
    CODE_MAX_SEVERITY = 4
} SeverityCode;

@end

@protocol PexesoDelegate <NSObject>

- (void) pexesoDidGetError:(SeverityCode) errorCode message:(NSString *) message;
- (void) pexesoDidRequestListener:(int) requestId withLocation:(CLLocation *) location;
- (void) pexesoDidChangeState:(StateId)state withPoi:(POI *)poi andSnapshotId:(int) snapshotId;
- (void) pexesoOnSnapshot:(Snapshot *) snapshot;
- (void) pexesoOnRecognition:(Snapshot *) snapshot;

@end
