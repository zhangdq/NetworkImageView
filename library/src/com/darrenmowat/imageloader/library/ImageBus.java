package com.darrenmowat.imageloader.library;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Message Bus used by NetworkImageView & ImageLoader to communicate
 * @author Darren Mowat
 *
 */
public class ImageBus {

	private static Bus BUS = new Bus(ThreadEnforcer.ANY);

	public static Bus getInstance() {
		return BUS;
	}

	private ImageBus() {

	}
}
