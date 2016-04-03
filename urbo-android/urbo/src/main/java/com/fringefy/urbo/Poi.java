package com.fringefy.urbo;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

/**
 * Represents a POI. This class can be serialized with GSON to comply with the ODIE
 * back-end.
 * This POJO take care of a self-incremented unique client ID, of adding client-generated
 * UNAs, and enumerating the Urls that come from Factual and other sources.
 */

public class Poi {

	/** global POI id, can be null in case of a client-only POI */
	private String _id;

	/** unique client ID */
	private String clientId;
	public boolean isClientOnly() { return _id == null; }

	/** returns the global ID, or if it isn't present - the client ID */
	public String getId() { return isClientOnly() ? clientId : _id; }

	private String name;
	public String getName() { return name; }

	private String description;
	public String getDescription() { return description; }
	public Poi setDescription(String sDescription) {
		lockCheck();
		description = sDescription;
		return this;
	}

 	float[] loc;
	// TODO: maybe keep accuracy, too?

	/** POI category (type) */
	public final static int UNKNOWN = 0;
	public final static int Food = 1;
	public final static int Bars = 2;
	public final static int Travel = 3;
	public final static int Landmark = 4;
	public final static int Street = 9;

	private int type;
	public int getType() { return type; }
	public Poi setType(int type) {
		lockCheck();
		this.type = type;
		return this;
	}

	/** Address and locality */
	private String locality;
	public String getLocality() { return locality; }
	public Poi setLocality(String sLocality) {
		lockCheck();
		locality = sLocality;
		return this;
	}

	private String address;
	public String getAddress() { return address; }
	public Poi setAddress(String sAddress) {
		lockCheck();
		address = sAddress;
		return this;
	}

	/** creation time-stamp */
	private Date timestamp;
	public Date getTimestamp() { return timestamp; }

	/** first comment to add to the POI when creating it */
	private String firstComment;
	public Poi setFirstComment(String sFirstComment) {
		lockCheck();
		firstComment = sFirstComment;
		return this;
	}

	String imgFileName;

	/**
	 * A visual urnban signature
	 */
	private static class Usig {

// Inner Types

		private static class Una {
			double azimuth;
			String data;

			Una(double camAzimuth, String sUna) {
				azimuth = camAzimuth;
				data = sUna;
			}
		}
		private static class Geo {
			float[][] facade;
		}

// Fields

		Una[] unas;
		Geo geo;
	}

	Usig usig;
	private float[][] facade;

	/** Website */
	private String website;
	public String getWebsite() { return website; }

	/** Data source id's */
	private String googlePlacesId;
	public String getGooglePlacesId() { return googlePlacesId; }
	private String factualId;
	public String getFactualId() { return factualId; }

	/** Factual ID's and URLs, data from http://www.factual.com/products/places-crosswalk */
	// data from http://www.factual.com/products/places-crosswalk
	private String tripadvisorId;
	private String tripadvisorUrl;
	private String yelpId;
	private String yelpUrl;
	private String foursquareId;
	private String foursquareUrl;
	private String wikipediaId;
	private String wikipediaUrl;
	private String gogobotId;
	private String gogobotUrl;
	private String facebookId;
	private String facebookUrl;
	private String twitterId;
	private String twitterUrl;
	private String opentableId;
	private String opentableUrl;
	private String grubhubId;
	private String grubhubUrl;
	private String aolId;
	private String aolUrl;
	private String homeUrl;
	private String eventfulId;
	private String eventfulUrl;
	private String hotelsId;
	private String hotelsUrl;
	private String instagramId;
	private String instagramUrl;
	private String openmenuId;
	private String openmenuUrl;
	private String superpagesId;
	private String superpagesUrl;
	private String yahoogeoplanetId;
	private String yahoogeoplanetUrl;
	private String yellopagesId;
	private String yellowpagesUrl;
	private String zagatId;
	private String zagatUrl;
	private String happyintlvId;
	private String happyintlvUrl;

	public static enum FactualLink {
		Google,
		Home,
		Tripadvisor,
		Yelp,
		Foursquare,
		Wikipedia,
		Gogobot,
		Facebook,
		Twitter,
		Opentable,
		Grubhub,
		AOL,
		Eventful,
		Hotels,
		Instagram,
		OpenMenu,
		SuperPages,
		YahooGeoplanet,
		YellowPages,
		Zagat,
		HappyInTLV,
		Alice
	}

	public String getFactualUrl(FactualLink fl) {
		switch (fl) {
			case Google:
				try {
					return "https://www.google.com/search?q=" +	URLEncoder.encode(
							name + (locality == null ? "" : ", " + locality), "utf-8");
				}
				catch (UnsupportedEncodingException e) {
					return "https://www.google.com/search?q=" + name;
				}

			case Home:
				return homeUrl;
			case Facebook:
				return facebookUrl;
			case Foursquare:
				return foursquareUrl;
			case Gogobot:
				return gogobotUrl;
			case Grubhub:
				return grubhubUrl;
			case Opentable:
				return opentableUrl;
			case Tripadvisor:
				return tripadvisorUrl;
			case Twitter:
				return twitterUrl == null ? null :
						twitterUrl.replace("//twitter.", "//mobile.twitter.");
			case Wikipedia:
				return wikipediaUrl;
			case Yelp:
				return yelpUrl;
			case AOL:
				return aolUrl;
			case Eventful:
				return eventfulUrl;
			case Hotels:
				return hotelsUrl;
			case Instagram:
				return instagramUrl;
			case OpenMenu:
				return openmenuUrl;
			case SuperPages:
				return superpagesUrl;
			case YahooGeoplanet:
				return yahoogeoplanetUrl;
			case YellowPages:
				return yellowpagesUrl;
			case Zagat:
				return zagatUrl;
			case HappyInTLV:
				return happyintlvUrl;
		}
		return null;
	}

// Construction

	public Poi(@NonNull String sName) {
		this.timestamp = new Date();
		this.name = sName;
	}

// Public Methods

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Poi) {
			Poi poiOther = (Poi)o;
			return getId().equals(poiOther.getId());
		}
		else if (o instanceof String) {
			return getId().equals(o);
		}
		return false;
	}


// Private Methods

	private void lockCheck() {
		if (getId() != null) {
			throw new IllegalStateException("Cannot modify an already registered POI");
		}
	}
}