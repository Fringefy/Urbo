#import "PexesoBridge.h"
#import "POI.h"
#include <typeinfo>

using namespace std;

@implementation PexesoBridge

IPexeso *px;
id<PexesoDelegate> delegate;

typedef enum StateId StateId;

class BufferManager: public IBufferManager
{
	ImgBuffer oldBuffer;
	void registerThread();
	char* open(ImgBuffer imgBuf);
	void close(ImgBuffer imgBuf, char*& pBuf);
	ImgBuffer newBuffer();
	void releaseBufferToCamera(ImgBuffer imgBuf);
	void deleteBuffer(ImgBuffer imgBuf);
};

BufferManager *bufferManager;

- (void) initPexeso:(id<PexesoDelegate>) newDelegate
{
	IPexeso::Params params;
	bufferManager = new BufferManager;
//	NSString* path = [[NSBundle mainBundle] pathForResource:@"settings" ofType:@"xml"];
//	std::string *xml = new std::string([path UTF8String]);
	params.inputType = InputType::BGR;
	params.bRotate = false;
	params.pBufferManager = bufferManager;
	
	delegate = newDelegate;
	
	params.stateChangeListener = [&](IPexeso::IState& state) {
		POI *poi;
		if (state.id == 1)
			poi = (__bridge POI *)state.pPoi->pUser;
		if ([delegate respondsToSelector:@selector(pexesoDidChangeState:withPoi:)])
			[delegate pexesoDidChangeState:State(state.id) withPoi:poi];
		return YES;
	};
	
	params.errorListener = [&](Severity severity, string sMsg) {
		if ([delegate respondsToSelector:@selector(pexesoDidGetError:)])
			[delegate pexesoDidGetError:SeverityCode(severity)];
	};
	
	params.poiCacheRequestListener = [&](int iRequestId, Location loc) {	
		CLLocation *location = [[CLLocation alloc] initWithLatitude:loc.fLat
														  longitude:loc.fLng];
		if ([delegate respondsToSelector:@selector(pexesoDidRequestListener:
												   withLocation:)]) {
			[delegate pexesoDidRequestListener:iRequestId withLocation:location];
		}
	};

	if (px == nullptr) {
		px = IPexeso::createInstance(params, "");
	}
}

-(void) initLiveFeed:(int) width  height:(int) height
{
	bufferManager->w = width;
	bufferManager->h = height;
	px->initLiveFeed();	
}

-(void) stopLiveFeed
{
	px->stopLiveFeed();
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
	Location loc = *new Location(location.coordinate.latitude,
								 location.coordinate.longitude,
								 location.horizontalAccuracy);
	px->pushLocation(loc);
}

-(void) forceCacheRefresh
{
	px->forceCacheRefresh();
}

-(void) poiCacheRequestCallback:(int) requestId
					   location:(CLLocation *) location
						   pois:(NSArray*) poisList
{
	Location loc = *new Location(location.coordinate.latitude,
								 location.coordinate.longitude,
								 location.horizontalAccuracy);
	ListPoiIterator poiIterator(poisList);
	px->poiCacheRequestCallback(requestId, loc, poiIterator);
}

-(void) takeSnapshot
{
	px->takeSnapshot();
}

-(id) getSnapshot:(long) snapshotId
{
	id joRe = nullptr;
	px->getSnapshot(snapshotId);
	return joRe;
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

class ListPoiIterator : public IPoiIterator {
	class ListUnaIterator : public IUnaIterator {
		NSArray *unasArray;
		
	public:
		ListUnaIterator( NSArray *unas) :
		IUnaIterator((int)unas.count),
		unasArray(unas)
		{}
		
		void operator()(const function<void(IUna una)>& fn) {
			for (int i = 0; i < nUnas; i++) {
				UNA *joUna = unasArray[i];
				if (joUna) {
					NSString *jsUna = joUna.data;
					if (jsUna) {
						const char *pcUna = [jsUna UTF8String];
						fn({pcUna, strlen(pcUna)});
					}
				}
			}
		}
	};
	
	NSArray *poisArray;

public:
	ListPoiIterator(NSArray *pois) :
	IPoiIterator((int)pois.count),
	poisArray(pois)
	{}
	
	void release(IPoi& iPoi) {
	}
	
	void operator()(const function<void(IPoi iPoi, IUnaIterator& usigIterator)>& fn) {
		for (int i = 0; i < nPois; i++) {
			POI *poi = poisArray[i];
			if (poi) {
				NSArray *usig = poi.usig;
				Location location(poi.location.coordinate.latitude,
								  poi.location.coordinate.longitude,
								  poi.location.horizontalAccuracy);
				ListUnaIterator unaIterator(usig);
				IPoi iPoi = *new IPoi;
				iPoi.pUser = (__bridge void *)poi;
				iPoi.loc = location;
				fn(iPoi, unaIterator);
			}
		}
	}
};

@end
