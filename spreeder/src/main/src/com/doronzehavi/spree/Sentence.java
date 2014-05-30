package com.doronzehavi.spree;

public class Sentence {
	protected int mStart; // index of first word in sentence
	protected int mEnd; // index of last word in sentence
	protected String mData;
	
	public String getData() {
		return mData;
	}

	public void setData(String data) {
		this.mData = data;
	}

	public Sentence(String data, int start, int end){
		mData = data;
		mStart = start;
		mEnd = end;
	}

	public int getStart() {
		return mStart;
	}

	public void setStart(int start) {
		mStart = start;
	}

	public int getEnd() {
		return mEnd;
	}

	public void setEnd(int end) {
		mEnd = end;
	}
}
