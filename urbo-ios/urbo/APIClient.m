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
                        [NSURL URLWithString:[Urbo getInstance].sEndpoint]];
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

-(void)getPois:(SuccessBlock)success
       withLat:(NSNumber *)lat
       andLong:(NSNumber *)lng
      accuracy:(NSNumber *)accuracy
{
    [self getPois:success withLat:lat andLong:lng accuracy:accuracy
        failure:^(NSURLSessionTask *task, NSError *error) {}
    ];
}

-(void)getPois:(SuccessBlock)success
    withLat:(NSNumber *)lat
    andLong:(NSNumber *)lng
    accuracy:(NSNumber *)accuracy
    failure:(FailureBlock)failure
{
    NSDictionary *param = @{@"lat":lat,
                            @"lng":lng,
                            @"deviceid":[Urbo getInstance].deviceId,
                            @"accuracy":accuracy};
    self.task = [self GET:@"pois" parameters:param progress:nil
                  success:^(NSURLSessionTask *task, id responseObject) {
                      success(task, responseObject);
                  }
                  failure:^(NSURLSessionTask *task, NSError *error) {
                      failure(task, error);
                  }];
}

-(void)sync:(SuccessBlock)success
    withRecoEvents:(NSDictionary *)poisWithEvents
{
    [self sync:success withRecoEvents:poisWithEvents
        failure: ^(NSURLSessionTask *task, NSError *error) {}];
}

-(void)sync:(SuccessBlock)success
    withRecoEvents:(NSDictionary *)poisWithEvents
    failure:(FailureBlock)failure
{
    [self setRequestSerializer:[AFJSONRequestSerializer serializer]];
    [self.requestSerializer setValue:@"application/json"
                  forHTTPHeaderField:@"Accept"];
    [self.requestSerializer setValue:@"application/json"
                  forHTTPHeaderField:@"Content-Type"];
    [self.requestSerializer setValue:[Urbo getInstance].apiKey
                  forHTTPHeaderField:@"x-api-key"];
    self.task = [self PUT:@"pois"
               parameters:poisWithEvents
                  success:^(NSURLSessionTask *task, id responseObject) {
                      success(task, responseObject);
                  }
                  failure:^(NSURLSessionTask *task, NSError *error) {
                      failure(task,error);
                  }];
}

@end
