package com.darrenmowat.imageloader.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.co.senab.bitmapcache.CacheableBitmapWrapper;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.darrenmowat.imageloader.library.events.ImageAvailableEvent;
import com.darrenmowat.imageloader.library.events.LoadImageEvent;
import com.darrenmowat.imageloader.library.events.PurgeImageViewEvent;
import com.squareup.otto.Subscribe;

public class ImageLoader {

	private String Tag = "ImageLoader";

	private static final ImageLoader INSTANCE = new ImageLoader();

	private ExecutorService storageThreadPool;
	private ExecutorService networkThreadPool;
	private int screenWidth = 500;

	private HashSet<String> activeDownloads;
	private ConcurrentHashMap<Integer, String> waitingViews;

	public ImageLoader() {
		ImageBus.getInstance().register(this);
		int d_threads = 2;
		switch (Runtime.getRuntime().availableProcessors()) {
		case 1:
			d_threads = 2;
			break;
		case 2:
		case 3:
			d_threads = 3;
			break;
		case 4:
			d_threads = 4;
			break;
		default:
			d_threads = 3;
			break;
		}
		networkThreadPool = Executors.newFixedThreadPool(d_threads);
		storageThreadPool = Executors.newFixedThreadPool(d_threads);
		activeDownloads = new HashSet<String>();
		waitingViews = new ConcurrentHashMap<Integer, String>();
	}

	public static ImageLoader getInstance() {
		return INSTANCE;
	}

