package com.fringefy.urbo;

import java.io.File;
import java.io.FilenameFilter;

import android.os.SystemClock;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

//TODO: [SY] Implement for Azure Blob Storage
//TODO: [AC] Review, remove unused code
class OdieBlob {

	private static final String TAG = "OdieS3";
	private static final int REPEAT_DELAY = 30000;
	private static final long LOCK_TIMEOUT = 20000;

// Members

	private final AmazonS3Client s3Client;
	private final File imgDir;
	private final FilenameFilter filenameFilter = new FilenameFilter() {
		@Override public boolean accept(File dir, String filename) {
			return (filename.endsWith(".jpg"));
		}
	};

	private String bucketName;
	private String folder = "";


// Construction

	OdieBlob(String sAccessKey, String sSecretKey, File imgDir) {

		this.imgDir = imgDir;
		s3Client = new AmazonS3Client(
				sAccessKey == null ?
						new AnonymousAWSCredentials() :
						new BasicAWSCredentials(sAccessKey, sSecretKey));
	}

	synchronized boolean uploadImage(File fImg) {

		if (bucketName == null || bucketName.isEmpty()) {
			SystemClock.sleep(REPEAT_DELAY);
			return false;
		}

		File fLock = new File(fImg.getParentFile(), "S3.lock");

		try {
			if (System.currentTimeMillis() - fLock.lastModified() < LOCK_TIMEOUT) {
				Log.d(TAG, fImg.getPath() + " locked");
				if (fImg.exists()) {
					SystemClock.sleep(REPEAT_DELAY);
				}
				return false;
			}

            try {
				fLock.createNewFile();
				fLock.setLastModified(System.currentTimeMillis());
			}
			catch (Exception ex) {
				Log.w(TAG, "failed to lock " + fImg.getPath());
			}

            s3Client.putObject(
					new PutObjectRequest(bucketName, folder + fImg.getName(), fImg).
							withCannedAcl(CannedAccessControlList.PublicReadWrite));
			Log.d(TAG, "uploaded to " + toStringForLog() + fImg.getName());

			fImg.delete();
			fLock.delete();
			return true;
		}
		catch (AmazonClientException e) {
            Urbo.urbo.onError(TAG, "Failed to upload " + fImg.getPath() +
                    " to " + toStringForLog(), e);
		}

		if (fImg.exists()) {
			SystemClock.sleep(REPEAT_DELAY);
		}

		try {
			fLock.delete();
		}
		catch (Exception ex) {
			Log.w(TAG, "failed to unlock " + fImg.getPath());
		}
		return false;
	}

	synchronized File[] listOutstandingImages() {
		File[] lstImgFiles = imgDir.listFiles(filenameFilter);
		return lstImgFiles;
	}

	void setBucket(String s3Bucket, String s3Folder) {
		if (s3Bucket == null || s3Bucket.isEmpty()) {

		}
		else {
			bucketName = s3Bucket;
		}
		if (s3Folder == null || s3Folder.isEmpty()) {
			folder = "";
		}
		else {
			folder = s3Folder + "/";
		}
	}

	String toStringForLog() {
		return "http://" + bucketName + ".s3.amazonaws.com/" + folder;
	}
}
