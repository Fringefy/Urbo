#import "AFHTTPSessionManager.h"
#import "AFHTTPRequestOperationManager.h"
#import "AFNetworkActivityIndicatorManager.h"

typedef void (^SuccessBlock)(AFHTTPRequestOperation *operation, id responseObject);
typedef void (^FailureBlock)(AFHTTPRequestOperation *operation, NSError *error);

@interface APIClient : AFHTTPRequestOperationManager

@property (strong, nonatomic) AFHTTPRequestOperation *operation;

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
