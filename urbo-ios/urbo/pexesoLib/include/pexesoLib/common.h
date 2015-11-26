#pragma once

// raw input types
enum InputType {
    NV12,
    BGR
};

#ifndef INTERFACE_ONLY

// platform dependent stuff
#ifdef _MSC_VER
#define _VARIADIC_MAX 6	// MSVC doesn't support more variadic templates
#include "targetver.h"
#define DEPRECATED __declspec(deprecated)
#define __android_log_print(a,b,c,d,e)
#define __STDC_LIB_EXT1__
#endif

#ifdef __GNUC__
#define DEPRECATED __attribute__((deprecated))
#endif


// runtime includes
#include <stdio.h>
#if defined(SAFE_CRT)
#define vsnprintf vsnprintf_s
#endif
#include <cmath>
#include <iomanip>
#include <atomic>
#include <numeric>
#include <random>
#include <queue>
#include <functional>
#include <unordered_map>
#include <memory>
#include <array>
#include <bitset>
#include <chrono>
#include <thread>
#include <mutex>
#include <condition_variable>


// externals
#include "opencv2/opencv.hpp"


// math macros
#define F_PI 3.1415926535897932384626433832795f
#define DEG2RAD(d) (d * F_PI / 180.0f)
#define RAD2DEG(d) (d * 180.0f / F_PI)
#define FINFINITY (std::numeric_limits<float>::infinity())
#define INT_CEIL(a, b) (((a) + (b) - 1) / b)
#define SAME_SIGN(a, b) (a*b >= 0.0f)



// debug flag macros
#ifdef _DEBUG
#define DEBUG
#endif
#ifdef DEBUG

#define DBG_FLAG_PARAM , bool bDebug
#define DBG_FLAG_VAL , bDebug
#define DBG_PARAM(def) , def
#define DBG_YES	, true
#define DBG_NO , false
#define IF_DBG(CODE) if (bDebug) { \
	CODE \
}
#define ELSE(CODE) else { \
	CODE \
}
#define IF_NDEF_DBG(CODE)
#define IF_DEF_DBG(CODE) CODE

#else

#define DBG_FLAG_PARAM 
#define DBG_FLAG_VAL 
#define DBG_PARAM(def)
#define DBG_YES	
#define DBG_NO 
#define IF_DBG(CODE) 
#define ELSE(CODE)
#define IF_DEF_DBG(CODE)
#define IF_NDEF_DBG(CODE) CODE

#endif

#endif