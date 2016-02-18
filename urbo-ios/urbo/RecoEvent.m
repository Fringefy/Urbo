#import "RecoEvent.h"
#import <objc/runtime.h>
#import "Urbo.h"

@implementation RecoEvent

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

@end
