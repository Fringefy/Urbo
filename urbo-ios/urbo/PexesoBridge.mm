#import "PexesoBridge.h"
#import "POI.h"
#import "PoiVote.h"
#import "Snapshot.h"
#include <typeinfo>

using namespace std;

@implementation PexesoBridge

IPexeso *px;
id<PexesoDelegate> delegate;
PexesoBridge *localBridge;

class BufferManager: public IBufferManager
{
public:
    BufferManager(int w, int h) {
        this->w = w;
        this->h = h;
        this->rotation = 0;
    }
    void registerThread();
    void closeThread();
    char* open(ImgBuffer imgBuf);
    void close(ImgBuffer imgBuf, char*& pBuf);
    ImgBuffer newBuffer();
    void releaseBufferToCamera(ImgBuffer imgBuf);
    void deleteBuffer(ImgBuffer imgBuf);
};

class ListUnaIterator : public IUnaIterator
{
    NSArray *unasArray;
    
public:
    ListUnaIterator(NSArray *unas) :
    IUnaIterator((int)unas.count),
    bCalled(false),
    unasArray(unas)
    {}
    
    void iterate(const Function&);
    bool bCalled;
};

class ListPoiIterator : public IPoiIterator
{
    NSArray *poisArray;
    
public:
    ListPoiIterator(NSArray *pois) :
    IPoiIterator((int)pois.count),
    poisArray(pois)
    {}
    void iterate(const Function&);
};

BufferManager *bufferManager;

- (void) initPexeso:(id<PexesoDelegate>) newDelegate
{
    IPexeso::Params params;
//    NSString* path = [[NSBundle mainBundle] pathForResource:@"settings" ofType:@"xml"];
//    std::string *xml = new std::string([path UTF8String]);
    params.inputType = InputType::BGR;
    params.bIdentify = true;
    
    delegate = newDelegate;
    localBridge = self;
    
    params.stateChangeListener = [&](const IPexeso::IState& state) {
        POI *poi;
        int snapshotId = 0;
        if (state.id == RECOGNITION) {
            poi = (__bridge POI *)state.pPoi->pUser;
            snapshotId = (int)state.snapshotId;
        }
        if ([delegate
             respondsToSelector:@selector(pexesoDidChangeState:withPoi:andSnapshotId:)])
            [delegate
             pexesoDidChangeState:StateId(state.id) withPoi:poi andSnapshotId:snapshotId];
        return YES;
    };
    
    params.errorListener = [&](Severity severity, string sMsg) {
        NSString *msg = [NSString stringWithCString:sMsg.c_str()
                                           encoding:[NSString defaultCStringEncoding]];
        if ([delegate respondsToSelector:@selector(pexesoDidGetError:message:)])
            [delegate pexesoDidGetError:SeverityCode(severity) message:msg];
    };
    
    params.poiCacheRequestListener = [&](int iRequestId, Location loc) {
        CLLocation *location = [[CLLocation alloc] initWithLatitude:loc.fLat
                                                          longitude:loc.fLng];
        if ([delegate respondsToSelector:@selector(pexesoDidRequestListener:
                                                   withLocation:)]) {
            [delegate pexesoDidRequestListener:iRequestId withLocation:location];
        }
    };
    
    params.snapshotListener = [&](const IPexeso::ISnapshot& snapshot,
                                  ImgBuffer& imgBuf, bool bTagImmediately) {
        [localBridge onSnapshot:snapshot imageBuffer:imgBuf tagImmediately:bTagImmediately];
    };

    if (px == nullptr) {
        px = IPexeso::createInstance(params, "");
    }
}

-(void) initLiveFeed:(int) width  height:(int) height
{
    bufferManager = new BufferManager(width, height);
    px->initLiveFeed(bufferManager);
}

-(void) stopLiveFeed
{
    px->stopLiveFeed();
}

-(void) restartLiveFeed
{
    px->initLiveFeed(bufferManager);
}

- (void) pushFrame:(CMSampleBufferRef) byteBuff
{
    CFRetain(byteBuff);
    ImgBuffer imgBufIn = byteBuff;
    px->pushFrame(imgBufIn);
}

-(void) pushHeading:(float) heading
{
    px->pushHeading(heading);
}

-(void) pushPitch:(float) pitch
{
    px->pushPitch(pitch);
}

-(void) pushLocation:(CLLocation *) location
{
    px->pushLocation(locationOtoC(location));
}

-(void) forceCacheRefresh
{
    px->forceCacheRefresh();
}

-(void) poiCacheRequestCallback:(int) requestId
                       location:(CLLocation *) location
                           pois:(NSArray*) poisList
{
    ListPoiIterator poiIterator(poisList);
    px->poiCacheRequestCallback(requestId, locationOtoC(location), poiIterator);
}

