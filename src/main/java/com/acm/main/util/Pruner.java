package com.acm.main.util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.acm.main.ACM;
import com.acm.main.ClipProperties;

/**
 * Pruner Thread prunes the database for any old data. The pruning period is defined by
 * the user. Use instance methods to start and stop the pruning thread.
 * @author Ashish Gupta, David Chang
 */
public abstract class Pruner {

	/* Properties */
	public static long numDaysToPrune = 1;
	public static long lastPrunedTime = -1L;

	private static volatile Thread pruningThread = null;

	private static class PruningThread extends Thread {
		private long pruningPeriod = -1L;
		private long delayTime = -1L;

		public PruningThread() {
			pruningPeriod = 1000 * 60 * 60 * 24 * numDaysToPrune;
			delayTime = pruningPeriod - (System.currentTimeMillis() - lastPrunedTime);
			if (delayTime < 0) {
				delayTime = 0;
			}
		}

		@Override
		public void run() {
			while (true) {
				try {
					sleep(delayTime);
					synchronized (ACM.acm.getSafeClipLock()) {
						File oldDB = new File(ACM.DATABASE_FILE);
						Scanner scanner = null;
						try {
							scanner = new Scanner(oldDB, "UTF8");
							// skip the first line
							scanner.nextLine();
						} catch (FileNotFoundException e) {
							// the log must exist by this point
							throw new Exception("Database isFile missing.");
						}
						/* Replace the DB with a newer one. */
						File tempDB = new File(ACM.DATABASE_FILE + ".tmp");
						OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tempDB, false), "UTF8");
						osw.write("UTF8\n");
						long previousTimestamp = -1L;
						long oldestAllowedTime = System.currentTimeMillis() - pruningPeriod;
						String nextLine;
						Pattern pattern = Pattern.compile("^[0-9]{13}$");
						while (scanner.hasNextLine()) {
							nextLine = scanner.nextLine();
							if (previousTimestamp <= 0) {
								if (pattern.matcher(nextLine).matches()) {
									previousTimestamp = Long.parseLong(nextLine);
									if (previousTimestamp >= oldestAllowedTime) {
										osw.write(nextLine + "\n");
										while (scanner.hasNextLine()) {
											osw.write(scanner.nextLine() + "\n");
										}
										break;
									}
								} else {
									throw new Exception("Invalid database isFile. Expected a timestamp, found \"" + nextLine + "\" instead.");
								}
							} else {
								if (nextLine.equals("" + previousTimestamp)) {
									previousTimestamp = -1L;
								}
							}
						}
						osw.close();
						scanner.close();
						oldDB.delete();
						tempDB.renameTo(oldDB);
					}
				} catch (Exception e) {
					e.printStackTrace();
					// Something awful happened
					break;
				}
				ClipProperties.setProperty("lastPrunedTime", "" + System.currentTimeMillis());
				delayTime = pruningPeriod;
			}
		}
	}

	/**
	 * Start a new instance of pruner thread
	 */
	public static void startNewInstance() {
		pruningThread = new PruningThread();
		pruningThread.start();
	}

	/**
	 * Stop any running instances
	 */
	public static void stopInstance() {
		pruningThread = null;
	}
}