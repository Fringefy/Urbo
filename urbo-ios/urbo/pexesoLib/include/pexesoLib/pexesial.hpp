// file:	dataTypes.hpp
//
// summary:	Declares the global data types

#pragma once
#include "common.h"

using namespace std;

/// <summary> A geographical location (we use decimal degrees). </summary>
struct Location {
	float fLat, fLng;
	float fRadius;

	Location(float fLat, float fLng, float fRadius) :
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
	void* pUser;
	Location loc;
};

/// <summary> A UNA. </summary>
struct IUna {
	const char* pcUna;
	size_t nUnaLen;
};

/// <summary> A poi cache iterator. </summary>
/// <remarks> A consumer should implement this interface to iterate POIs when
/// 	updating the POI cache. </remarks>
class IUnaIterator {
protected:
	IUnaIterator(int nUnas) :
			nUnas(nUnas) {};
	virtual ~IUnaIterator() {};

public:
	const int nUnas;
	virtual void operator()(const function<void(IUna una)>& fn) = 0;
};

class IPoiIterator {
protected:
	IPoiIterator(int nPois) :
		nPois(nPois) {};
	virtual ~IPoiIterator() {};

public:
	const int nPois;
	virtual void release(IPoi& poi) {}
	virtual void operator()(
		const function<void(IPoi poi, IUnaIterator& unaIterator)>& fn) = 0;
};



enum Severity {
	DBG = 0,
	INFORMATION = 1,
	WARNING = 2,
	ERROR = 3,

	MAX_SEVERITY = 4
};

typedef std::function<void(Severity severity, std::string sMsg)> ErrorListener;
typedef void* ImgBuffer;

struct SensorState {
	float fHeading;
	float fPitch;
	Location location;

	SensorState() = default;
	template <typename T> SensorState(const T& sensorState):
		fHeading(sensorState.fHeading),
		fPitch(sensorState.fPitch),
		location(sensorState.location)
	{}

};


#ifndef INTERFACE_ONLY

#include "cortex/Cortex.hpp"

using namespace std;

struct Una {
	crtx::UNA crtxUna;

	Una(IUna una) :
		crtxUna(Cortex::deserializeUna(una.pcUna, una.nUnaLen))
	{}
};

struct Usig {
	vector<Una> unas;
};

struct Poi : IPoi {
	Usig usig;

	Poi(IPoi& poi) :
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

	inline T exchange(T desired, std::memory_order order = std::memory_order_seq_cst) {
		T& prev = *p;
		store(desired, order);
		return prev;
	}

	inline operator T() const {
		return load();
	}

	inline T operator=(T desired) {
		store(desired);
		return *p;
	}
};


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