package com.acm.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ClipProperties {

	public static final String PROPERTIES_FILE = "ACM.dat";

	static Properties properties = new Properties();

	public static boolean loadProperties() {
		File f = new File(PROPERTIES_FILE);
		try {
			if (!f.exists()) {
				f.createNewFile();
			}
			FileInputStream streamP = new FileInputStream(f);
			properties.load(streamP);
			streamP.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static boolean saveProperties() {
		try {
			FileOutputStream streamP = new FileOutputStream(new File(PROPERTIES_FILE));
			properties.store(streamP, "ACM Properties. DO NOT TOUCH THIS FILE!!! BAD THINGS WILL HAPPEN (though you're more than welcome to try!)");
			streamP.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static String getProperty(String name, String defaultValue) {
		String value = properties.getProperty(name);
		if (value == null) {
			value = defaultValue;
			setProperty(name, value);
		}
		return value;
	}

	public static boolean setProperty(String name, String value) {
		properties.put(name, value);
		new Thread() {
			@Override
			public void run() {
				saveProperties();
			}
		}.start();
		return true;
	}
}