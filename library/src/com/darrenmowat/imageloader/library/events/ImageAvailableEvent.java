package com.darrenmowat.imageloader.library.events;

import uk.co.senab.bitmapcache.CacheableBitmapWrapper;

public class ImageAvailableEvent {

	public final CacheableBitmapWrapper image;
	public final String url;

	public ImageAvailableEvent(CacheableBitmapWrapper image, String url) {
		this.image = image;
		this.url = url;
	}

}
