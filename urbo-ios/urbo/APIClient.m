#import "APIClient.h"
#import "Urbo.h"

@interface APIClient ()

@end

@implementation APIClient

+ (instancetype)sharedClient {
    static APIClient *sharedClient = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedClient = [[APIClient alloc] initWithBaseURL:
                        [NSURL URLWithString:[Urbo getInstance].baseUrl]];
        [sharedClient setupHTTPserializer];
    });
    return sharedClient;
}

-(void) setupHTTPserializer
{
    [self setRequestSerializer:[AFHTTPRequestSerializer serializer]];
    [self.requestSerializer setValue:@"application/json"
                  forHTTPHeaderField:@"Accept"];
    [self.requestSerializer setValue:[Urbo getInstance].apiKey
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
    self.task = [self GET:@"/odie/pois" parameters:param progress:nil
                  success:^(NSURLSessionTask *task, id responseObject) {
                      success(task, responseObject);
                  }
                  failure:^(NSURLSessionTask *task, NSError *error) {
                      failure(task,error);
                  }];
}

-(void)sendPoisWithEvents:(NSDictionary *)poisWithEvents
                  success:(SuccessBlock)success
                  failure:(FailureBlock)failure
{
    [self setRequestSerializer:[AFJSONRequestSerializer serializer]];
    [self.requestSerializer setValue:@"application/json"
                  forHTTPHeaderField:@"Accept"];
    [self.requestSerializer setValue:@"application/json"
                  forHTTPHeaderField:@"Content-Type"];
    [self.requestSerializer setValue:[Urbo getInstance].apiKey
                  forHTTPHeaderField:@"x-api-key"];
    self.task = [self PUT:@"/odie/pois"
               parameters:poisWithEvents
                  success:^(NSURLSessionTask *task, id responseObject) {
                      success(task, responseObject);
                  }
                  failure:^(NSURLSessionTask *task, NSError *error) {
                      failure(task,error);
                  }];
}

@end
