// file:	ICortex.hpp
//
// summary:	Declares the ICortex interface

#pragma once

// this macro is defined when the header is used internally, in which case we do not want
// to include system headers that are a part of the PCH. 
#ifndef CORTEX_IMPL
#include <string>
#include <vector>
#include <functional>
#include "opencv2/opencv.hpp"
#define INTERFACE_ONLY
#include "cortical.hpp"
#endif

// maximum encoded UNA size, we use base64 so x2 is an upper bound
#define MAX_UNA_B64_SIZE	25600

namespace crtx {

enum CrtxResult {
	CRTX_OK = 0,
	CRTX_NO_MATCH = -2,		// the query was not found within the shortlist
	CRTX_NON_INDEXABLE = -1	// the query does not contain a valid location
};

template<typename UnaTy = string, typename IdTy = int>
struct VoteEntry {
	UnaTy una;
	IdTy id;
	float fScore;
		
	VoteEntry() { assert(false);  }
	VoteEntry(UnaTy una, IdTy id, float fScore = NAN) :
		una(una),
		id(id),
		fScore(fScore)
	{}

	// decending score order
	friend bool operator< (VoteEntry &entry1, VoteEntry &entry2) {
		return entry1.fScore > entry2.fScore;
	};
};

/// <summary> A cortex vote. </summary>
/// <remarks> The primary cortex result - a vector in the order of the shortlist with the
/// 	corresponding score for each POI. </remarks>
template<typename UnaTy = std::string, typename IdTy = int>
struct Vote : std::vector<VoteEntry<UnaTy, IdTy>> {
	
	typedef VoteEntry<UnaTy, IdTy> Entry;
	typedef std::vector<Entry> V;

	CrtxResult result;			///< identification result
	float fConfidence;			///< the confidence in the selected UNA
	Entry* pMaxVote;

	Vote() :
		result(CRTX_NON_INDEXABLE),
		fConfidence(0),
		pMaxVote(nullptr)
	{}
	Vote(size_t nSz) :
		Vote() {
		V::reserve(nSz);
	}
	template<typename It, typename Fn>
	Vote(It itBegin, It itEnd, Fn unaAndIdGetter) :
		Vote(itEnd - itBegin) {
		
		for (It it = itBegin; it != itEnd; it++) {
			auto unaAndId = unaAndIdGetter(*it);
			emplace_back(unaAndId.first, unaAndId.second);
		}
	}

	inline Entry* find(IdTy id) {
		return &*find_if(V::begin(), V::end(), [&](Entry& entry) { 
			return entry.id == id; 
		});
	}

	void clear() {
		V::clear();
		fConfidence = 0;
		pMaxVote = nullptr;
		result = CRTX_NON_INDEXABLE;
	}
};


/// <summary> The Cortex public interface. </summary>
/// <remarks> This is used both as a public interface for Cortex consumers who do not
/// 	wish to include the entire Cortex header set. It is also used as a factory with
/// 	the createInstance method. </remarks>
class ICortex
{
public:
	virtual ~ICortex() {};

	/// <summary> The public params. </summary>
	/// <remarks> A subset of all the cortex params which are of interest to a consumer.
	/// 	The interface routes these params to the params structure. The rest of the
	/// 	params are loaded through XML. </remarks>
	struct Params {
		bool bIdentify;
		InputType inputType;
	};

	/// <summary> Creates a cortex instance. </summary>
	/// <param name="params"> Cortex interface public parameters. </param>
	/// <param name="sParamsXmlFile"> The parameters XML file. </param>
	/// <returns> null if it fails, else the new instance. </returns>
	static ICortex* createInstance(Params& params, std::string sParamsXmlFile);
	
	/// <summary> Does the magic. </summary>
	/// <param name="imQ"> [in] The query image </param>
	/// <param name="viPois"> vector of POIs to test the query against. </param>
	/// <param name="vote"> [in,out] The list of UNAs to vote on. </param>
	/// <param name="pcUnaQ"> [out] If non-null, the serialized UNA of the query. </param>
	/// <param name="piUnaLen"> [out] If non-null, length of the UNA. </param>
	/// <returns> true if it succeeds, false if it fails. </returns>
	virtual bool identify(cv::Mat& imQ, Vote<std::string>& vote,	
		unsigned char* pcUnaQ, size_t* piUnaLen, const InterruptCallback& cb) = 0;
#define IDENTIFY_INTERRUPT				0
#define IDENTIFY_ORIGINAL_BUFFER_FREE	1

};

}