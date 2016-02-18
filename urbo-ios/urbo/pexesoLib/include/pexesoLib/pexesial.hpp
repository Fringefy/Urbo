// file:	dataTypes.hpp
//
// summary:	Declares the global data types

#pragma once
#include "common.h"

using namespace std;

#ifndef PlatformPoi
class _PlatformPoi;
typedef _PlatformPoi* PlatformPoi;
#endif

/// <summary> A geographical location (we use decimal degrees). </summary>
struct Location {
	float fLat, fLng;
	float fRadius;

	Location(float fLat, float fLng, float fRadius = -1) :
		fLat(fLat), fLng(fLng),
		fRadius(fRadius)
	{}
	Location() :
		fLat(NAN)
	{}

	inline bool isNull() const {
		return isnan(fLat);
	}
};

struct IPoi {
	typedef uint32_t ClientId;
	const static int INVALID_ID = 0;

	IPoi(): clientId(INVALID_ID), pUser(nullptr) {}; // special constructor of an invalid object
	IPoi(string name, Location loc, string id, ClientId clientId):
		clientId(clientId == INVALID_ID ? ++nextId : clientId),
		name(name), id(id), loc(loc), pUser(nullptr) {};

	bool operator==(const IPoi& other) const {
		return operator==(other.id);
	}

	bool operator==(const string& otherId) const {
		return getId() == otherId;
	}

	string getId() const {
		if (id.empty()) {
			ostringstream osClientId;
			osClientId << clientId;
			return osClientId.str();
		}
		return id;
	}

	ClientId clientId;
	string name;
	string id;
	Location loc;
	PlatformPoi pUser;

private:
	static atomic<int> nextId;	// TODO: [SY] consider having the PoiCache manage this
};

struct IVote {
	IVote(const IPoi* poi, float fScore):
			poi(poi), fScore(fScore)
	{};

	const IPoi* poi;
	float fScore;

	bool operator<(const IVote& other) const {
		return fScore < other.fScore;
	}
};

typedef vector<IVote> PoiShortlist;

/// <summary> A UNA. </summary>
struct IUna {
	const char* pcUna;
	size_t nUnaLen;
};

template <typename... ArgTypes> class IIterator {
protected:
	IIterator(size_t size) : length(size) {};
	virtual ~IIterator() {};
	typedef function<void(ArgTypes...)> Function;

public:
	/// <summary>size must be locked during iterate()</summary>
	virtual void iterate(const Function& fn) = 0;
	virtual size_t size() const { return length; }

private:
	size_t length;
};

typedef IIterator<const string&> IUnaIterator;
typedef IIterator<const IPoi&, IUnaIterator&> IPoiIterator;

enum CameraRotation {
	// constants synchronized with CameraView
	ROTATION_0 = 0,
	ROTATION_90 = 1,
	ROTATION_270 = -1
};

enum Severity {
	DBG = 0,
	INFORMATION = 1,
	WARNING = 2,
	ERROR = 3,

	MAX_SEVERITY = 4
};

typedef function<void(Severity severity, string sMsg)> ErrorListener;

struct SensorState {
	float fHeading;
	float fPitch;
	float fVelocity;
	Location location;

	SensorState() = default;
	template <typename T> SensorState(const T& sensorState):
		fHeading(sensorState.fHeading),
		fPitch(sensorState.fPitch),
		fVelocity(sensorState.fVelocity),
		location(sensorState.location)
	{}

};


#ifndef INTERFACE_ONLY

#include "cortex/Cortex.hpp"
#include <forward_list>	// TODO: [SY] move to system includes

using namespace std;

struct Una {
	crtx::UNA crtxUna;

//	Una(const IUna& una) :
//		crtxUna(Cortex::deserializeUna(una.pcUna, una.nUnaLen))
//	{}
	Una(string una) :
		crtxUna(Cortex::deserializeUna(una.data(), una.size()))
	{}
};

struct Usig {

	const forward_list<Una>& getUnas() const { // TODO: even this should be called with PoiCache lock
		return unas;
	}

private:
	friend class PoiCache;
	forward_list<Una> unas;
	void emplace(Una& una) {
		unas.emplace_front(una);
	}
};

struct Poi : IPoi {
	Usig usig;

	Poi(const IPoi& poi) :
		IPoi(poi)
	{}
};

template<typename T>
class single_writer_atomic {
	T data[2];
	atomic<T*> p;

public:
	single_writer_atomic() :
		p(data)
	{}
	single_writer_atomic(T& init) :
		single_writer_atomic() {
		data[0] = init;
	}

	inline T load(std::memory_order order = std::memory_order_seq_cst) const {
		return *(p.load(order));
	}

	inline void store(T desired, std::memory_order order = std::memory_order_seq_cst) {
		T& newDataSlot = data[p == data ? 1 : 0];
		newDataSlot = desired;
		p.store(&newDataSlot, order);
	}

	inline operator T() const {
		return load();
	}

	inline T operator=(T desired) {
		store(desired);
		return desired;
	}
};

#if defined(_PTHREAD_H_) || defined(_PTHREAD_H)
class WriteLock {
protected:
	pthread_rwlock_t rwlock;

	WriteLock() {
		pthread_rwlock_init(&rwlock, nullptr);
	}
	~WriteLock() {
		pthread_rwlock_destroy(&rwlock);
	}
public:
	void lock() {
		pthread_rwlock_wrlock(&rwlock);
	}
	void unlock() {
		pthread_rwlock_unlock(&rwlock);
	}
};
class ReadLock : public WriteLock {
protected:
	ReadLock() {}
public:
	void lock() {
		pthread_rwlock_rdlock(&rwlock);
	}
};
class ReadWriteLock : public ReadLock {
public:
	ReadWriteLock() {}
};
#endif

#define ERR_MSG_MAX_LEN	256


class Util {
public:
	static void formatError(const ErrorListener& errorListener,
		Severity severity, string sMsg, ...)
	{
		char pcMsg[ERR_MSG_MAX_LEN];
		va_list args;
		va_start(args, sMsg);
		vsnprintf(pcMsg, ERR_MSG_MAX_LEN, sMsg.c_str(), args);
		va_end(args);

		errorListener(severity, pcMsg);
	}
};

#endif