	@SuppressWarnings("deprecation")
	public void setup(Context context, int loadingDrawableId, int loadFailedDrawableId) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		screenWidth = display.getWidth(); // Deprecated, Meh!
		NetworkImageView.setLoadingDrawableId(loadingDrawableId);
		NetworkImageView.setLoadingFailedDrawableId(loadFailedDrawableId);
	}

	@Subscribe
	public void onLoadImage(LoadImageEvent event) {
		if (!activeDownloads.contains(event.url)) {
			addToActiveDownloads(event.url);
			addToWaitingViews(event);
			storageThreadPool.execute(new StorageRunnable(event));
		} else {
			// This image is already being downloaded
			// Add the waiting image view
			addToWaitingViews(event);
		}
	}

	@Subscribe
	public void onPurgeImageView(PurgeImageViewEvent event) {
		synchronized (waitingViews) {
			String url = waitingViews.remove(event.viewid);
			if (!isViewWaitingOnUrl(url)) {
				// No other views were waiting on the url
				// Remove it from activeDownloads if it is there
				removeFromActiveDownloads(url);
			}
		}
	}

	public void cleanCache(Context context) {
		storageThreadPool.execute(new CacheCleanRunnable(context, false));
	}

	public void clearCache(Context context) {
		storageThreadPool.execute(new CacheCleanRunnable(context, true));
	}

	private class StorageRunnable implements Runnable {

		private LoadImageEvent event;

		public StorageRunnable(LoadImageEvent event) {
			this.event = event;
		}

		@Override
		public void run() {
			// Check in memory cache one more time
			if (!isViewWaitingOnUrl(event.url)) {
				removeFromActiveDownloads(event.url);
				return;
			}
			CacheableBitmapWrapper bitmap = event.cache.get(event.url);
			if (bitmap != null && bitmap.hasValidBitmap()) {
				postImageAvailable(bitmap, event);
				removeFromActiveDownloads(event.url);
				removeFromWaitingViews(event);
				return;
			}
			bitmap = loadImageFromDisk(event);
			if (bitmap != null && bitmap.hasValidBitmap()) {
				postImageAvailable(bitmap, event);
				removeFromActiveDownloads(event.url);
				removeFromWaitingViews(event);
				return;
			}
			networkThreadPool.execute(new NetworkRunnable(event));
		}

	}

	private class NetworkRunnable implements Runnable {

		private LoadImageEvent event;

		public NetworkRunnable(LoadImageEvent event) {
			this.event = event;
		}

		@Override
		public void run() {
			try {
				// Sleep for a little bit before accessing the network
				// Stop unneeded network requests if the user is flinging
				// through a list
				Thread.sleep(150);
			} catch (InterruptedException e1) {

			}
			// Check in memory cache one more time
			if (!isViewWaitingOnUrl(event.url)) {
				removeFromActiveDownloads(event.url);
				return;
			}
			CacheableBitmapWrapper bitmap = event.cache.get(event.url);
			if (bitmap != null && bitmap.hasValidBitmap()) {
				postImageAvailable(bitmap, event);
				removeFromActiveDownloads(event.url);
				removeFromWaitingViews(event);
				return;
			}
			try {
				if (downloadImageToDisk(event)) {
					bitmap = loadImageFromDisk(event);
					if (bitmap != null && bitmap.hasValidBitmap()) {
						postImageAvailable(bitmap, event);
						removeFromActiveDownloads(event.url);
						removeFromWaitingViews(event);
						return;
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ImageBus.getInstance().post(new ImageAvailableEvent(null, event.url));
			removeFromActiveDownloads(event.url);
			// It's still waiting though?
			removeFromWaitingViews(event);
		}

	}

	private class CacheCleanRunnable implements Runnable {

		private boolean shouldClear;
		private Context context;

		public CacheCleanRunnable(Context context, boolean shouldClear) {
			Log.v(Tag, "Created a CacheCleanRunnable");
			this.shouldClear = shouldClear;
			this.context = context;
		}

		@Override
		public void run() {
			Log.v(Tag, "Running a CacheCleanRunnable");
			try {
				File cache;
				if (hasWritableExternalStorage()) {
					// Attempt to read the image from external storage
					cache = context.getExternalCacheDir();
				} else {
					// Attempt to read the image from internal storage
					cache = context.getCacheDir();
				}
				File[] files = cache.listFiles();
				if (files == null) {
					return;
				}
				if (shouldClear) {
					for (File f : files) {
						if (f != null) {
							if (f.delete()) {
								Log.v(Tag, "Deleted " + f.getAbsolutePath());
							}
						}
					}
				} else {
					Calendar delete = Calendar.getInstance();
					delete.add(Calendar.DAY_OF_MONTH, -2);
					long del = delete.getTimeInMillis();
					for (File f : files) {
						if (f != null) {
							Date lmod = new Date(f.lastModified());
							if (del > lmod.getTime()) {
								if (f.delete()) {
									Log.v(Tag, "Deleted " + f.getAbsolutePath());
								}
							}
						}
					}
				}
			} catch (Exception e) {
				// Don't really care if this fails
				Log.v(Tag, "Couldn't run CacheCleanRunnable", e);
			}
		}

	}

	// Methods that help to manage ImageView states

	private void addToActiveDownloads(String url) {
		synchronized (activeDownloads) {
			activeDownloads.add(url);
		}
	}

	private void removeFromActiveDownloads(String url) {
		synchronized (activeDownloads) {
			if (activeDownloads.contains(url)) {
				activeDownloads.remove(url);
			}
		}
	}

	private void addToWaitingViews(LoadImageEvent event) {
		synchronized (waitingViews) {
			waitingViews.put(event.viewid, event.url);
		}
	}

	private void removeFromWaitingViews(LoadImageEvent event) {
		synchronized (waitingViews) {
			// Remove any view waiting on event.url
			Set<Integer> ids = waitingViews.keySet();
			Iterator<Integer> keyIt = ids.iterator();
			while (keyIt.hasNext()) {
				int n = keyIt.next();
				if (event.url.equals(waitingViews.get(n))) {
					waitingViews.remove(n);
				}
			}
		}
	}

	private void postImageAvailable(CacheableBitmapWrapper bitmap, LoadImageEvent event) {
		ImageBus.getInstance().post(new ImageAvailableEvent(bitmap, event.url));
	}

	private boolean isViewWaitingOnUrl(String url) {
		if (url == null) {
			return false;
		}
		synchronized (waitingViews) {
			// Test to see if any view is waiting on event.url
			for (Integer view : waitingViews.keySet()) {
				if (url.equals(waitingViews.get(view))) {
					return true;
				}
			}
		}
		return false;
	}

	private CacheableBitmapWrapper loadImageFromDisk(LoadImageEvent event) {
		File file;
		if (hasReadableExternalStorage()) {
			// Attempt to read the image from external storage
			file = new File(event.context.getExternalCacheDir(), getFileName(event.url));
		} else {
			// Attempt to read the image from internal storage
			file = new File(event.context.getCacheDir(), getFileName(event.url));
		}
		if (file.exists()) {
			try {
				Bitmap bitmap = loadDrawableFromFile(file, event);
				if (bitmap != null) {
					return new CacheableBitmapWrapper(event.url, bitmap);
				}
			} catch (IOException e) {
				// Error with file on the disk, delete it & return null
				file.delete();
			}
		}
		return null;
	}

	private boolean downloadImageToDisk(LoadImageEvent event) throws MalformedURLException,
			IOException, OutOfMemoryError {
		File file;
		if (hasWritableExternalStorage()) {
			// Attempt to write the image to external storage
			event.context.getExternalCacheDir().mkdirs();
			file = new File(event.context.getExternalCacheDir(), getFileName(event.url));
		} else {
			// Attempt to write the image to internal storage
			event.context.getCacheDir().mkdirs();
			file = new File(event.context.getCacheDir(), getFileName(event.url));
		}
		// Now save the bitmap to file
		// Log.v(Tag, "Save to: " + file.getAbsolutePath());
		HttpURLConnection conn = null;
		InputStream is = null;
		try {
			conn = (HttpURLConnection) new URL(event.url).openConnection();
			is = new FlushedInputStream(conn.getInputStream());
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inTempStorage = new byte[16 * 1024];
			Bitmap bitmap;
			try {
				bitmap = BitmapFactory.decodeStream(is, null, options);
			} catch (OutOfMemoryError e) {
				event.cache.trimMemory();
				bitmap = BitmapFactory.decodeStream(is, null, options);
			}
			// Write the bitmap to disk
			if (bitmap != null) {
				FileOutputStream out = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
				out.close();
				// This bitmap is temporary - recycle this
				bitmap.recycle();
			}
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (conn != null) {
					conn.disconnect();
				}
			} catch (Exception e) {
				// Close Quietly
			}
		}
		return true;
	}

	private String getFileName(String url) {
		if (url == null) {
			return null;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(url.getBytes());
			byte[] a = digest.digest();
			int len = a.length;
			StringBuilder sb = new StringBuilder(len << 1);
			for (int i = 0; i < len; i++) {
				sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
				sb.append(Character.forDigit(a[i] & 0x0f, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			return String.valueOf(url.hashCode());
		}
	}

	private boolean hasReadableExternalStorage() {
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED)
				|| state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
	}

	private boolean hasWritableExternalStorage() {
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED);
	}

	private Bitmap loadDrawableFromFile(File f, LoadImageEvent event) throws IOException {
		FileInputStream stream = new FileInputStream(f);
		Bitmap bitmap = null;
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(stream, null, o);
		stream.close();
		stream = null;
		stream = new FileInputStream(f);
		int scale = 1;
		if (o.outWidth > screenWidth) {
			scale = (int) Math.pow(
					2,
					(int) Math.round(Math.log(screenWidth
							/ (double) Math.max(o.outHeight, o.outWidth))
							/ Math.log(0.5)));
		}
		// Decode with inSampleSize to reduce memory used
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		o2.inPreferredConfig = Bitmap.Config.ARGB_8888;
		o2.inTempStorage = new byte[16 * 1024];
		try {
			bitmap = BitmapFactory.decodeStream(stream, null, o2);
			f.setLastModified(System.currentTimeMillis());
		} catch (OutOfMemoryError e) {
			event.cache.trimMemory();
			try {
				bitmap = BitmapFactory.decodeStream(stream, null, o2);
			} catch (OutOfMemoryError failed) {
				return null;
			}
		} finally {
			stream.close();
		}
		return bitmap;
	}
}
