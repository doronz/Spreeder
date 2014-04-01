package com.doronzehavi.spree;
import android.os.Parcel;
import android.os.Parcelable;
import nl.siegmann.epublib.domain.Book;

public class MyBook extends Book implements Parcelable {

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}

}
