#import "APIClient.h"

#define apiKey @"RnNmIgxtZzcajIZww7NlKnAeYwTjOq9xp9Xu7YkS"

@interface APIClient ()

@end

@implementation APIClient

+ (instancetype)sharedClient {
	static APIClient *sharedClient = nil;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		sharedClient = [[APIClient alloc] initWithBaseURL:
						[NSURL URLWithString:@"https://qaodie.fringefy.com/"]];
		[sharedClient setupHTTPserializer];
	});
	return sharedClient;
}

-(void) setupHTTPserializer
{
	[self setRequestSerializer:[AFHTTPRequestSerializer serializer]];
	[self.requestSerializer setValue:@"application/json"
				  forHTTPHeaderField:@"Accept"];
	[self.requestSerializer setValue:apiKey
				  forHTTPHeaderField:@"x-api-key"];
	[self.requestSerializer setValue:@"application/x-www-form-urlencoded"
				  forHTTPHeaderField:@"Content-Type"];
	[self.requestSerializer setTimeoutInterval:15];
}

-(void)getPOIs:(NSNumber *)lng
		   lat:(NSNumber *)lat
	  deviceId:(NSString *)deviceId
	  accuracy:(NSNumber *)accuracy
	   success:(SuccessBlock)success
	   failure:(FailureBlock)failure
{
	NSDictionary *param = @{@"lat":lat,
							@"lng":lng,
							@"deviceid":deviceId,
							@"accuracy":accuracy};
	[self setupHTTPserializer];
	self.operation = [self GET:@"/odie/pois" parameters:param
					   success:^(AFHTTPRequestOperation *operation, id responseObject) {
						   success(operation, responseObject);
					   }
					   failure:^(AFHTTPRequestOperation *operation, NSError *error) {
						   failure(operation,error);
					   }];
}

-(void)sendPoisWithEvents:(NSDictionary *)poisWithEvents
				  success:(SuccessBlock)success
				  failure:(FailureBlock)failure
{
	[self setRequestSerializer:[AFJSONRequestSerializer serializer]];
	self.operation = [self PUT:@"/odie/pois"
					parameters:poisWithEvents
					   success:^(AFHTTPRequestOperation *operation, id responseObject) {
						   success(operation, responseObject);
					   }
					   failure:^(AFHTTPRequestOperation *operation, NSError *error) {
						   failure(operation,error);
					   }];
}

@end
