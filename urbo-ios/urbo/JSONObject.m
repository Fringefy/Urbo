#import "JSONObject.h"
#import <objc/runtime.h>

@interface JSONObject ()
@end


@implementation JSONObject

- (id)initWithJSON:(id)poiJson {
    self = [self init];
    if (self){
        [self setValuesForKeysWithDictionary:poiJson];
    }
    return self;
}
- (void)setValue:(id)value forUndefinedKey:(NSString *)key{
    
}

- (NSString *)description{
    return [NSString stringWithFormat:@"(%@): %@",
            [super description],
            [self descriptionString]];
}

- (NSString *)descriptionString {
    NSString *description = @"";
    unsigned int outCount, i;
    objc_property_t *properties = class_copyPropertyList([self class], &outCount);
    for(i = 0; i < outCount; i++) {
        objc_property_t property = properties[i];
        const char *propName = property_getName(property);
        if (propName) {
            NSString *propertyName = [NSString stringWithUTF8String:propName];
            id propertyValue = [self valueForKey:propertyName];
            NSString *className = NSStringFromClass([propertyName class]);
            NSString *address = [NSString stringWithFormat:@"%p", propertyValue];
            NSString *valueDescription = [propertyValue description];
            description = [description stringByAppendingFormat:@"<%@(%@: %@): %@>, ",
                           propertyName,
                           className,
                           address,
                           valueDescription];
        }
    }
    if ([description hasSuffix:@", "])
        description = [description stringByReplacingCharactersInRange:
                       NSMakeRange(description.length-2, 2) withString:@""];
    else
        description = [description stringByAppendingString:@""];
    
    free(properties);
    return description;
}

@end