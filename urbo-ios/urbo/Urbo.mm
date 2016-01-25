#import "Urbo.h"
#import "POI.h"
#import "APIClient.h"
#import "Snapshot.h"
#import <AWSCore/AWSCore.h>
#import <AWSS3/AWSS3.h>

#define degrees(x) (180 * x / M_PI)
#define awskey @"eu-west-1:1b7bb31e-db37-4d1a-99cf-615f36f531ea"

@implementation Urbo

static Urbo *sharedInstance = nil;

+ (Urbo *)getInstance{
    @synchronized(self){
    }
    return sharedInstance;
}

+ (void) startWithApiKey:(NSString *) apiKey
{
    sharedInstance = [[self alloc] init];
    sharedInstance.pexeso = [[PexesoBridge alloc] init];
    sharedInstance.poisArray = [[NSMutableArray alloc] init];
    sharedInstance.apiKey = apiKey;
    sharedInstance.baseUrl = @"https://odie.fringefy.com/";
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
    AWSCognitoCredentialsProvider *credentials = [[AWSCognitoCredentialsProvider alloc]
                                                  initWithRegionType:AWSRegionEUWest1
                                                  identityPoolId:awskey];
    AWSServiceConfiguration *configuration = [[AWSServiceConfiguration alloc]
                                              initWithRegion:AWSRegionEUWest1
                                              credentialsProvider:credentials];
    AWSServiceManager.defaultServiceManager.defaultServiceConfiguration = configuration;
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
    if (newHeading.headingAccuracy < 0)
        return;
    CLLocationDirection  theHeading = ((newHeading.trueHeading > 0) ?
            newHeading.trueHeading : newHeading.magneticHeading);
    [self.pexeso pushHeading:theHeading];
}


-(void)locationManager:(CLLocationManager *)manager
    didUpdateLocations:(NSArray *)locations
{
    [self.pexeso pushLocation:locations.lastObject];
}

-(void) tagSnapshot:(Snapshot*)snapshot poi:(POI *)poi {
    [self.pexeso tagSnapshot:snapshot poi:poi];
}

-(NSArray *) getPoiShortlist {
    return [self.pexeso getPoiShortlist];
}

-(BOOL) confirmRecognition:(long) snapshotId {
  return  [self.pexeso confirmRecognition:snapshotId];
}

-(BOOL) rejectRecognition:(long) snapshotId {
   return [self.pexeso rejectRecognition:snapshotId];
}

-(BOOL) getSnapshot:(long) snapshotId {
    return [self.pexeso getSnapshot:snapshotId];
}

-(BOOL) takeSnapshot {
    return [self.pexeso takeSnapshot];
}

- (void) forceCacheRefresh {
    [self.pexeso forceCacheRefresh];
}

- (void) restartLiveFeed {
    [self.pexeso restartLiveFeed];
}

#pragma mark pexeso delegates


-(void)pexesoOnRecognition:(Snapshot *)snapshot
{
    [self uploadImage:snapshot];
    NSArray *poiArray;
    if (snapshot.poi.isClientOnly) {
        poiArray = [NSArray arrayWithObject:snapshot.poi.dictionaryRepresentation];
    }
    else {
        poiArray = [[NSArray alloc] init];
    }
    NSDictionary *resultDict = [NSDictionary
        dictionaryWithObjectsAndKeys: poiArray,
        @"pois",
        [NSArray arrayWithObject: snapshot.recoEvent.dictionaryRepresentation],
        @"recognitionEvents",
        nil];
    [[APIClient sharedClient] sendPoisWithEvents:resultDict
        success:^(NSURLSessionTask *task, id responseObject) {
            [self.pexeso poiCacheUpdateCallback:responseObject];
            if ([self.delegate respondsToSelector:
                @selector(urboApiMessage:response:)]) {
                    [self.delegate urboApiMessage:1 response:responseObject];
                }
        }
        failure:^(NSURLSessionTask *task, NSError *error) {
            if ([self.delegate respondsToSelector:
                @selector(urboApiMessage:response:)]) {
                    [self.delegate urboApiMessage:0 response:error.userInfo];
                }
        }
    ];
}

-(void) uploadImage:(Snapshot *) snapshot
{
    NSString *docDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
                                                           NSUserDomainMask, YES)[0];
    NSString *filePath = [docDir stringByAppendingPathComponent:
                          snapshot.recoEvent.imgFileName];
    NSData *imageData = [NSData dataWithData:UIImageJPEGRepresentation
                         (snapshot.snapshotImage, 1.0)];
    [imageData writeToFile:filePath atomically:YES];
    AWSS3TransferManager *transferManager = [AWSS3TransferManager
                                             defaultS3TransferManager];
    AWSS3TransferManagerUploadRequest *request = [AWSS3TransferManagerUploadRequest new];
    request.body = [NSURL fileURLWithPath:filePath];
    request.key = snapshot.recoEvent.imgFileName;
    request.bucket = [NSString stringWithFormat:@"%@/%@", self.s3Bucket,self.s3Folder];
    request.contentType = @"image/jpg";
    [[transferManager upload:request] continueWithBlock:^id(AWSTask *task) {
        [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
        return nil;
    }];
}

-(void)pexesoDidChangeState:(StateId)state withPoi:(POI *)poi
              andSnapshotId:(int) snapshotId;
{
    if ([self.delegate
         respondsToSelector:@selector(urboDidChangeState:withPoi:andSnapshotId:)])
        [self.delegate urboDidChangeState:state withPoi:poi andSnapshotId:snapshotId];
}

-(void)pexesoOnSnapshot:(Snapshot *)snapshot
{
    if ([self.delegate respondsToSelector:@selector(urboOnSnapshot:)])
        [self.delegate urboOnSnapshot:snapshot];
}

-(void)pexesoDidGetError:(SeverityCode)errorCode message:(NSString *)message
{
    switch(errorCode) {
        case CODE_DBG:
//            NSLog(@"DBG \n");
            break;
        case CODE_INFORMATION:
//            NSLog(@"INFORMATION \n");
            break;
        case CODE_WARNING:
//            NSLog(@"WARNING \n");
            break;
        case CODE_ERROR:
//            NSLog(@"ERROR \n");
            break;
        case CODE_MAX_SEVERITY:
//            NSLog(@"MAX_SEVERITY \n");
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
                              success:^(NSURLSessionTask *task,id responseObject){
                                  self.s3Bucket = responseObject[@"s3Bucket"];
                                  self.s3Folder = responseObject[@"s3Folder"];
                                  self.sharepageUrl = responseObject[@"sharepageUrl"];
                                  self.tabpageUrl = responseObject[@"tabpageUrl"];
                                  self.mappageUrl = responseObject[@"mappageUrl"];
                                  self.downsizedUrl = responseObject[@"downsizedUrl"];
                                  [[Urbo getInstance].poisArray removeAllObjects];
                                  for (NSDictionary *dict in responseObject[@"pois"]) {
                                      POI *poi = [POI poiWithJSON:dict];
                                      [[Urbo getInstance].poisArray addObject:poi];
                                  }
                                  [self.pexeso poiCacheRequestCallback:requestId
                                                              location:location
                                                                  pois:self.poisArray];
                              }
                              failure:^(NSURLSessionTask *task,NSError *error) {
                              }];
}

@end
