package com.darrenmowat.imageloader.library;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapWrapper;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.darrenmowat.imageloader.library.events.ImageAvailableEvent;
import com.darrenmowat.imageloader.library.events.LoadImageEvent;
import com.darrenmowat.imageloader.library.events.PurgeImageViewEvent;
import com.squareup.otto.Subscribe;


public abstract class CallbackNetworkImageView {

	private String url;
	private Context context;
	private BitmapLruCache mCache;

	private boolean registered = false;

	public CallbackNetworkImageView(Context context) {
		this.context = context;
		if (context == null) {
			throw new IllegalArgumentException("Context passed in was null!");
		}
		mCache =BitmapCache.getBitmapCache(context);
	}
	
	public Context getContext() {
		return context;
	}

	private void register() {
		if (!registered) {
			ImageBus.getInstance().register(this);
			registered = true;
		}
	}

	private void unregister() {
		if (registered) {
			ImageBus.getInstance().unregister(this);
			registered = false;
		}
	}

	public Bitmap hasImage(String url) {
		this.url = url;
		CacheableBitmapWrapper wrapper = mCache.get(url);
		if (null != wrapper && wrapper.hasValidBitmap()) {
			// Return a copy of this bitmap
			return Bitmap.createBitmap(wrapper.getBitmap());
		}
		return null;
	}

	public void setImageUrl(String url) {

		this.url = url;

		if (url == null) {
			throw new IllegalArgumentException("Url passed in was null!");
		}

		// Check the cache
		CacheableBitmapWrapper wrapper = mCache.get(url);
		if (null != wrapper && wrapper.hasValidBitmap()) {
			// Return a copy of this bitmap
			imageAvailable(url, Bitmap.createBitmap(wrapper.getBitmap()));
			return;
		}
		// Register with the ImageLoader
		register();
		ImageBus.getInstance().post(new LoadImageEvent(url, mCache, hashCode(), context));
	}

	@Subscribe
	public void onImageAvailable(final ImageAvailableEvent event) {
		if(ImageLoader.DEBUG)Log.v("CallbackNetworkImageView", "onImageAvailable: " + event.url + " " + event.image);
		if (url != null && url.equals(event.url)) {
			if (event.image != null && event.image.hasValidBitmap()) {
				Bitmap copy = Bitmap.createBitmap(event.image.getBitmap());
				imageAvailable(event.url, copy);
			} else {
				imageAvailable(url, null);
			}
			unregister();
		}
	}

	public abstract void imageAvailable(String url, Bitmap bitmap);

}