-(void) poiCacheUpdateCallback:(NSDictionary *)response
{
    if (!response){
        return;
    }
    NSDictionary *oPois = response[@"pois"];
    if (!oPois)
    {
        return;
    }
    NSDictionary *oSyncList = oPois[@"syncList"];
    if (!oSyncList)
    {
        return;
    }

    if ([oSyncList allKeys].count != 0) {
        NSString *sClientId = [oSyncList allKeys][0];
        NSString *sServerId = oSyncList[sClientId];
        IPoi::ClientId clientId = [sClientId intValue];
        const char* zServerId = [sServerId UTF8String];
        px->updatePoiId(clientId, zServerId);
    }
}


-(BOOL) takeSnapshot
{
   return px->takeSnapshot();
}

-(BOOL) tagSnapshot:(Snapshot *)oSnap poi:(POI *)oPoi
{
    if (!oSnap) {
        return false;
    }
    if (!oPoi) {
        return false;
    }
    IPoi poi = createPoi(oPoi);
    IPexeso::ISnapshot snapshot = createSnapshot(oSnap);
    TagResult tagResult = px->tagSnapshot(snapshot, poi);
    oSnap.recoEvent.isIndex = @(tagResult.bIsIndex);
    oSnap.recoEvent.userFeedback = @(tagResult.bUserFeedback);
    oSnap.recoEvent.userSelectedPoi = [NSString
                                       stringWithCString:tagResult.pPoi->getId().c_str()
                                       encoding:[NSString defaultCStringEncoding]];
    oPoi.clientId = [NSString stringWithFormat:@"%ld",(long)tagResult.pPoi->clientId];
    if (!oPoi.imgFileName) {
        oPoi.imgFileName = oSnap.recoEvent.imgFileName;
    }
    oPoi.loc = [self locationCtoOarray:tagResult.pPoi->loc];
    oSnap.poi = oPoi;
    
    if([delegate respondsToSelector:@selector(pexesoOnRecognition:)])
        [delegate pexesoOnRecognition:oSnap];
    
    return true;
}

IPexeso::ISnapshot createSnapshot(Snapshot *inSnap)
{
    IPexeso::ISnapshot snapshot;
    if (!inSnap) {
        return snapshot;
    }
    
    NSString *sUna = inSnap.recoEvent.clientGeneratedUNA;
    if (sUna) {
        const char* pcUna = [sUna UTF8String];
        snapshot.sUna = pcUna;
    }
    snapshot.sensorState.location = locationOarraytoC(inSnap.recoEvent.loc);
    snapshot.sensorState.fHeading = [inSnap.recoEvent.camAzimuth floatValue];
    if (inSnap.poi) {
        snapshot.machineSelectedPoi = createPoi(inSnap.poi);
    }
    return snapshot;
}

IPoi createPoi(POI *oPoi)
{
    if (!oPoi || !oPoi.name) {
        return IPoi();
    }
    Location loc = locationOarraytoC(oPoi.loc);

    const char* pcPoiName = [oPoi.name UTF8String];
    const char* pcPoiId = oPoi.poiId ? [oPoi.poiId UTF8String] : "";

    IPoi poi(pcPoiName, loc, pcPoiId, oPoi.clientId ? [oPoi.clientId intValue] : IPoi::INVALID_ID);
    poi.pUser = (__bridge void *)oPoi;

    oPoi.clientId = [NSString stringWithFormat:@"%u", poi.clientId];

    return poi;
}

-(BOOL) getSnapshot:(long) snapshotId
{
    return px->getSnapshot(snapshotId);
}

-(BOOL)confirmRecognition:(long)snapshotId
{
    return px->confirmRecognition(snapshotId);
}

-(BOOL)rejectRecognition:(long)snapshotId
{
    return px->rejectRecognition(snapshotId);
}

-(NSArray *) getPoiShortlist
{
    PoiShortlist shortlist = px->getPoiShortlist(true);
    NSMutableArray *oPois = [[NSMutableArray alloc] init];
    for (int i = 0; i < shortlist.size(); i++) {
        [oPois addObject:(__bridge POI *)shortlist[i].poi->pUser];
    }
    return oPois;
}

-(CLLocation *) getCurrentLocation
{
    return [self locationCtoO:px->getCurrentLocation()];
}

-(void) onSnapshot:(const IPexeso::ISnapshot&) snapshot imageBuffer:(ImgBuffer&) imgBuf
    tagImmediately:(bool)bTagImmediately
{
    CLLocation *oLocation = [self locationCtoO:snapshot.sensorState.location];
    NSString *sUna = [NSString stringWithCString:snapshot.sUna.c_str()
                                       encoding:[NSString defaultCStringEncoding]];
    
    POI *oMachineSelectedPoi = (__bridge POI *)snapshot.machineSelectedPoi.pUser;
    Snapshot *oSnapshot = [[Snapshot alloc] init];
    oSnapshot.recoEvent = [[RecoEvent alloc]initWithLocation:oLocation
                                                       pitch:snapshot.sensorState.fPitch
                                                     azimuth:snapshot.sensorState.fHeading
                                                 selectedPoi:oMachineSelectedPoi.getId
                                                   clientUNA:sUna];
    
    CMSampleBufferRef buffer = (CMSampleBufferRef)imgBuf;
    UIImage *sampleImage =  [self imageFromSampleBuffer:buffer];
    oSnapshot.snapshotImage = sampleImage;
    if (bTagImmediately) {
        [self tagSnapshot:oSnapshot poi:oMachineSelectedPoi];
    }
    vector<IVote> votes = snapshot.vVotes;
    NSMutableArray *arrayVotes = [[NSMutableArray alloc] init];
    for (int i=0; i<votes.size();i++)
    {
        PoiVote *vote = [[PoiVote alloc] init];
        vote.poi = (__bridge POI *)votes[i].poi->pUser;
        vote.vote = votes[i].fScore;
        [arrayVotes addObject:vote];
    }
    oSnapshot.votes = arrayVotes;
    if ([delegate respondsToSelector:@selector(pexesoOnSnapshot:)])
        [delegate pexesoOnSnapshot:oSnapshot];
}

