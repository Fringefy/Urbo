package com.fringefy.urbo;

import java.util.Map;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Query;

interface Odie {

	@GET("/pois")
	OdieUpdate getPois(@Query("lat") double dLat, @Query("lng") double dLong,
                    @Query("accuracy") double dAccuracy,
                    @Query("country") String sCountryCode,
                    @Query("deviceId") String sDeviceId);

	class OdieUpdate {
		public Poi[] pois;
		public float radius;
		public int hitMeAgainIn;
		public String s3Bucket;
		public String s3Folder;
	}

	@PUT("/pois")
	PutResponse sync(@Body PutRequest putReq);

	class PutRequest {
		public Poi[] pois;
		public RecoEvent[] recognitionEvents;
	}

	class PutResponse {
		class Pois {
			Map<String, String> syncList;
		}
		Pois pois;
	}
}