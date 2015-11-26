#import "POI.h"
#import <objc/runtime.h>
#import "UrboCameraView.h"

@implementation POI

+ (POI *)poiWithJSON:(id)JSON;
{
	return [[self alloc] initWithJSON:JSON];
}

-(void)setValue:(id)value forUndefinedKey:(NSString *)key
{
	if ([key isEqualToString:@"_id"])
		self.poiId = value;
	if ([key isEqualToString:@"clientId"])
		self.poiId = value;
	if ([key isEqualToString:@"description"])
		self.poiDescription = value;
}

-(void)setValue:(id)value forKey:(NSString *)key
{
	if ([key isEqualToString:@"usig"])
	{
		NSMutableArray *tempArray = [[NSMutableArray alloc] init];
		for (NSDictionary *dictionary in value[@"unas"])
		{
			UNA *una = [UNA unaWithJSON:dictionary];
			[tempArray addObject:una];
		}
		self.usig = tempArray;
	}
	else if ([key isEqualToString:@"loc"])
	{
		self.loc = value;
		self.location = [[CLLocation alloc]
						 initWithLatitude:[[value objectAtIndex:1] floatValue]
						 longitude:[[value objectAtIndex:0] floatValue]];
	}
	else
	{
		[super setValue:value forKey:key];
	}
}

- (NSDictionary *)dictionaryRepresentation {
	self.timestamp = [NSNumber numberWithLongLong:
					  [[NSDate date] timeIntervalSince1970] * 1000];
	unsigned int count = 0;
	objc_property_t *properties = class_copyPropertyList([self class], &count);
	NSMutableDictionary *dict = [[NSMutableDictionary alloc] initWithCapacity:count];
	for (int i = 0; i < count; i++) {
		NSString *key = [NSString stringWithUTF8String:property_getName(properties[i])];
		NSString *value = [self valueForKey:key];
		if (value)
		{
			if ([key isEqualToString:@"poiId"])
			{
				if (self.isNew)
					key = @"clientId";
				else
					key = @"_id";
				[dict setObject:value forKey:key];
			}
			else if ([key isEqualToString:@"usig"])
			{
				NSMutableArray *tempArray = [[NSMutableArray alloc] init];
				for (UNA *una in (NSArray *)value)
				{
					NSDictionary *unaDic = [una dictionaryRepresentation];
					[tempArray addObject:unaDic];
				}
				[dict setObject:@{@"unas":tempArray} forKey:key];
			}
			else if (!([key isEqualToString:@"distance"]
					   || [key isEqualToString:@"index"]
					   || [key isEqualToString:@"score"]
					   || [key isEqualToString:@"geoScore"]
					   || [key isEqualToString:@"location"]
					   || [key isEqualToString:@"isRecognised"]
					   || [key isEqualToString:@"isNew" ]
					   || [key isEqualToString:@"imageFileName"]
					   || [key isEqualToString:@"actionDate"]
					   || [key isEqualToString:@"actionType"]
					   || [key isEqualToString:@"actionDateWithTime"]))
			{
				[dict setObject:value forKey:key];
			}
		}
	}
	
	free(properties);
	
	return dict;
}

@end
