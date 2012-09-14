package com.darrenmowat.imageloader.library;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapWrapper;
import uk.co.senab.bitmapcache.CacheableImageView;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;

import com.darrenmowat.imageloader.library.events.ImageAvailableEvent;
import com.darrenmowat.imageloader.library.events.LoadImageEvent;
import com.darrenmowat.imageloader.library.events.PurgeImageViewEvent;
import com.squareup.otto.Subscribe;

public class NetworkImageView extends CacheableImageView {

	private static int loadingDrawableId;
	private static int loadingFailedDrawableId;

	private String url;
	private Activity activity;
	private BitmapLruCache mCache;

	private boolean registered = false;

	public NetworkImageView(Context context) {
		super(context);
		register();
	}

	public NetworkImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		register();
	}

	public static void setLoadingDrawableId(int id) {
		loadingDrawableId = id;
	}

	public static void setLoadingFailedDrawableId(int id) {
		loadingFailedDrawableId = id;
	}

	public void register() {
		if (!registered) {
			ImageBus.getInstance().register(this);
			registered = true;
		}
	}

	public void unregister() {
		if (registered) {
			ImageBus.getInstance().post(new PurgeImageViewEvent(getId()));
			ImageBus.getInstance().unregister(this);
			registered = false;
		}
	}

	// Ensure that we are only registered to the ImageLoader bus if the
	// ImageView is attached to a window

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		register();
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		unregister();
	}

	public void setImageUrl(String url, Activity activity) {

		this.url = url;
		this.activity = activity;
		this.mCache = BitmapCache.getBitmapCache(activity);

		if (url == null) {
			throw new NullPointerException("Url passed in was null!");
		}

		setTag(url);

		CacheableBitmapWrapper wrapper = mCache.get(url);

		if (null != wrapper && wrapper.hasValidBitmap()) {
			// The cache has it, so just display it
			setImageCachedBitmap(wrapper);
		} else {
			setImageResource(loadingDrawableId);
			ImageBus.getInstance().post(new LoadImageEvent(url, mCache, hashCode(), activity));
		}
	}

	@Subscribe
	public void onImageAvailable(final ImageAvailableEvent event) {
		if (url != null && url.equals(event.url)) {
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (event.image != null && event.image.hasValidBitmap()) {
						setImageCachedBitmap(event.image);
					} else {
						setImageResource(loadingFailedDrawableId);
					}
				}

			});
		}
	}

}
