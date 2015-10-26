package com.fringefy.urbo;

import java.util.List;
import java.util.Map;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Query;

interface Odie {

	@GET("/pois")
	Pois getPois(@Query("lat") double dLat, @Query("lng") double dLong,
                    @Query("accuracy") double dAccuracy,
                    @Query("country") String sCountryCode,
                    @Query("deviceId") String sDeviceId,
                    @Query("settings") boolean bGetSettings);

	class Pois {
		public List<Poi> pois;
		public float radius;
		public int hitMeAgainIn;
		public String s3Bucket;
		public String s3Folder;
		public String sharepageUrl;
	}

	@PUT("/pois")
	PutResponse sync(@Body PutRequest putReq);

	class PutRequest {
		public Poi[] pois;
		public RecoEvent[] recognitionEvents;
	}

	class PutResponse {
		public class PoisResponse {
			public Map<String, String> syncList;
		}

		public PoisResponse pois;
	}
}