#import "Urbo.h"
#import "POI.h"
#import "APIClient.h"

#define degrees(x) (180 * x / M_PI)

@implementation Urbo
+ (Urbo *)sharedInstance{
	static Urbo *sharedInstance = nil;
	@synchronized(self) {
		if (!sharedInstance) {
			sharedInstance = [[self alloc] init];
			sharedInstance.pexeso = [[PexesoBridge alloc] init];
			sharedInstance.poisArray = [[NSMutableArray alloc] init];
			if ([[NSUserDefaults standardUserDefaults]
				 objectForKey:@"uniqueIdentifier"] == nil) {
				NSString *deviceId = [[[UIDevice currentDevice] identifierForVendor]
									  UUIDString];
				[[NSUserDefaults standardUserDefaults] setObject: deviceId
														  forKey:@"uniqueIdentifier"];
			}
			sharedInstance.deviceId = [[NSUserDefaults standardUserDefaults]
									   objectForKey:@"uniqueIdentifier"];
			sharedInstance.locationManager = [sharedInstance setupLocationManager];
			sharedInstance.locationManager.delegate = sharedInstance;
			[sharedInstance initMotionManager];
			[sharedInstance initPexeso];
		}
	}
	return sharedInstance;
}

- (void) initPexeso
{
	[self.pexeso initPexeso:self];
}

- (CLLocationManager *) setupLocationManager
{
	CLLocationManager *locationManager = [[CLLocationManager alloc] init];
	if ([locationManager respondsToSelector:@selector(requestWhenInUseAuthorization)]) {
		[locationManager requestWhenInUseAuthorization];
	}
	locationManager.desiredAccuracy = kCLLocationAccuracyBest;
	locationManager.headingOrientation = CLDeviceOrientationPortrait;
	locationManager.headingFilter = 1;
	[locationManager startUpdatingHeading];
	[locationManager startUpdatingLocation];
	return locationManager;
	
}

-(void) initMotionManager
{
	self.manager = [[CMMotionManager alloc] init];
	if (self.manager.deviceMotionAvailable) {
		self.manager.deviceMotionUpdateInterval = 0.01;
		[self.manager startDeviceMotionUpdatesToQueue:[NSOperationQueue mainQueue]
										  withHandler:^(CMDeviceMotion * _Nullable motion,
														NSError * _Nullable error) {
			float degrees = degrees(motion.attitude.pitch);
			[self.pexeso pushPitch:(degrees - 90)];
		}];
		
	}
	
}

-(void)locationManager:(CLLocationManager *)manager
	  didUpdateHeading:(CLHeading *)newHeading
{
	[self.pexeso pushHeading:newHeading.trueHeading];
}


-(void)locationManager:(CLLocationManager *)manager
	didUpdateLocations:(NSArray *)locations
{
	CLLocation *location = locations.lastObject;
	[self.pexeso pushLocation:location];
}

#pragma mark pexeso delegates
-(void)pexesoDidChangeState:(State)state withPoi:(POI *)poi
{
	if ([self.delegate respondsToSelector:@selector(urbo:didChangeState:withPoi:)])
		[self.delegate urbo:self didChangeState:state withPoi:poi];
}

-(void)pexesoDidGetError:(SeverityCode)errorCode
{
	switch(errorCode) {
		case CODE_DBG:
			NSLog(@"DBG \n");
			break;
		case CODE_INFORMATION:
			NSLog(@"INFORMATION \n");
			break;
		case CODE_WARNING:
			NSLog(@"WARNING \n");
			break;
		case CODE_ERROR:
			NSLog(@"ERROR \n");
			break;
		case CODE_MAX_SEVERITY:
			NSLog(@"MAX_SEVERITY \n");
			break;
		default:
			NSLog(@"Error");
	}
}

-(void)pexesoDidRequestListener:(int)requestId withLocation:(CLLocation *)location
{
	[[APIClient sharedClient] getPOIs:@(location.coordinate.longitude)
								  lat:@(location.coordinate.latitude)
							 deviceId:self.deviceId
							 accuracy:@(location.horizontalAccuracy)
							  success:^(AFHTTPRequestOperation *operation,
										id responseObject) {
								  [[Urbo sharedInstance].poisArray removeAllObjects];
								  for (NSDictionary *dict in responseObject[@"pois"]) {
									  POI *poi = [POI poiWithJSON:dict];
									  [[Urbo sharedInstance].poisArray addObject:poi];
								  }
								  [self.pexeso poiCacheRequestCallback:requestId
															  location:location
																  pois:self.poisArray];
							  }
							  failure:^(AFHTTPRequestOperation *operation,
										NSError *error) {
							  }];
}

@end