- (UIImage *) imageFromSampleBuffer:(CMSampleBufferRef) sampleBuffer
{
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
    CVPixelBufferLockBaseAddress(imageBuffer, 0);
    size_t bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer);
    size_t width = CVPixelBufferGetWidth(imageBuffer);
    size_t height = CVPixelBufferGetHeight(imageBuffer);
    u_int8_t *baseAddress = (u_int8_t *)malloc(bytesPerRow*height);
    memcpy( baseAddress, CVPixelBufferGetBaseAddress(imageBuffer), bytesPerRow * height);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(baseAddress,
                                                 width,
                                                 height,
                                                 8,
                                                 bytesPerRow,
                                                 colorSpace,
                                                 kCGBitmapByteOrder32Little |
                                                 kCGImageAlphaNoneSkipFirst);
    CGImageRef quartzImage = CGBitmapContextCreateImage(context);
    CVPixelBufferUnlockBaseAddress(imageBuffer,0);
    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    UIImage *image = [UIImage imageWithCGImage:quartzImage];
    free(baseAddress);
    CGImageRelease(quartzImage);
    
    
    return (image);
}

#pragma mark BufferManager implementation

char* BufferManager::open(ImgBuffer imgBuf)
{
    CMSampleBufferRef buffer = (CMSampleBufferRef)imgBuf;
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(buffer);
    CVPixelBufferLockBaseAddress(imageBuffer,0);
    char* src_buff = (char *)CVPixelBufferGetBaseAddress(imageBuffer);
        
    return src_buff;
}

void BufferManager::close(ImgBuffer imgBuf, char *&pBuf)
{
    CMSampleBufferRef buffer = (CMSampleBufferRef)imgBuf;
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(buffer);
    CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
    pBuf = nil;
}

void BufferManager::registerThread()
{
}

void BufferManager::closeThread()
{
}

ImgBuffer BufferManager::newBuffer()
{
    ImgBuffer imgBuf = this; // buffer used as dummy to avoid crashes with nill buffer
    return imgBuf;
}

void BufferManager::releaseBufferToCamera(ImgBuffer imgBuf)
{
        if (imgBuf != this)
        {
            CFRelease((CMSampleBufferRef)imgBuf);
            imgBuf = nil;
        }
}

void BufferManager::deleteBuffer(ImgBuffer imgBuf)
{
    
}


#pragma mark private methods


Location locationOtoC(CLLocation * oLoc)
{
    if (!oLoc) {
        return Location();
    }
    else {
        return Location (oLoc.coordinate.latitude,
                         oLoc.coordinate.longitude,
                         oLoc.horizontalAccuracy);
    }
}

-(NSArray *) locationCtoOarray:(Location) location
{
    return @[@(location.fLng),@(location.fLat)];
}

Location locationOarraytoC(NSArray *locArray)
{
    if (!locArray) {
        return Location();
    }
    else {
        return Location ([locArray[1] floatValue],
                         [locArray[0] floatValue]);
    }
}

-(CLLocation *) locationCtoO:(Location) location
{
    
    CLLocation *loc = [[CLLocation alloc]
                       initWithCoordinate:CLLocationCoordinate2DMake(location.fLat,
                                                                     location.fLng)
                       altitude:0
                       horizontalAccuracy:location.fRadius
                       verticalAccuracy:0
                       timestamp:[NSDate date]];
    return loc;
}

void ListUnaIterator::iterate(const ListUnaIterator::Function& fn)
{
    bCalled = true;
    for (int i = 0; i < size(); i++) {
        UNA *oUna = unasArray[i];
        if (oUna) {
            NSString *sUna = oUna.data;
            if (sUna) {
                const char *pcUna = [sUna UTF8String];
                fn(pcUna);
            }
        }
    }
}

void ListPoiIterator::iterate(const ListPoiIterator::Function& fn)
{
    for (size_t i = 0; i < size(); i++) {
        POI *poi = poisArray[i];
        if (poi) {
            IPoi iPoi = createPoi(poi);
            NSArray *usig = poi.usig;
            ListUnaIterator unaIterator(usig);
            fn(iPoi, unaIterator);
        }
    }
}


@end
