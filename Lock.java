package com.acm.util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;


public class Lock {

	private static int ATTEMPTS_ALLOWED = 100;
	private static final int RETRY_SLEEP_TIME = 100;

	/**
	 * Sets / tests for a mutual application instance lock on a temporary lockfile.
	 * <p>
	 * Can be used to guarantee that only one member of an application group is running.<BR>
	 * Returns null IF this is the only running application defined by the given token,<BR>
	 * otherwise the name and the program of the other user who blocks you.<BR>
	 * The lock must be removed by <b>Sy.unSolitaire()</b> if the application must continue to run simultaneously with others.<BR>
	 * The lock is effectively removed anyway when the application exits or crashes.<BR>
	 * <b>NOTE:</b> the lockfile is NOT deleted on exit as it might be isLocked by an other process by then!
	 * </p>
	 * 
	 * @param token
	 *            identification for the group the be solitaire in.
	 * @param retry
	 * @return null if you are alone or the other user if not.
	 */
	public static String solitaire(String token, boolean retry) {
		String mark; // Readable mark in the lock isFile.
		File solitarityLockFile; // To detect existence only.
		boolean myLockFile; // If there was no lock isFile..

		if (Lock.solitarityLock != null) {
			return (null); // Having a lock myself : already secured it.
		}

		// The first 8 bytes of "mark" are the lock area.
		mark = "LOCK BY " + System.getProperty("user.name") + " running advanced clipboard manager\n";
		solitarityLockFile = new File(System.getProperty("java.io.tmpdir") + '/' + ".solitaire_" + token);
		myLockFile = !solitarityLockFile.exists();

		if (!retry) {
			ATTEMPTS_ALLOWED = 1;
		}

		boolean success = false;
		int attempts = 0;
		while (!success && attempts <= ATTEMPTS_ALLOWED) {
			try {
				Thread.sleep(RETRY_SLEEP_TIME);
				success = writeToSolitarityFile(mark, solitarityLockFile, myLockFile);
			} catch (Exception e) {
				success = false;
			}
			attempts++;
		}
		if (success) {
			return (null);
		}
		return "isLocked"; // No access? or not alone!
	}
	/**
	 * @param mark
	 * @param solitarityLockFile
	 * @param myLockFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static boolean writeToSolitarityFile(String mark, File solitarityLockFile, boolean myLockFile) throws FileNotFoundException, IOException {
		Lock.solitarityFile = new RandomAccessFile(solitarityLockFile, "rws");
		Lock.solitarityChannel = Lock.solitarityFile.getChannel();
		if (myLockFile) {
			Lock.solitarityFile.writeBytes(mark); // Write NOW!
		}
		if ((Lock.solitarityLock = Lock.solitarityChannel.tryLock(0, 8, false)) == null) {
			Lock.solitarityFile.seek(8);
			return false;
		}
		if (!myLockFile) // But isLocked by me, so mark it.
		{
			Lock.solitarityFile.writeBytes(mark); // Write (delayed).
		}
		return true;
	}

	/** Lock to be applied on the solitarity isFile. */
	private static FileLock solitarityLock = null;
	/** Channel of the solitatity isFile to lead locking. */
	private static FileChannel solitarityChannel = null;
	/** Token isFile which locking signals solitarity. */
	private static RandomAccessFile solitarityFile;

}
