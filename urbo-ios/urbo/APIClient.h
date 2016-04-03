#import "AFHTTPSessionManager.h"
#import "AFNetworkActivityIndicatorManager.h"

typedef void (^SuccessBlock)(NSURLSessionTask *task, id responseObject);
typedef void (^FailureBlock)(NSURLSessionTask *task, NSError *error);

@interface APIClient : AFHTTPSessionManager

@property (strong, nonatomic) NSURLSessionTask *task;

+ (instancetype)sharedClient;

-(void)getPois:(SuccessBlock)success
	withLat:(NSNumber *)lat
	andLong:(NSNumber *)lng
	accuracy:(NSNumber *)accuracy;

-(void)getPois:(SuccessBlock)success
	withLat:(NSNumber *)lat
	andLong:(NSNumber *)lng
	accuracy:(NSNumber *)accuracy
	failure:(FailureBlock)failure;

-(void)sync:(SuccessBlock)success
	withRecoEvents:(NSDictionary *)poisWithEvents;

-(void)sync:(SuccessBlock)success
	withRecoEvents:(NSDictionary *)poisWithEvents
	failure:(FailureBlock)failure;

@end
