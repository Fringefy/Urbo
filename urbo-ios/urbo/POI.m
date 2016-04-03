#import "POI.h"
#import <objc/runtime.h>

@implementation POI

+ (POI *)poiWithJSON:(id)poiJson;
{
    return [[self alloc] initWithJSON:poiJson];
}

-(instancetype)init:(NSString*)name
{
    self = [super init];
    if (self) {
        self.name = name;
        self.type = 0;
    }
    return self;
}

-(void)setValue:(id)value forUndefinedKey:(NSString *)key
{
    if ([key isEqualToString:@"_id"]) {
        self.poiId = value;
    }
    else if ([key isEqualToString:@"description"]) {
        self.poiDescription = value;
    }
    else if ([key isEqualToString:@"usig"]) {
        self.unas = value[@"unas"];
        self.facade = value[@"geo"][@"facade"];
    }
    else if ([[key substringFromIndex:[key length] - 3] isEqualToString:@"Url"]) {
        if (self.urls == nil) {
            self.urls = [[NSMutableDictionary alloc] init];
        }
        self.urls[key] = value;
    }
}

-(NSString *) getId
{
    return [self isClientOnly] ? self.clientId : self.poiId;
}

- (BOOL) isClientOnly
{
    return self.poiId == nil;
}

- (NSDictionary *)dictionaryRepresentation {
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];

    unsigned int count = 0;
    objc_property_t *properties = class_copyPropertyList([self class], &count);
    for (int i = 0; i < count; i++) {
        NSString *key = [NSString stringWithUTF8String:property_getName(properties[i])];
        NSString *value = [self valueForKey:key];
        if (value)
        {
            if ([key isEqualToString:@"poiId"])
            {
                [dict setObject:value forKey:@"_id"];
            }
            else if ([key isEqualToString:@"poiDescription"])
            {
                [dict setObject:value forKey:@"description"];
            }
            else if ([[NSArray arrayWithObjects:
                       @"clientId",
                       @"name",
                       @"address",
                       @"locality",
                       @"type",
                       @"loc",
                       @"imgFileName",
                       @"timestamp",
                       @"firstComment",
                       nil] containsObject:key])
            {
                [dict setObject:value forKey:key];
            }
        }
    }
    if (self.urls != nil) {
        for (NSString *key in self.urls.keyEnumerator) {
            NSString *value = [self.urls valueForKey:key];
            [dict setObject:value forKey:key];
        }
    }

    free(properties);
    
    return dict;
}

@end
