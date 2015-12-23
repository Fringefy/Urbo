package com.fringefy.urbo;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.squareup.okhttp.OkHttpClient;

abstract class OdieFactory {

	static Gson getGson() {
		return new GsonBuilder()
			.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
				@Override
				public Date deserialize(JsonElement json, Type typeOfT,
										JsonDeserializationContext context)
						throws JsonParseException {
					return new Date(json.getAsJsonPrimitive().getAsLong());
				}
			})
			.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
				@Override
				public JsonElement serialize(Date d, Type typeOfT,
											 JsonSerializationContext context) {
					return new JsonPrimitive(d.getTime());
				}
			})
			.create();
	}

	static Odie getInstance(String sEndpoint, final String apiKey) {

		OkHttpClient okHttpClient = new OkHttpClient();
		okHttpClient.setReadTimeout(140, TimeUnit.SECONDS);
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setLogLevel(BuildConfig.DEBUG ?
						RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE)
				.setEndpoint(sEndpoint)
				.setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						request.addHeader("x-api-key", apiKey);
						request.addHeader("Accept", "application/json");
					}
				})
				.setConverter(new GsonConverter(getGson()))
				.setClient(new OkClient(okHttpClient))
				.build();

		return restAdapter.create(Odie.class);
	}
}