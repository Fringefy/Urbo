package com.fringefy.urbo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Snapshot extends RecoEvent { // TODO: maybe simply merge the three fields into RecoEvent and get rid of this class?

	transient private Poi poi;
	transient private Urbo.PoiVote[] votes;
	transient byte[] jpegBuffer;

	public Poi getPoi() {
		return poi;
	}
	public Urbo.PoiVote[] getVotes() {
		return votes;
	}
	public InputStream getJpegStream() {
		return new ByteArrayInputStream(jpegBuffer);
	}
}