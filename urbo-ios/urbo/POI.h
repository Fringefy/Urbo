
#import "JSONObject.h"
#import <CoreLocation/CoreLocation.h>

@interface POI : JSONObject 

@property (nonatomic, strong) NSString *poiId;
@property (nonatomic, strong) NSString *clientId;
@property (nonatomic, strong) NSString *address;
@property (nonatomic, strong) NSString *poiDescription;
@property (nonatomic, strong) NSString *homeUrl;
@property (nonatomic, strong) NSString *tripadvisorUrl;
@property (nonatomic, strong) NSString *yelpUrl;
@property (nonatomic, strong) NSString *foursquareUrl;
@property (nonatomic, strong) NSString *wikipediaUrl;
@property (nonatomic, strong) NSString *gogobotUrl;
@property (nonatomic, strong) NSString *facebookUrl;
@property (nonatomic, strong) NSString *twitterUrl;
@property (nonatomic, strong) NSString *opentableUrl;
@property (nonatomic, strong) NSString *grubhubUrl;
@property (nonatomic, strong) NSString *factualId;
@property (nonatomic, strong) NSString *locality;
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong) NSNumber *type;
@property (nonatomic) int distance;
@property (nonatomic) double score;
@property (nonatomic) NSMutableArray* usig;
@property (nonatomic) NSArray* loc;
@property (nonatomic) CLLocation *location;
@property (nonatomic) double geoScore;
@property (nonatomic) NSNumber *timestamp;
@property (nonatomic) NSString *firstComment;
@property (nonatomic) NSString *imgFileName;
@property (nonatomic, strong) NSString *aolUrl;
@property (nonatomic, strong) NSString *eventfulUrl;
@property (nonatomic, strong) NSString *foodfinderUrl;
@property (nonatomic, strong) NSString *hotelsUrl;
@property (nonatomic, strong) NSString *hotelcombinedUrl;
@property (nonatomic, strong) NSString *instagramUrl;
@property (nonatomic, strong) NSString *openmenuUrl;
@property (nonatomic, strong) NSString *superpagesUrl;
@property (nonatomic, strong) NSString *yahoogeoplanetUrl;
@property (nonatomic, strong) NSString *yellowpagesUrl;
@property (nonatomic, strong) NSString *zagatUrl;
@property (nonatomic, strong) NSString *happyintlvUrl;
@property (nonatomic, strong) NSDate *actionDate;
@property (nonatomic, strong) NSDate *actionDateWithTime;
@property (nonatomic, strong) NSNumber *actionType;

+ (POI*) poiWithJSON:(id)JSON;
- (instancetype)init:(NSString*)name;
- (NSDictionary *)dictionaryRepresentation;
- (NSString *) getId;
- (BOOL) isClientOnly;

@end
