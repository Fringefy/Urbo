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
	SEARCH = 0,
	RECOGNITION = 1,
	NO_RECOGNITION = 2,
	NON_INDEXABLE = 3,
	BAD_ORIENTATION = 4,
	MOVING = 5
};

enum UserAction {
	TAG
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
	typedef size_t SnapshotId;
	const SnapshotId INVALID_SNAPSHOT_ID = -1;

	struct IState {
		steady_clock::time_point t;
		StateId id;
		IPoi* pPoi;
		SnapshotId snapshotId;

		IState() = default;
		IState(StateId id, SnapshotId snapshotId, IPoi* pPoi = nullptr) :
			id(id),
			pPoi(pPoi),
			snapshotId(snapshotId)
		{
			t = steady_clock::now();
		}
	};

	struct IVote {
		IVote(IPoi* poi, float fScore):
			poi(poi), fScore(fScore)
		{};

		IPoi* poi;
		float fScore;
	};

	struct ISnapshot {
		steady_clock::time_point t;
		SensorState sensorState;
		ImgBuffer imgBuf;

		virtual string getUna() = 0;
		virtual IPoi* getMachineSelectedPoi() = 0;
		virtual vector<IVote> getVotes() = 0;

		virtual void unlock() = 0;
	};
	
	typedef std::function<void(int iRequestId, 
		const Location& location)> PoiCacheRequestListener;

	typedef std::function<bool(IPexeso::IState& state)> StateChangeListener;
	typedef std::function<void(IPexeso::ISnapshot&)> SnapshotListener;

	struct Params : ICortex::Params {
		ErrorListener errorListener;
		PoiCacheRequestListener poiCacheRequestListener;
		IBufferManager* pBufferManager;
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

	virtual void addClientGeneratedPoi(IPoi& poi, IUnaIterator& unaIterator) = 0;

	virtual void initLiveFeed() = 0;

	virtual void stopLiveFeed() = 0;

	virtual bool takeSnapshot() = 0;

	virtual bool getSnapshot(SnapshotId snapshotId) = 0;

	virtual void forceCacheRefresh() = 0;

// Sensor Inputs

	virtual void pushFrame(ImgBuffer imgBuf) = 0;

	virtual void pushHeading(float fHeading) = 0;
	
	virtual void pushPitch(float fPitch) = 0;

	virtual void pushLocation(Location location) = 0;

	//virtual void pushUserFeedback(UserAction& action, IRecognitionEvent* pRe,
	//	int iUserSelectedPoiId = NULL_POI_ID) = 0;
};

class IBufferManager {
public:
	int w, h;

	virtual void registerThread() = 0;
	virtual char* open(ImgBuffer imgBuf) = 0;
	virtual void close(ImgBuffer imgBuf, char*& pBuf) = 0;
	virtual ImgBuffer newBuffer() = 0;
	virtual void releaseBufferToCamera(ImgBuffer imgBuf) = 0;
	virtual void deleteBuffer(ImgBuffer imgBuf) = 0;
};
