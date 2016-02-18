#import "PexesoBridge.h"
#import "POI.h"
#import "UNA.h"
#import "PoiVote.h"
#import "Snapshot.h"
#import "Urbo.h"
#include <typeinfo>
#include "IPexeso.hpp"

using namespace std;

@implementation PexesoBridge

IPexeso *px;
Urbo* urbo;
PexesoBridge *localBridge;

class BufferManager: public IBufferManager
{
public:
    void registerThread();
    void closeThread();
    char* open(ImgBuffer imgBuf);
    void close(ImgBuffer imgBuf, char*& pBuf);
    ImgBuffer newBuffer();
    void releaseBufferToCamera(ImgBuffer imgBuf);
    void deleteBuffer(ImgBuffer imgBuf);
    PlatformImage compress(ImgBuffer imgBuf);
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

BufferManager bufferManager;

RecoEvent* getRecoEvent(const IPexeso::Snapshot& snapshot) {

    RecoEvent* oRecoEvent = [[RecoEvent alloc] init];
    oRecoEvent.deviceID = [Urbo getInstance].deviceId;
    oRecoEvent.clientTimestamp = @(snapshot.clientTimestamp.count());
    oRecoEvent.loc = @[
        @(snapshot.sensorState.location.fLng),@(snapshot.sensorState.location.fLat)];
    oRecoEvent.locAccuracy = @(snapshot.sensorState.location.fRadius);
    oRecoEvent.pitch = @(snapshot.sensorState.fPitch);
    oRecoEvent.camAzimuth = @(snapshot.sensorState.fHeading);
    const char* machineSelectedPoi = snapshot.machineSelectedPoi.getId().c_str();
    if (machineSelectedPoi && machineSelectedPoi[0]) {
        oRecoEvent.machineSelectedPoi = [NSString stringWithCString:machineSelectedPoi
                                         encoding:[NSString defaultCStringEncoding]];
    }
    oRecoEvent.imgFileName = [NSString stringWithFormat:@"%@.%li.jpg", oRecoEvent.deviceID,
        [oRecoEvent.clientTimestamp longValue]];
    oRecoEvent.clientGeneratedUNA = [NSString stringWithCString:snapshot.sUna.c_str()
                                         encoding:[NSString defaultCStringEncoding]];
        
    return oRecoEvent;
}

Snapshot* getSnapshot(const IPexeso::Snapshot& snapshot) {

    Snapshot* oSnapshot = [[Snapshot alloc] init];
    oSnapshot.recoEvent = getRecoEvent(snapshot);
    oSnapshot.poi = (__bridge POI*)snapshot.machineSelectedPoi.pUser;
    oSnapshot.snapshotImage = (UIImage*)CFBridgingRelease(snapshot.platformImage);

    vector<IVote> votes = snapshot.vVotes;
    NSMutableArray *arrayVotes = [[NSMutableArray alloc] init];
    for (int i=0; i<votes.size();i++) {
        PoiVote *vote = [[PoiVote alloc] init];
        vote.poi = (__bridge POI*)votes[i].poi->pUser;
        vote.vote = votes[i].fScore;
        [arrayVotes addObject:vote];
    }
    oSnapshot.votes = arrayVotes;
    return oSnapshot;
}

- (void) initPexeso:(Urbo*) urboInstance
{
    IPexeso::Params params;
//    NSString* path = [[NSBundle mainBundle] pathForResource:@"settings" ofType:@"xml"];
//    std::string *xml = new std::string([path UTF8String]);
    params.inputType = InputType::BGR;
    params.bIdentify = true;

    urbo = urboInstance;
    localBridge = self;
    
    params.stateChangeListener = [&](const IPexeso::State& state) {
        if ([urbo.delegate respondsToSelector:@selector(onStateChanged: withSnapshot:)])
            [urbo.delegate onStateChanged:StateId(state.id) withSnapshot:nil];
    };

    params.recognitionListener = [&](const IPexeso::Snapshot& snapshot) {
        if ([urbo.delegate
             respondsToSelector:@selector(onStateChanged: withSnapshot:)]) {
            Snapshot* oSnapshot = getSnapshot(snapshot);
            [urbo.delegate onStateChanged:StateId(RECOGNITION) withSnapshot:oSnapshot];
        }
    };

    params.errorListener = [&](Severity severity, string sMsg) {
        NSString *msg = [NSString stringWithCString:sMsg.c_str()
                                           encoding:[NSString defaultCStringEncoding]];
            [urbo.delegate onError:SeverityCode(severity) message:msg];
    };
    
    params.poiCacheRequestListener = [&](int iRequestId, Location loc) {
        CLLocation *location = [[CLLocation alloc]
                                initWithLatitude:loc.fLat longitude:loc.fLng];
        [urbo sendCacheRequest:iRequestId withLocation:location];
    };
    
    params.snapshotListener = [&](const IPexeso::Snapshot& snapshot) {
        if ([urbo.delegate respondsToSelector:@selector(onSnapshot:)]) {
            Snapshot* oSnapshot = getSnapshot(snapshot);
            [urbo.delegate onSnapshot:oSnapshot];
        }
    };

    if (px == nullptr) {
        px = IPexeso::createInstance(params, "");
    }
}

-(void) initLiveFeed:(int) width  height:(int) height
{
    bufferManager.w = width;
    bufferManager.h = height;
    bufferManager.rotation = 0;
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
    px->pushFrame(CFRetain(byteBuff));
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
    if (!response) {
        return;
    }
    NSDictionary *oPois = response[@"pois"];
    if (!oPois) {
        return;
    }
    NSDictionary *oSyncList = oPois[@"syncList"];
    if (!oSyncList) {
        return;
    }

    if ([oSyncList allKeys].count != 0) {
        NSString *sClientId = [oSyncList allKeys][0];
        NSString *sServerId = oSyncList[sClientId];
        IPoi::ClientId clientId = [sClientId intValue];
        const char* szServerId = [sServerId UTF8String];
		const IPoi* pPoi = px->updatePoiId(clientId, szServerId);
        if (pPoi && pPoi->pUser) {
            POI* poi = (__bridge POI*)pPoi->pUser;
            poi.poiId = sServerId;
        }
        else {
            [urbo.delegate onError:SeverityCode(ERROR)
                message:@"poiCacheUpdateCallback could not find cached POI"];
        }
    }
}


-(BOOL) takeSnapshot
{
   return px->takeSnapshot();
}

-(void) tagSnapshot:(Snapshot*)oSnapshot poi:(POI*)oPoi
{
    if (!oSnapshot) {
        [urbo.delegate onError:SeverityCode(ERROR)
            message:@"tagSnapshot: snapshot is NIL"];
        return;
    }
    if (!oPoi || oPoi.name.length == 0) {
        [urbo.delegate onError:SeverityCode(ERROR)
            message:@"tagSnapshot: poi is NIL"];
        return;
    }
    IPoi poi = createPoi(oPoi);
    IPexeso::Snapshot snapshot = createSnapshot(oSnapshot);
    TagResult tagResult = px->tagSnapshot(snapshot, poi);
    oSnapshot.recoEvent.isIndex = @(tagResult.bIsIndex);
    oSnapshot.recoEvent.userFeedback = @(tagResult.bUserFeedback);
    oSnapshot.recoEvent.userSelectedPoi = [NSString
        stringWithCString:tagResult.pPoi->getId().c_str()
        encoding:[NSString defaultCStringEncoding]];
    oPoi.clientId = [NSString stringWithFormat:@"%ld",(long)tagResult.pPoi->clientId];
    if (!oPoi.imgFileName) {
        oPoi.imgFileName = oSnapshot.recoEvent.imgFileName;
    }
    oPoi.loc = locationCtoOarray(tagResult.pPoi->loc);
    oSnapshot.poi = (__bridge POI*)tagResult.pPoi->pUser;
    
    [urbo sendRecoEvent:oSnapshot];
}

IPexeso::Snapshot createSnapshot(Snapshot *oSnapshot)
{
    IPexeso::Snapshot snapshot;

    NSString *sUna = oSnapshot.recoEvent.clientGeneratedUNA;
    if (sUna) {
        const char* pcUna = [sUna UTF8String];
        snapshot.sUna = pcUna;
    }
    snapshot.sensorState.location = locationOarraytoC(oSnapshot.recoEvent.loc);
    snapshot.sensorState.fHeading = [oSnapshot.recoEvent.camAzimuth floatValue];
    if (oSnapshot.poi) {
        snapshot.machineSelectedPoi = createPoi(oSnapshot.poi);
    }
    return snapshot;
}

IPoi createPoi(POI *oPoi)
{
    Location loc = locationOarraytoC(oPoi.loc);

    const char* pcPoiName = [oPoi.name UTF8String];
    const char* pcPoiId = oPoi.poiId ? [oPoi.poiId UTF8String] : "";
    IPoi::ClientId clientId = oPoi.clientId ? [oPoi.clientId intValue] : IPoi::INVALID_ID;

    IPoi poi(pcPoiName, loc, pcPoiId, clientId);

    oPoi.clientId = [NSString stringWithFormat:@"%u", poi.clientId];
    poi.pUser = (__bridge_retained void *)oPoi; // PoiCache is now the owner of the POI object
    oPoi = nil;

    return poi;
}

-(void)confirmRecognition:(Snapshot*)pSnapshot
{
    px->confirmRecognition(createSnapshot(pSnapshot));
}

-(void)rejectRecognition:(Snapshot*)pSnapshot
{
    px->rejectRecognition(createSnapshot(pSnapshot));
}

-(NSArray *) getPoiShortlist
{
    PoiShortlist shortlist = px->getPoiShortlist(true);
    NSMutableArray *oPois = [[NSMutableArray alloc] init];
    for (int i = 0; i < shortlist.size(); i++) {
        IVote vote = shortlist[i];
        POI *poi = (__bridge POI *)vote.poi->pUser;
        [oPois addObject:poi];
    }
    return oPois;
}

-(CLLocation *) getCurrentLocation
{
    return locationCtoO(px->getCurrentLocation());
}

#pragma mark BufferManager implementation

char* BufferManager::open(ImgBuffer imgBuf)
{
    CMSampleBufferRef buffer = (CMSampleBufferRef)imgBuf;
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(buffer);
    CVPixelBufferLockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly);
    void* src_buff = CVPixelBufferGetBaseAddress(imageBuffer);
        
