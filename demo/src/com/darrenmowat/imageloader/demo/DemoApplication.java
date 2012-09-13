package com.darrenmowat.imageloader.demo;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.darrenmowat.imageloader.library.BitmapCache;
import com.darrenmowat.imageloader.library.ImageLoader;
import com.darrenmowat.imageloadersample.R;

public class DemoApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectAll().penaltyLog().build());

		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
				.penaltyLog().penaltyDeath().build());

		ImageLoader.getInstance().setup(getApplicationContext(),
				R.drawable.ic_launcher, R.drawable.ic_action_search);

	}

	@Override
	public void onLowMemory() {
		BitmapCache.getBitmapCache(this).trimMemory();
		super.onLowMemory();
	}

	public static DemoApplication getApplication(Context context) {
		return (DemoApplication) context.getApplicationContext();
	}
}
