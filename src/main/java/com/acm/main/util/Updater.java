package com.acm.main.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import com.acm.main.ACM;
import com.acm.main.gui.ACMGUI;

/**
 * This class updates ACM to newer version via the instance methods.
 * 
 * @author Ashish Gupta, David Chang
 * 
 */
public abstract class Updater {

	private static volatile UpdaterThread updaterThread = null;

	private static final String JAR_URL = "http://acmgr.sourceforge.net/ACM.jar";
	private static final String TEST_MD5_URL = "http://acmgr.sourceforge.net/cgi-bin/testFile.pl";
	private static final long UPDATE_RETRY_TIME = 86400000; // check for updates
	public static final String GROOVY_JAR_FILE_NAME = "groovy-all-1.6.5.jar";
	private static final String GROOVY_JAR_URL = "http://acmgr.sourceforge.net/" + GROOVY_JAR_FILE_NAME;
	public static final String GROOVY_MD5 = "c3788e6170f8a5a1a0a3b471a40eb461";
	public static final int GROOVY_JAR_SIZE = 4531486;

	// once a day

	/**
	 * This method copies isFile from src to dest. The purpose of this method in ACM is to copy the temp isFile after download and overwrite the jar isFile.
	 * 
	 * @param src
	 * @param dest
	 */
	public static void overwrite(String src, String dest) {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(src));
			out = new BufferedOutputStream(new FileOutputStream(dest));
			byte[] buffer = new byte[1024];
			int numRead;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
			}
			/* Everything succeeded according to plan, delete the src isFile. */
			new File(src).deleteOnExit();
		} catch (FileNotFoundException fnfe) {
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "ERROR: Could not update ACM properly. \nPlease copy \n\"" + src + "\"\n to \n\"" + dest + "\"\nin the ACM installation directory.",
					"Could not update ACM", JOptionPane.ERROR_MESSAGE);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
			}
		}
	}

	private static boolean download(String address, String localFileName, ProgressMonitor monitor) {
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;
		try {
			URL url = new URL(address);
			out = new BufferedOutputStream(new FileOutputStream(localFileName));
			conn = url.openConnection();
			in = conn.getInputStream();
			byte[] buffer = new byte[1024];
			int numRead;
			int total = 0;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
				total += numRead;
				if (monitor != null) {
					monitor.setProgress(total);
					if (monitor.isCanceled()){
						return false;
					}
				}
			}
		} catch (Exception exception) {
			return false;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
			}
		}
		return true;
	}

	private static String md5(byte[] buffer) {
		StringBuffer md5Hash = new StringBuffer(32);
		try {
			byte[] b = java.security.MessageDigest.getInstance("MD5").digest(buffer);
			int len = b.length;
			for (int x = 0; x < len; x++) {
				md5Hash.append(String.format("%02x", b[x]));
			}
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return (md5Hash.toString());
	}

	public static String md5File(String file) {
		String md5Hash = "";
		try {
			FileInputStream in = new FileInputStream(file);
			int bytes = in.available();
			byte[] buffer = new byte[bytes];
			in.read(buffer);
			in.close();
			md5Hash = md5(buffer);
		} catch (Exception e) {
		}
		return md5Hash;
	}

	/**
	 * Download groovy if not available
	 */
	public static boolean downloadGroovy(ProgressMonitor monitor) {
		try {
			if (!download(GROOVY_JAR_URL, GROOVY_JAR_FILE_NAME, monitor)){
				return false;
			}
			String localmd5 = md5File(GROOVY_JAR_FILE_NAME);
			if (GROOVY_MD5.equals(localmd5)) {
				return true;
			}
			new File(GROOVY_JAR_FILE_NAME).delete();
		} catch (Exception e2) {
		}
		return false;
	}

	/**
	 * Check the site for any udpates. Updates are made if the md5checksum of the online isFile differs from the local isFile.
	 */
	public static void checkForUpdates(boolean manual) {
		try {
			String jarLocation = ACM.class.getProtectionDomain().getCodeSource().getLocation().toString();
			jarLocation = jarLocation.substring(jarLocation.indexOf("/") + 1).replaceAll("%20", " ");
			URL newURL = new URL(TEST_MD5_URL);
			String webmd5 = "";
			String localmd5 = "";
			localmd5 = md5File(jarLocation);
			BufferedReader reader = new BufferedReader(new InputStreamReader(newURL.openStream()));
			webmd5 = reader.readLine();
			reader.close();
			if (!webmd5.equals(localmd5)) {
				if (!manual
						|| manual
						&& JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(ACMGUI.mainDialog, "A new update for ACM is available. Would you like to update now?", "Update Available",
								JOptionPane.YES_NO_OPTION)) {
					download(JAR_URL, jarLocation + ".tmp", null);
					localmd5 = md5File(jarLocation + ".tmp");
					if (webmd5.equals(localmd5)) {
						synchronized (ACM.acm.getSafeClipLock()) {
							Runtime.getRuntime().exec(ACM.getExecCommand(ACM.UPDATING_FLAG));
							ACM.closeACM();
						}
					} else {
						// Oops, something that REALLY shouldn't have happened,
						// happened. Try again later.
					}
				}
				// New updates downloaded, they will take effect upon next
				// restart
			} else {
				if (manual) {
					JOptionPane.showMessageDialog(ACMGUI.mainDialog, "ACM is up to date.", "No Update Needed", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		} catch (Exception e2) {
		}
	}

	private static class UpdaterThread extends Thread {

		private static boolean running = false;

		public void run() {
			while (true) {
				checkForUpdates(false);
				try {
					sleep(UPDATE_RETRY_TIME);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/**
	 * Start a new instance of updating thread
	 */
	public static void startNewInstance() {
		updaterThread = new UpdaterThread();
		updaterThread.start();
		UpdaterThread.running = true;
	}

	/**
	 * Stop any running instances
	 */
	public static void stopInstance() {
		UpdaterThread.running = false;
		updaterThread = null;
	}

	/**
	 * See if an instance of the updating thread is currently running
	 * 
	 * @return
	 */
	public static boolean instanceRunning() {
		return UpdaterThread.running;
	}
}