package com.pasdam.universalsearch;

import java.net.URI;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents a single item in the search results
 * 
 * @author paco
 * @version 0.1
 */
public class ResultItem implements Parcelable {

	/**
	 * Result's page title
	 */
	public String title;

	/**
	 * Result's description
	 */
	public String snippet;

	/**
	 * Result's uri
	 */
	public URI uri;

	/**
	 * CREATOR is an object implementing the Parcelable.Creator interface
	 */
	public static final Parcelable.Creator<ResultItem> CREATOR = new Parcelable.Creator<ResultItem>() {
		public ResultItem createFromParcel(Parcel in) {
			return new ResultItem(in);
		}

		public ResultItem[] newArray(int size) {
			return new ResultItem[size];
		}
	};

	/**
	 * Empty constructor
	 */
	public ResultItem() {
	}

	/**
	 * Construct a {@link ResultItem} starting from a serialized object
	 * @param in the serialized object
	 */
	public ResultItem(Parcel in) {
		readFromParcel(in);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.title);
		dest.writeString(this.snippet);
		dest.writeString(this.uri.toString());
	}

	/**
	 * It reads a serialized {@link ResultItem}
	 * @param in the serialized object
	 */
	public void readFromParcel(Parcel in) {
		this.title = in.readString();
		this.snippet = in.readString();
		this.uri = URI.create(in.readString());
	}
}
