#import "AFHTTPSessionManager.h"
#import "AFNetworkActivityIndicatorManager.h"

typedef void (^SuccessBlock)(NSURLSessionTask *task, id responseObject);
typedef void (^FailureBlock)(NSURLSessionTask *task, NSError *error);

@interface APIClient : AFHTTPSessionManager

@property (strong, nonatomic) NSURLSessionTask *task;

+ (instancetype)sharedClient;

-(void)getPOIs:(NSNumber *)lng
           lat:(NSNumber *)lat
      deviceId:(NSString *)deviceId
      accuracy:(NSNumber *)accuracy
       success:(SuccessBlock)success
       failure:(FailureBlock)failure;

-(void)sendPoisWithEvents:(NSDictionary *)poisWithEvents
                  success:(SuccessBlock)success
                  failure:(FailureBlock)failure;

@end
