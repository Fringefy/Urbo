#import "APIClient.h"

@interface RecoEvent : NSObject

@property (nonatomic) NSNumber *clientTimestamp;
@property (nonatomic) NSString *userSelectedPoi;
@property (nonatomic) NSString *machineSelectedPoi;
@property (nonatomic) NSArray  *loc;
@property (nonatomic) NSNumber *locAccuracy;
@property (nonatomic) NSNumber *pitch;
@property (nonatomic) NSNumber *camAzimuth;
@property (nonatomic) NSString *clientGeneratedUNA;
@property (nonatomic) NSNumber *isIndex;
@property (nonatomic) NSNumber *userFeedback;
@property (nonatomic) NSString *deviceID;
@property (nonatomic) NSString *imgFileName;
- (NSDictionary *)dictionaryRepresentation;

@end
