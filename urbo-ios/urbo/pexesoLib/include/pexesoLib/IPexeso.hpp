// file:	IPexeso.hpp
//
// summary:	Declares the IPexeso interface and public data types


// this macro is defined when the header is used internally, in which case we do not want
// to include system headers that are a part of the PCH. 
#ifndef PEXESO_IMPL
#include <string>
#include "opencv2/opencv.hpp"
#define INTERFACE_ONLY
#include "pexesial.hpp"
#endif

#pragma once
#include "cortex/ICortex.hpp"

using namespace std::chrono;

enum StateId {
	COLD_START = -1,
	SEARCH = 0,
	RECOGNITION = 1,
	NO_RECOGNITION = 2,
	NON_INDEXABLE = 3,
	BAD_ORIENTATION = 4,
	MOVING = 5
};

struct TagResult {
	IPoi* pPoi;
	bool bIsIndex;
	bool bUserFeedback;
};

class IBufferManager;


/// <summary> The Pexeso public interface. </summary>
/// <remarks> This is used both as a public interface for Pexeso consumers who do not
/// 	wish to include the entire Pexeso header set. It is also used as a factory with
/// 	the createInstance method. </remarks>
class IPexeso
{
public:
	virtual ~IPexeso() {};

// Inner Types

	typedef steady_clock::time_point Timestamp;
	typedef size_t SnapshotId;
	const static SnapshotId INVALID_SNAPSHOT_ID = (const SnapshotId) -1;

	struct IState {
		Timestamp t;
		StateId id;
		const IPoi* pPoi;
		SnapshotId snapshotId;

		IState() = default;
		IState(StateId id, const IPoi* pPoi, SnapshotId snapshotId) :
				id(id),
				pPoi(pPoi),
				snapshotId(snapshotId)
		{
			t = steady_clock::now();
		}
	};

	struct ISnapshot {
		Timestamp timestamp;
		SensorState sensorState;
		string sUna;
		vector<IVote> vVotes;
		IPoi machineSelectedPoi;
	};

	typedef function<void(int iRequestId,
		const Location& location)> PoiCacheRequestListener;

	typedef function<bool(const IPexeso::IState& state)> StateChangeListener;
	typedef function<void(const IPexeso::ISnapshot&, ImgBuffer&, bool)> SnapshotListener;

	struct Params : ICortex::Params {
		ErrorListener errorListener;
		PoiCacheRequestListener poiCacheRequestListener;
		StateChangeListener stateChangeListener;
		SnapshotListener snapshotListener;
	};


// Factory

	/// <summary> Creates a pexeso instance. </summary>
	/// <param name="params"> Pexeso interface public parameters. </param>
	/// <param name="sParamsXmlFile"> The parameters XML file. </param>
	/// <returns> null if it fails, else the new instance. </returns>
	static IPexeso* createInstance(Params& params, std::string sParamsXmlFile);


// Public Interface 

	virtual bool poiCacheRequestCallback(int iRequestId, const Location& location,
		IPoiIterator& poiIterator) = 0;
	virtual void updatePoiId(IPoi::ClientId clientId, string serverId) = 0;

	virtual void initLiveFeed(IBufferManager* pBufManager) = 0;
	virtual void stopLiveFeed() = 0;

	virtual TagResult tagSnapshot(const ISnapshot& snapshot, const IPoi& poi) = 0;

	virtual bool takeSnapshot() = 0;
	virtual bool getSnapshot(SnapshotId snapshotId) = 0;
	virtual bool confirmRecognition(SnapshotId snapshotId) = 0;
	virtual bool rejectRecognition(SnapshotId snapshotId) = 0;

	virtual void forceCacheRefresh() = 0;

	virtual Location getCurrentLocation() = 0;
	virtual PoiShortlist getPoiShortlist(bool bSort = false) = 0;

// Sensor Inputs

	virtual void pushFrame(ImgBuffer imgBuf) = 0;
	virtual void pushHeading(float fHeading) = 0;
	virtual void pushPitch(float fPitch) = 0;
	virtual void pushLocation(Location location) = 0;
};


class IBufferManager {
public:
	int w, h;
	int rotation = 0;

	virtual void registerThread() = 0;
	virtual void closeThread() = 0;
	virtual char* open(ImgBuffer imgBuf) = 0;
	virtual void close(ImgBuffer imgBuf, char*& pBuf) = 0;
	virtual ImgBuffer newBuffer() = 0;
	virtual void releaseBufferToCamera(ImgBuffer imgBuf) = 0;
	virtual void deleteBuffer(ImgBuffer imgBuf) = 0;
};
