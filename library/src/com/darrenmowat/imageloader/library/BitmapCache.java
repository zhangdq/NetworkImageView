package com.darrenmowat.imageloader.library;

import android.content.Context;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class BitmapCache {

	private static BitmapLruCache mCache;

	
	public static BitmapLruCache getBitmapCache(Context context) {
		if(mCache == null)
			mCache = new BitmapLruCache(context);
		return mCache;
	}
}
