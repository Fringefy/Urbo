#import <CoreLocation/CoreLocation.h>
#import "APIClient.h"

@interface RecoEvent : NSObject

@property (nonatomic) NSNumber *clientTimestamp;
@property (nonatomic) NSString *userSelectedPoi;
@property (nonatomic) NSString *machineSelectedPoi;
@property (nonatomic) NSArray *loc;
@property (nonatomic) NSNumber *locAccuracy;
@property (nonatomic) NSArray *deviceOrientation;
@property (nonatomic) NSString *clientGeneratedUNA;
@property (nonatomic) NSNumber *isIndex;
@property (nonatomic) NSNumber *userFeedback;
@property (nonatomic) NSNumber *clientCalculationMillisec;
@property (nonatomic) NSString *deviceID;
@property (nonatomic) NSString *imgFileName;

- (NSDictionary *)dictionaryRepresentation;

@end
