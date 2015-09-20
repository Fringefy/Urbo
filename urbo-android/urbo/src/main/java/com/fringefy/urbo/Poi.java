package com.fringefy.urbo;

import android.location.Location;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a POI. This class can be serialized with GSON to comply with the ODIE
 * back-end. The only logic in this POJO is a self-incremented unique client ID
 */

/* TODO: [AC] strip this class of non-essential data (i.e. addresses, URLs etc.)
    These fields (and others) can be contained in the host's subclasses
 */

@SuppressWarnings("unused")
public class Poi {

// Fields

	private volatile boolean bLocked;

	/** global POI id, can be null in case of a client-only POI */
	private String _id;
	void setId(String sId) { _id = sId; clientId = null; }

	/** unique client ID */
	private String clientId;
	boolean isClientOnly() { return _id == null; }
	static AtomicInteger iNextId = new AtomicInteger(1); // the next available client ID

	/** returns the global ID, or if it isn't present - the client ID */
	public String getId() { return isClientOnly() ? clientId : _id; }

	private String name;
	public String getName() { return name; }
	public void setName(String sName) {
		lockCheck();
		name = sName;
	}

	private String description;
	public String getDescription() { return description; }
	public void setDescription(String sDescription) {
		lockCheck();
		description = sDescription;
	}

	private double[] loc;
	private transient Location location;
	public Location getLocation() {
		if (location == null) {
			location = new Location("Urbo");
			location.setLatitude(loc[Constants.LAT]);
			location.setLongitude(loc[Constants.LONG]);
		}

		return location;
	}
	public void setLocation(Location location) {
		lockCheck();
		loc[Constants.LAT] = location.getLatitude();
		loc[Constants.LONG] = location.getLongitude();
	}

	/** POI category (type) */
	public enum Type {
		Food(1),
		Bars(2),
		Travel(3),
		Landmark(4);

		private int iType;

		Type(int iType) {
			this.iType = iType;
		}
		public int toInt() {
			return iType;
		}
	}
	private Type type;
	public Type getType() { return type; }
	public void setType(Type type) {
		lockCheck();
		this.type = type;
	}

	/** Address and locality */
	private String locality;
	public String getLocality() { return locality; }
	public void setLocality(String sLocality) {
		lockCheck();
		locality = sLocality;
	}

	protected String address;
	public String getAddress() { return address; }
	public void setAddress(String sAddress) {
		lockCheck();
		address = sAddress;
	}

	/** creation time-stamp */
	private Date timestamp;
	public Date getTimestamp() { return timestamp; }
	public void setTimestamp(Date timestamp) {
		lockCheck();
		this.timestamp = timestamp;
	}

	/** first comment to add to the POI when creating it */
	private String firstComment;
	public void setFirstComment(String sFirstComment) {
		lockCheck();
		firstComment = sFirstComment;
	}


	/** Urban signature (USIG) */
	Usig usig;

	/** Website */
	private String website;
	public String getWebsite() { return website; }

	/** Data source id's */
	private String googlePlacesId;
	public String getGooglePlacesId() { return googlePlacesId; }
	private String factualId;
	public String getFactualId() { return factualId; }

	/** Factual ID's and URLs, data from http://www.factual.com/products/places-crosswalk */
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
	private String homeUrl;

	public enum FactualLink {
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
		AliceWho
	}

	public String getFactualUrl(FactualLink fl) {
		switch (fl) {
			case Google:
				try {
					return "https://www.google.com/search?q=" +	URLEncoder.encode(
							name + (locality == null ? "" : ", " + locality), "utf-8");
				}
				catch (UnsupportedEncodingException e) {
					// TODO: error
					return "https://www.google.com/search?q=" + name;
				}
			case AliceWho:
				if (isClientOnly()) {
					return null;
				}
				return "http://tabs.alicewho.com/" + getId();

			case Home:
				return website;
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
		}
		return null;
	}

	private String imgFileName;
	public String getImageUrl() {
		return "http://fringefyimages.s3.amazonaws.com/d/" + imgFileName;
	}


// Construction

	public Poi() {
		this.bLocked = false;
		this.clientId = Integer.toString(iNextId.getAndIncrement());
	}

	public Poi(String sName, String sDescription, Type type, String sFirstComment,
	           Location location, Date timestamp, String sLocality, String sAddress) {
		this();

		this.name = sName;
		this.description = sDescription;
		this.firstComment = sFirstComment;
		setLocation(location);
		this.timestamp = timestamp;
		this.locality = sLocality;
		this.address = sAddress;
		this.type = type;
	}


// Public Methods

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Poi){
			Poi poiOther = (Poi)o;
			return ((this._id != null && this._id.equals(poiOther._id)) ||
					(this.clientId != null && this.clientId.equals(poiOther.clientId)));
		}
		return false;
	}


// Private Methods

	void lock() {
		bLocked = true;
	}

	private void lockCheck() {
		if (bLocked)
			throw new UnsupportedOperationException("Cannot modify an already registered POI");
	}
}