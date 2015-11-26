// file:	cortical.hpp 
// summary:	Declares and implements global methods and datastructures
//		used by the Cortext and the sub components. 

#pragma once

#ifndef INTERFACE_ONLY

#include "../common.h"
#include <set>
#include "externals/tinyxml2.h"
extern "C" {
#include "externals/cencode.h"
#include "externals/cdecode.h"
}

using namespace std;
using namespace cv;

#endif

namespace crtx {

#define INTERRUPT_OK				0
#define INTERRUPT_DONE_WITH_BUFFER	1
typedef std::function<bool(int iStatusCode)> InterruptCallback;
#define NO_INTERRUPT	([](int) -> bool { return true; })

#ifndef INTERFACE_ONLY

#define HELLO_ALICE

// uchar string (used primarily for UNA serialization buffers)
typedef basic_string<unsigned char> ustring;

// gray matter types
#define PP_RECTIFIER		1
#define UM_STRANDS			10
#define UM_STRANDSTX		11
#define LD_LINE_RESPONSE	100
#define CM_NBR				1000
#define	SM_FUSED_MATCHER	10000
#define SM_NN_MATCHER		10001


#define PREPROCESS			PP_RECTIFIER
#define UNA_MANAGER			UM_STRANDS
#define LINE_DETECTOR		LD_LINE_RESPONSE
#define CHAIN_MANAGER		CM_NBR
#define STRANDS_MATCHER		SM_FUSED_MATCHER

// constants for foveal channels
#define CHAN_GRAY		(1 << 0)
#define CHAN_BGR		(1 << 1)
#define CHAN_YUV		(1 << 2)


// UNA versions
enum UnaVer {
	UNAVER_NBR_V1 = 2
};


/// <summary> Defines the basis of all chains. </summary>
/// <remarks> Every chain has a location (0-based) and can be ordered according to that
/// 	location. This keeps UNAs in ascending location order. </remarks>
struct BaseChain {
public:
	int iLoc;			// location
};
#define CHAIN_LOC_BYTES	2	// location serialization size

inline bool operator< (const BaseChain& lhs, const BaseChain& rhs)
{
	return lhs.iLoc < rhs.iLoc;
}

/// <summary> Defines the basis of all strands. </summary>
/// <typeparam name="Chain"> Type of the chain this strand is comprised from. </typeparam>
template<typename Chain>
struct BaseStrand : vector<Chain> {
public:
	int iRefHeight;	// the reference height of the strand
	uint iScale;		// the scale this strand came from (0 is original)

#ifdef DEBUG
	Rect roi;	// the ROI of the tube the strand came from
	Mat imSrc;	// the image the strand came from
#endif
};
#define STRAND_LENGTH_BYTES	2	// length serialization size
#define REF_HEIGHT_BYTES	2	// ref height serialization size

struct BaseUNA {
public:
	enum Status {
		OK,
		NON_INDEXABLE
	} status;
};



/// <summary> noramlizes a gray scale image to mean 0 and stddev 1. </summary>
/// <param name="imIn"> [in] Input image. </param>
/// <param name="imOut"> [out] Output image (can be = imIn). </param>
inline void normalizeMeanStd(Mat& imIn, Mat& imOut)
{
	Scalar sMean, sStdDev;
	meanStdDev(imIn, sMean, sStdDev);

	subtract(imIn, sMean, imOut);
	divide(imOut, sStdDev, imOut);
}

/// <summary> noramlizes a gray scale image to rms 1. </summary>
/// <param name="imIn"> [in] Input image. </param>
/// <param name="imOut"> [out] Output image (can be = imIn). </param>
inline void normalizeRms(double factor, Mat& imIn, Mat& imOut)
{
	Scalar sMean, sStdDev, sRms;
	meanStdDev(imIn, sMean, sStdDev);
	sRms = factor * sqrt(sMean[0] * sMean[0] + sStdDev[0]);
	divide(imIn, sRms, imOut);
}

/// <summary> Iterates every possible pair (n choose 2) between begin and end. </summary>
/// <typeparam name="It"> Type of the iterator. </typeparam>
/// <typeparam name="Fn"> The function to execue on the pair. </typeparam>
/// <param name="itBegin"> Forward iterator to beginning of list. </param>
/// <param name="itEnd"> Iterator to the end of the list. </param>
/// <param name="fn"> The function to execute on the pair. </param>
template<typename It, typename Fn>
inline void for_each_pair(It itBegin, It itEnd, Fn fn)
{
	if (itEnd - itBegin > 2) {
		for (It it = itBegin; it != itEnd - 1; it++) {
			for (It it2 = it + 1; it2 != itEnd; it2++)
				fn(*it, *it2);
		}
	}
}

/// <summary> Iterates every consecutive pair (n-1) between begin and end. </summary>
/// <typeparam name="It"> Type of the iterator. </typeparam>
/// <typeparam name="Fn"> The function to execue on the pair. </typeparam>
/// <param name="itBegin"> Forward iterator to beginning of list. </param>
/// <param name="itEnd"> Iterator to the end of the list. </param>
/// <param name="fn"> The function to execute on the pair. </param>
template<typename It, typename Fn>
inline void for_each_consecutive_pair(It itBegin, It itEnd, Fn fn)
{
	if (itEnd - itBegin > 2) {
		for (It it = itBegin; it != itEnd - 1; it++) {
			fn(*it, *(it+1));
		}
	}
}

/// <summary> N choose 2. </summary>
/// <typeparam name="T"> Generic type parameter. </typeparam>
/// <param name="n"> n. </param>
/// <returns> n choose 2. </returns>
template<typename T>
inline T nC2(T n)
{
	return n * (n - 1) / 2;
}

/// <summary> Normalize homogenous coordinate. </summary>
/// <param name="v"> [in,out] The vector to process. </param>
/// <param name="iIdx"> Zero-based index of the index of the entry that should be
/// 	normalized by (either 0 or 1). </param>
inline void normh(Vec3f& v, int iIdx)
{
	v[2] /= v[iIdx];
	v[!iIdx] /= v[iIdx];
	v[iIdx] = 1;
}

/// <summary> Normalize vector to norm1. </summary>
/// <typeparam name="_Tp"> Underlying type. </typeparam>
/// <typeparam name="m"> Vector size. </typeparam>
/// <typeparam name="n"> Vector size. </typeparam>
/// <param name="M"> [in,out] The vector to process. </param>
/// <param name="iBegin"> (Optional) zero-based index to start normalization from. </param>
/// <param name="iEnd"> (Optional) zero-based index to end normalization. </param>
/// <param name="fFactor"> (Optional) normalization factor (multiplied inside the sqrt). </param>
/// <returns> The normalized vector. </returns>
template<typename _Tp, int m, int n>
inline Matx<_Tp, m, n>& normc(Matx<_Tp, m, n>& M, int iBegin = 0, int iEnd = m*n,
	float fFactor = 1.0f)
{
	auto fNorm = std::sqrt(normL2Sqr<_Tp, _Tp>(M.val + iBegin, iEnd - iBegin) * fFactor);
	for (_Tp *pf = M.val + iBegin, *pfEnd = M.val + iEnd; pf != pfEnd; pf++)
		*pf /= fNorm;
	return M;
}

/// <summary> Query if any if the vector components is NaN. </summary>
/// <param name="v"> [in] The vector to check. </param>
/// <returns> true if any element is NaN, false otherwise. </returns>
inline bool isnan(const Vec3f& v)
{
	return std::isnan(v[0]) || std::isnan(v[1]) || std::isnan(v[2]);
}

/// <summary> Mimics MATLAB's linspace </summary>
/// <param name="fMin"> The minimum value. </param>
/// <param name="fMax"> The maximum value. </param>
/// <param name="nSteps"> How many steps. </param>
/// <returns> A vector of nSteps numbers linearly spaced between fMin and fMax </returns>
inline vector<float> linspace(float fMin, float fMax, int nSteps)
{
	vector<float> vfRange;
	vfRange.reserve(++nSteps);
	
	float fStep = (fMax - fMin) / nSteps;
	for (int i = 0; i < nSteps; i++, fMin += fStep)
		vfRange.push_back(fMin);

	return vfRange;
}

/// <summary> Find the median in a non-sorted list. </summary>
/// <typeparam name="It"> Type of the iterator. </typeparam>
/// <typeparam name="Pr"> Comparison predicate. </typeparam>
/// <param name="itBegin"> Random access iterator to head of list. </param>
/// <param name="itEnd"> Iterator to end of list. </param>
/// <param name="n"> Length of list. </param>
/// <param name="pred"> Comparison predicate. </param>
/// <returns> Iterator pointing to the median value. </returns>
template <typename It, typename Pr>
inline It median(It itBegin, It itEnd, int n, Pr pred)
{
	int middle = n / 2;
	nth_element(itBegin, itBegin + middle, itEnd, pred);
	return itBegin + middle;
}

/// <summary> Find the the minimum and maximum values in a Mat. </summary>
/// <param name="m"> The const Mat to process. </param>
/// <param name="iChannel"> Zero-based index of the channel to process. </param>
/// <param name="pfMin"> [out] If non-null, the pf minimum. </param>
/// <param name="pfMax"> [out] If non-null, the pf maximum. </param>
inline void minMax(const Mat& m, int iChannel, float* pfMin, float* pfMax)
{
	*pfMax = -FINFINITY;
	*pfMin = FINFINITY;
	
	for (int r = 0; r < m.rows; r++) {
		for (const Vec3f *pv = m.ptr<Vec3f>(r), *pvEnd = pv+m.cols; pv != pvEnd; pv++) {
			if ((*pv)[iChannel] > *pfMax)
				*pfMax = (*pv)[iChannel];
			if ((*pv)[iChannel] < *pfMin)
				*pfMin = (*pv)[iChannel];
		}
	}
}


// -- Serialization Helpers  ------------------------------------------------------------
/*
* Tiny helpers for cross-platform binary serialization of numerics
* TODO: when this starts becoming complicated, consider using Google's ProtoBuf
*/

/// <summary> Serializes an int. </summary>
/// <typeparam name="nBytes"> How many bytes to use. </typeparam>
/// <typeparam name="t_"> Type of int. </typeparam>
/// <param name="n"> The int to write. </param>
/// <param name="pc"> [out] The output buffer. </param>
/// <returns> How many bytes were written. </returns>
template<size_t nBytes, typename int_>
inline size_t writeInt(int_ n, uchar* pc)
{
	for (size_t i = 0; i < nBytes; i++)
		pc[i] = (n >> (i*8)) & 0xff;
	return nBytes;
}

/// <summary> Reads an int. </summary>
/// <typeparam name="nBytes"> How many bytes to use. </typeparam>
/// <typeparam name="t_"> Type of int. </typeparam>
/// <param name="pc"> [in] Buffer to read from. </param>
/// <param name="n"> [out] The int. </param>
/// <returns> How many bytes were read. </returns>
template<size_t nBytes, typename int_>
inline size_t readInt(const uchar* pc, int_& n)
{
	n = static_cast<int_>(0);
	for (size_t i = 0; i < nBytes; i++)
		n = static_cast<int_>(n | (pc[i] << (i*8)));
	return nBytes;
}

/// <summary> Quantizes and writes a float. </summary>
/// <typeparam name="nBytes"> How many bytes to use. </typeparam>
/// <typeparam name="float_"> Type of the float. </typeparam>
/// <param name="f"> The float_ to process. </param>
/// <param name="fMin"> The minimum value for quantization. </param>
/// <param name="fMax"> The maximum value for quantization. </param>
/// <param name="pc"> [out] Output buffer. </param>
/// <returns> How many bytes were written. </returns>
template<size_t nBytes, typename float_>
inline size_t writeFloat(const float_& f, float_ fMin, float_ fMax, uchar* pc)
{
	// linearly quantize the float, ((1 << (nBytes * 8)) - 1) = max int value
	unsigned int n = static_cast<unsigned int>( ((1 << (nBytes * 8)) - 1) *
		(f > fMin ? (f < fMax ? f - fMin : fMax - fMin) : 0) / (fMax - fMin) );
	
	// serialize the int
	writeInt<nBytes>(n, pc);
	return nBytes;
}

/// <summary> Reads a quantized float. </summary>
/// <typeparam name="nBytes"> How many bytes to read. </typeparam>
/// <typeparam name="float_"> Type of the float. </typeparam>
/// <param name="pc"> [in] Buffer to read from. </param>
/// <param name="fMin"> The minimum value for quantization. </param>
/// <param name="fMax"> The maximum value for quantization. </param>
/// <param name="f"> [out] The result. </param>
/// <returns> How many bytes were read. </returns>
template<size_t nBytes, typename float_>
inline size_t readFloat(uchar* pc, float_ fMin, float_ fMax, float_& f)
{
	// deserialize the int
	unsigned int n;
	readInt<nBytes>(pc, n);

	// linearly de-quantize the float, ((1 << (nBytes * 8)) - 1) = max int value
	f = fMin + ((fMax - fMin) * n) / ((1 << (nBytes * 8)) - 1);
	return nBytes;
}


#ifdef DEBUG

// multipurpose struct for various debug params
struct DebugParams {
	int i1;

	DebugParams() : i1(0) {}
};

#endif

#endif
}

using namespace crtx;