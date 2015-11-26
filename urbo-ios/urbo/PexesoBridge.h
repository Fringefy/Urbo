#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreLocation/CoreLocation.h>
#ifdef __cplusplus
#include "IPexeso.hpp"
#endif

@class POI;

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

typedef enum {
	STATE_SEARCH = 0,
	STATE_RECOGNITION = 1,
	STATE_NO_RECOGNITION = 2,
	STATE_NON_INDEXABLE = 3,
	STATE_BAD_ORIENTATION = 4,
	STATE_MOVING = 5
} State;

typedef enum {
	CODE_DBG = 0,
	CODE_INFORMATION = 1,
	CODE_WARNING = 2,
	CODE_ERROR = 3,
	CODE_MAX_SEVERITY = 4
} SeverityCode;

@end

@protocol PexesoDelegate <NSObject>

- (void) pexesoDidGetError:(SeverityCode) errorCode;
- (void) pexesoDidRequestListener:(int) requestId withLocation:(CLLocation *) location;
- (void) pexesoDidChangeState:(State) state withPoi:(POI *) poi;

@end
