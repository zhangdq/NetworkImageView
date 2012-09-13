NetworkImageView
================

A simple ImageView & Image Loader for Android which user Thread Pools to load files from the filesystem & download them from the Internet.

NetworkImageView uses a 3 tier cache to cache images on the device.

	1. Memory
	2. External Storage
	3. Internal Storage - Only if External Storage is unavailable 
	
NetworkImageView uses [Android-BitmapMemoryCache][2] internally to cache bitmaps in memory. Android-BitmapMemoryCache ensures that only bitmaps referenced by a view are stored in ram. When they aren't referenced by any views the bitmap is recycled. This is great as it helps to greatly reduce the number of OOM Exceptions in bitmap heavy applications.


Usage
-----

Download the NetworkImageView [jar][4] and add it to your libs folder. 
NetworkImageView depends on Otto by Square, a message bus which allows diffrent parts of NetworkImageView to communicate. Download the Otto [jar][5] and add it to your libs folder. 

First set the default loading & failed drawables that ImageLoader should use. This should be done once when your application starts (i.e. In your Application class):

Setup:

	ImageLoader.getInstance().setup(context,
				R.drawable.loading_drawable, R.drawable.failed_drawable);


Load/Download Image:

	 // Create a new NetworkImageView
	 NetworkImageView iv = new NetworkImageView(context);
	 // Set the image
	 // A reference to the host activity must be passed in 
	 // so we can set the image later on using runOnUiThread
	 iv.setImageUrl(url, activity);
	 
	 // Or
	 
	 // Grab a NetworkImageView from a layout
	 NetworkImageView iv = (NetworkImageView) findViewById(R.id.commentListSmallImage);
	 iv.setImageUrl(url, activity);

	
	
The image will be downloaded, saved to the disk & resized to best support the users screen size. 
	
Android Image Loader also has methods to clean out old files from the on disk image cache. You can delete images that haven't been loaded from the disk for 2 days by:

	ImageLoader.getInstance().cleanImageCache(context);
	
Or you can delete every cached image using:

	ImageLoader.getInstance().clearImageCache(context);

These methods run on a background thread so they can be called from the UI thread.

Advanced Usage
--------------

NetworkImageView contains an abstract class called CallbackNetworkImageView. This class can be used by to get a callback when the image has been loaded/downloaded. To use it extends the class and implement the abstract method
	
	public abstract void imageAvailable(String url, Bitmap bitmap);
	
This can be useful for downloading image for RemoteViews. For example in a personal project I use this to download a profile picture which is used in a Notification. I call 

	CallbackNetworkImageView.setImageUrl(profileImageUrl);

and then run the rest of my notification code when imageAvailable is called. 

Note: The bitmap returned to you here will be a copy of any existing Bitmap held in the cache. As NetworkImageView & Android-BitmapMemoryCache deal with recycling images in the background your reference to the bitmap could randomly be recycled. Therefore a copy is returned. 
This method will also be called from a background thread and not from the UI thread so be careful!


Demo
-----
You can download a demo apk [here][6].

Licence
-----

The code in this project is licensed under the Apache Software License 2.0.

Contributing
------------

I'm open to any suggestions and pull requests. 


[1]: https://github.com/DarrenMowat/NetworkImageView/issues
[2]: https://github.com/chrisbanes/Android-BitmapMemoryCache
[3]: https://github.com/chrisbanes
[4]: https://github.com/downloads/DarrenMowat/NetworkImageView/networkimageview.jar
[5]: http://square.github.com/otto/
[6]: https://github.com/downloads/DarrenMowat/NetworkImageView/NetworkImageViewSample.apk