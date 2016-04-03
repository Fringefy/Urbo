
#import "JSONObject.h"

@interface POI : JSONObject 

@property (nonatomic, strong) NSString *poiId; // @"_id"
@property (nonatomic, strong) NSString *clientId;
@property (nonatomic, strong) NSString *address;
@property (nonatomic, strong) NSString *poiDescription; // @"description"
@property (nonatomic, strong) NSString *locality;
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong) NSNumber *type;
@property (nonatomic, strong) NSArray  *facade; // @"usig/geo/facade"
@property (nonatomic, strong) NSMutableArray *unas; // @"usig/unas"
@property (nonatomic, strong) NSArray *loc;
@property (nonatomic, strong) NSNumber *timestamp;
@property (nonatomic, strong) NSString *firstComment;
@property (nonatomic, strong) NSString *imgFileName;
@property (nonatomic, strong) NSMutableDictionary  *urls;

+ (POI*) poiWithJSON:(id)JSON;
- (instancetype)init:(NSString*)name;
- (NSDictionary *)dictionaryRepresentation;
- (NSString *) getId;
- (BOOL) isClientOnly;

@end