    return (char*)src_buff;
}

void BufferManager::close(ImgBuffer imgBuf, char *&pBuf)
{
    CMSampleBufferRef buffer = (CMSampleBufferRef)imgBuf;
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(buffer);
    CVPixelBufferUnlockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly);
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
    if (imgBuf != this) {
        CFRelease(imgBuf);
        imgBuf = nil;
    }
}

void BufferManager::deleteBuffer(ImgBuffer imgBuf)
{
}

// Note that on iOS, compress creates a UIImage* from ImgBuffer;
// actual JPEG is generated in Urbo.uploadImage
PlatformImage BufferManager::compress(ImgBuffer imgBuf)
{
    CMSampleBufferRef buffer = (CMSampleBufferRef)imgBuf;
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(buffer);
    CVPixelBufferLockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly);
    size_t bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer);
    size_t width = CVPixelBufferGetWidth(imageBuffer);
    size_t height = CVPixelBufferGetHeight(imageBuffer);
    void* src_buff = CVPixelBufferGetBaseAddress(imageBuffer);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(src_buff, width, height, 8,
        bytesPerRow, colorSpace, kCGBitmapByteOrder32Little | kCGImageAlphaNoneSkipFirst);
    CGImageRef quartzImage = CGBitmapContextCreateImage(context);
    CVPixelBufferUnlockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly);
    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    UIImage *image = [UIImage imageWithCGImage:quartzImage];
    CGImageRelease(quartzImage);
    return CFBridgingRetain(image);
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

NSArray* locationCtoOarray(Location location)
{
    return @[@(location.fLng),@(location.fLat)];
}

Location locationOarraytoC(NSArray *locArray)
{
    if (!locArray) {
        return Location();
    }
    else {
        return Location ([locArray[1] floatValue], [locArray[0] floatValue]);
    }
}

CLLocation* locationCtoO(Location location)
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
            NSArray *usig = poi.usig;
            IPoi iPoi = createPoi(poi);
            ListUnaIterator unaIterator(usig);
            fn(iPoi, unaIterator);
        }
    }
}


@end
