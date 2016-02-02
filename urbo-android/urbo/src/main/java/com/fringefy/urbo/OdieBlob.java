package com.fringefy.urbo;

import android.util.Log;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.io.IOException;

class OdieBlob {

	private static final String TAG = "OdieBlob";

// Members

	private final OkHttpClient okHttpClient;

	private String bucketName = "fringefyimages";
	private String folder = "d/";
	String serverAdress;

// Construction

	OdieBlob() {
		okHttpClient = new OkHttpClient();
		setBucket(bucketName, folder);
	}

	/**
	 * Synchronous Get method to upload an image to the Odie server.
	 * @param imgName The image name to create
	 * @param imgBuf  The bytes to upload
	 * @return return true if succeeded to upload
	 */
	synchronized boolean uploadImage(String imgName, byte[] imgBuf) {
		if (imgName == null || imgBuf == null) {
			return false;
		}
		try {
			Log.d(TAG, "Uploading " + String.format("%.1fKB", imgBuf.length/1024.) + " to " +
					String.format(serverAdress, imgName));
			uploadInternal(imgName, imgBuf);
			return true;
		}
		catch (Exception e) {
			Log.e(TAG, "Failed to upload " + imgName +
                    " to " + toStringForLog(), e);
		}
		return false;
	}

	private void uploadInternal(String imgName, byte[] imgBuf) throws IOException {

		Request request = new Request.Builder()
				.header("Content-Type", "image/jpg")
				.url(String.format(serverAdress, imgName))
				.put(RequestBody.create(MediaType.parse("image/jpg"), imgBuf))
				.build();

		Response response = okHttpClient.newCall(request).execute();

		if (!response.isSuccessful()){
            throw new IOException("Unexpected code " + response);
        }

        Headers responseHeaders = response.headers();
        for (int i = 0; i < responseHeaders.size(); i++) {
           Log.d(TAG, responseHeaders.name(i) + ": " + responseHeaders.value(i));
        }
        Log.d(TAG, response.body().string());
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
				"%s?Content-Type=image/jpg&Expires=1437757726&x-amz-acl=public-read";
	}

	String toStringForLog() {
		return serverAdress;
	}
}
