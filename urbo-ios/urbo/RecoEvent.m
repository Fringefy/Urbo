#import "RecoEvent.h"
#import <objc/runtime.h>
#import "Urbo.h"

@implementation RecoEvent

- (instancetype)init
{
    self = [super init];
    if (self) {  
        
    }
    return self;
}

- (NSDictionary *)dictionaryRepresentation {
    unsigned int count = 0;
    objc_property_t *properties = class_copyPropertyList([self class], &count);
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] initWithCapacity:count];
    for (int i = 0; i < count; i++) {
        NSString *key = [NSString stringWithUTF8String:property_getName(properties[i])];
        NSString *value = [self valueForKey:key];
        if (value)
            [dict setObject:value forKey:key];
    }
    free(properties);
    return dict;
}

-(instancetype)initWithLocation:(CLLocation *)location
                          pitch:(float)pitch
                        azimuth:(float)azimuth
                    selectedPoi:(NSString *)poiId
                      clientUNA:(NSString *)clientUNA
{
    self = [super init];
    if (self)
    {
        self.deviceID = [Urbo getInstance].deviceId;
        self.clientTimestamp = @((long)([[NSDate date] timeIntervalSince1970] * 1000));
        self.imgFileName = [NSString stringWithFormat:@"%@.%li.jpg",
                            self.deviceID,
                            [self.clientTimestamp longValue]];
        self.loc = @[@(location.coordinate.longitude),@(location.coordinate.latitude)];
        self.locAccuracy = @(location.horizontalAccuracy);
        self.pitch = @(pitch);
        self.camAzimuth = @(azimuth);
        if (poiId == nil)
            self.machineSelectedPoi = @"";
        else
            self.machineSelectedPoi = poiId;
        self.clientGeneratedUNA = clientUNA;
    }
    return self;
}

@end
