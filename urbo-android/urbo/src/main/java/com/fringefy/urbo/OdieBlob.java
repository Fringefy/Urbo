package com.fringefy.urbo;

import android.os.SystemClock;
import android.util.Log;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

class OdieBlob {

	private static final String TAG = "OdieBlob";
	private static final int REPEAT_DELAY = 30000;

// Members

	private final OkHttpClient okHttpClient;
	private final File fIimgDir;
	private static final FilenameFilter filenameFilterJpeg = new FilenameFilter() {
		@Override public boolean accept(File dir, String filename) {
			return (filename.endsWith(".jpg"));
		}
	};

	private String bucketName = "fringefyimages";
	private String folder = "d/";
	private String serverAdress;

// Construction

	OdieBlob(File imgDir) {
		fIimgDir = imgDir;
		okHttpClient = new OkHttpClient();
		setBucket(bucketName, folder);
	}

    //TODO: organize according to Inner Classes, members,
    // construction, events, public methods, private methods

	/**
	 * Synchronous Get method to upload an image to the Odie server.
	 * @param fImg The image to upload
	 * @return return true if succeeded to upload
	 * @throws IOException
	 */
	synchronized boolean uploadImage(File fImg) throws IOException {
		try {
			uploadInternal(fImg);
			return fImg.delete();
		}
		catch (Exception e) {
            Log.e(TAG, "Failed to upload " + fImg.getPath() +
                    " to " + toStringForLog(), e);
		}
		return false;
	}

	private void uploadInternal(File fImg) throws IOException {

		Request request = new Request.Builder()
				.header("Content-Type", "image/jpg")
				.url(String.format(serverAdress, fImg.getName()))
				.put(RequestBody.create(MediaType.parse("image/jpg"), fImg))
				.build();

		Response response = okHttpClient.newCall(request).execute();

		if (!response.isSuccessful()){
            throw new IOException("Unexpected code " + response);
        }

        Headers responseHeaders = response.headers();
        for (int i = 0; i < responseHeaders.size(); i++) {
           Log.d(TAG,responseHeaders.name(i) + ": " + responseHeaders.value(i));
        }
        Log.d(TAG,response.body().string());
}

	synchronized File[] listOutstandingImages() {
		return fIimgDir.listFiles(filenameFilterJpeg);
	}

	void setBucket(String s3Bucket, String s3Folder) {
		if (s3Bucket != null && !s3Bucket.isEmpty()) {
			bucketName = s3Bucket;
		}
		if (s3Folder != null && !s3Folder.isEmpty()) {
			folder = s3Folder + "/";
		}
		else {
			folder = "";
		}
		serverAdress = "https://" + bucketName + ".s3-eu-west-1.amazonaws.com/" + folder +
				"%s?Content-Type=image%2Fjpg&Expires=1437757726&x-amz-acl=public-read";
	}

	String toStringForLog() {
		return serverAdress;
	}
}
