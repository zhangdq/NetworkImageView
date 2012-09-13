package com.darrenmowat.imageloader.library.events;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.content.Context;

public class LoadImageEvent {

	public final String url;
	public final BitmapLruCache cache;
	public final int viewid;
	public final Context context;

	public LoadImageEvent(String url, BitmapLruCache cache, int viewid, Context context) {
		this.url = url;
		this.cache = cache;
		this.viewid = viewid;
		this.context = context;
	}

}
