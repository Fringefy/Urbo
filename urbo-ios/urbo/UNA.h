#import "JSONObject.h"

#import <Foundation/Foundation.h>

@interface UNA : JSONObject

@property (nonatomic) NSNumber *azimuth;
@property (nonatomic) NSString *data;

+ (UNA*) unaWithJSON:(id)JSON;

- (NSDictionary *)dictionaryRepresentation;

@end
