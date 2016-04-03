#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreLocation/CoreLocation.h>

#define PlatformPoi   CFTypeRef
#define PlatformImage CFTypeRef
#define ImgBuffer     CFTypeRef

#ifdef __cplusplus
#include "IPexeso.hpp"
#endif

@class POI;
@class Snapshot;
@class Urbo;

@interface PexesoBridge : NSObject

- (void) initPexeso:(Urbo *) urbo;
- (void) initLiveFeed:(int) width  height:(int) height;
- (void) pushFrame:(CMSampleBufferRef) byteBuff;
- (void) pushPitch:(float) pitch;
- (void) pushLocation:(CLLocation*) location;
- (void) pushHeading:(float) heading;
- (void) poiCacheRequestCallback:(int) requestId
						location:(CLLocation *) location
							pois:(NSArray*) poisList;
- (void) stopLiveFeed;
- (void) tagSnapshot:(Snapshot *) snapshot poi:(POI *)poi;
- (NSArray *) getPoiShortlist;
- (CLLocation *) getCurrentLocation;
- (void) confirmRecognition:(Snapshot *) snapshot;
- (void) rejectRecognition:(Snapshot *) snapshot;
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

