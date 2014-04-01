package com.doronzehavi.spree;

public class Material {
	private int mProgress;
	private long mId;
	
	private String mData;
	private String mSummary;

	Material () {
		
	}
	
//	Material (int progress, int Id, String data, String summary) {
//		mProgress = progress;
//		mId = Id;
//		mData = data;
//		mSummary = summary;
//	}
	
	
	public long getId() {
		return mId;
	}

	public void setId(long iD) {
		mId = iD;
	}

	public int getProgress() {
		return mProgress;
	}

	public void setProgress(int progress) {
		mProgress = progress;
	}

	public String getData() {
		return mData;
	}

	public void setData(String data) {
		mData = data;
	}

	public String getSummary() {
		return mSummary;
	}

	public void setSummary(String summary) {
		mSummary = summary;
	}

}
