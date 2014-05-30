package com.doronzehavi.spree;

public class Word extends Sentence {
	private int mLength;
	private int mSentenceIndex;
	private int mIndex;

	public Word(String data, int start, int end, int length, int sentenceIndex, int index) {
		super(data, start, end);
		// TODO Auto-generated constructor stub
		mData = data;
		mStart = start;
		mEnd = end;
		mLength = length;
		mIndex = index;
	}

	public int getIndex() {
		return mIndex;
	}

	public void setIndex(int index) {
		mIndex = index;
	}

	public int getSentenceIndex() {
		return mSentenceIndex;
	}

	public void setSentenceIndex(int sentenceIndex) {
		mSentenceIndex = sentenceIndex;
	}

	public int getLength() {
		return mLength;
	}

	public void setLength(int length) {
		mLength = length;
	}

}