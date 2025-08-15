package com.acm.main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import com.acm.clip.Clip;
import com.acm.clip.ClipBoardOperator;
import com.acm.clip.ClipItemListener;
import com.acm.clip.ClipSlots;
import com.acm.clip.ClipWriter;
import com.acm.keyboard.win.KeyboardHook;
import com.acm.main.gui.ACMClip;
import com.acm.main.gui.ACMClipSlots;
import com.acm.main.gui.ACMGUI;
import com.acm.main.gui.Splash;
import com.acm.main.gui.dialogs.ExtensionDialog;
import com.acm.main.gui.dialogs.HelpDialog;
import com.acm.main.keyboard.ACMKeyListener;
import com.acm.main.util.FilteredStreams;
import com.acm.main.util.Network;
import com.acm.main.util.Pruner;
import com.acm.main.util.Updater;
import com.acm.util.Lock;

/**
 * The Main class
 * 
 * @author Ashish Gupta, David Chang
 */
public final class ACM extends ClipBoardOperator {

	/* Current release information. */
	public static final String TITLE = "Advanced Clipboard Manager";
	public static final String VERSIONID = "4.1.1 Beta";
	public static final String RELEASEDATE = "12/08/2009";

	/* Flags */
	public static final String UPDATING_FLAG = "updating=true";
	public static final String UPDATED_FLAG = "updated=true";
	public static final String RETRYING_FLAG = "retry=true";
	public static final String DEVELOPING_FLAG = "developing=true";
	public static final String VERSION_FLAG = "version=";
	public static final String NETWORKING_FLAG = "networking=true";
	public static final String NETWORKING_IP_FLAG = "ip=";
	public static final String NETWORKING_PORT_FLAG = "port=";
	public static final String NETWORKING_PASSWORD_FLAG = "password=";

	public static final String COMMAND_TEXBOX_HISTORY_FILE = "history.dat";
	public static final String KEYBOARD_WIN_HOOK_DLL = "syshook.dll"; // Manually
	// add
	// res
	public static final String DATABASE_FILE = "ACM.db";
	public static final String NETWORK_HISTORY_FILE = "network.db";

	private static ArrayList<String> time = null;
	private static HashMap<String, String> preview = null;
	private static boolean haventRefreshed = true;

	/* This may come in handy one day. So we are going to keep it for now */
	@SuppressWarnings("unused")
	private static String previousVersion = VERSIONID;

	public static ACM acm = null;
	public static Splash splash = null;
	public static Network network = null;
	public static KeyboardHook kh = null;
	public static Macros macros = null;
	private static ProgressMonitor monitorNetwork;

	private static NextItem nextItem = new NextItem();

	/* Do not manually set any of these! */
	public static boolean DEVELOPING = false;
	public static boolean UPDATED = false;
	public static boolean running = false;

	public ACM(int size, ClipSlots clipSlots) throws Exception {
		super(size, clipSlots);
	}

	public ACM(ClipSlots clipSlots) {
		super(clipSlots);
	}

	@Override
	public ACMClipSlots getClips() {
		return (ACMClipSlots) super.getClips();
	}

	/**
	 * Main method
	 * 
	 * @param aArguments
	 */
	public static void main(String... aArguments) {
		PrintStream ps = null;
		FilteredStreams.FilteredInputStream is = new FilteredStreams.FilteredInputStream(System.in);

		try {
			ps = new PrintStream(new FilteredStreams.FilteredStream(new ByteArrayOutputStream()), false, "UTF8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			boolean retry = false;
			String ip = null;
			String password = "";
			int port = 0;
			if (aArguments.length > 0) {
				for (int i = 0; i < aArguments.length; i++) {
					// Iterate over all parameters.
					if (aArguments[i].equals(UPDATING_FLAG)) {
						Updater.overwrite("ACM.jar.tmp", "ACM.jar");
						Runtime.getRuntime().exec(ACM.getExecCommand(UPDATED_FLAG + " " + VERSION_FLAG + "\"" + VERSIONID + "\""));
						closeACM();
					} else if (aArguments[i].equals(NETWORKING_FLAG)) {
						Network.networked = true;
						retry = true;
					} else if (aArguments[i].equals(RETRYING_FLAG)) {
						retry = true;
					} else if (aArguments[i].equals(UPDATED_FLAG)) {
						retry = true;
						UPDATED = true;
					} else if (aArguments[i].equals(DEVELOPING_FLAG)) {
						DEVELOPING = true;
					} else if (aArguments[i].startsWith(VERSION_FLAG)) {
						previousVersion = aArguments[i].substring(VERSION_FLAG.length(), aArguments[i].length());
					} else if (aArguments[i].startsWith(NETWORKING_IP_FLAG)) {
						ip = aArguments[i].substring(NETWORKING_IP_FLAG.length(), aArguments[i].length());
					} else if (aArguments[i].startsWith(NETWORKING_PORT_FLAG)) {
						port = Integer.parseInt(aArguments[i].substring(NETWORKING_PORT_FLAG.length(), aArguments[i].length()));
					} else if (aArguments[i].startsWith(NETWORKING_PASSWORD_FLAG)) {
						password = aArguments[i].substring(NETWORKING_PASSWORD_FLAG.length(), aArguments[i].length());
					}
				}
			}
			if (Network.networked) {
				try {
					monitorNetwork = new ProgressMonitor(null, "Downloading Networked ACM's Data. Please wait... ", "", 0, 4);
					monitorNetwork.setMillisToPopup(10);
					monitorNetwork.setMillisToDecideToPopup(10);
					new Thread() {
						@Override
						public void run() {
							while (!monitorNetwork.getNote().equals("Done")) {
								try {
									Thread.sleep(5);
									if (monitorNetwork.isCanceled()) {
										Runtime.getRuntime().exec(ACM.getExecCommand(RETRYING_FLAG));
										closeACM();
									}
								} catch (Exception e) {
								}
							}
						}
					}.start();
					monitorNetwork.setNote("Connecting...");
					network = new Network(Network.networked, ip, port, password);
					monitorNetwork.setProgress(1);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "Could not connect to " + ip + ":" + port, "Connection Failed", JOptionPane.ERROR_MESSAGE);
					Runtime.getRuntime().exec(ACM.getExecCommand(RETRYING_FLAG));
					closeACM();
				}
			}
			// Acquiring a lock:
			// http://www.experts-exchange.com/Programming/Programming_Languages/
			// Java/Q_22027757.html
			String user;
			if ((user = Lock.solitaire("ACM", retry)) != null) {
				System.err.println("Another instance is being used by \'" + user + "\'.");
				closeACM();
			}
			try {
				String javaVersion = System.getProperty("java.version");
				String[] jvsplit = javaVersion.split("\\.");
				double js = Double.parseDouble(jvsplit[0] + "." + jvsplit[1]);
				if (js < 1.6) {
					System.out.println("WARNING: Please install JRE 6.0 (or greater) from http://java.sun.com. You are using Java " + javaVersion);
					JOptionPane.showMessageDialog(null, "WARNING: Please install JRE 6.0 (or greater) from http://java.sun.com.\nYou are using Java " + javaVersion, "JRE 6.0+ required",
							JOptionPane.ERROR_MESSAGE);
					closeACM();
				}
			} catch (Exception e) {
			}

			/* Load all the required libraries. */
			loadLibraries();

			/* Load in the properties. */
			ClipProperties.loadProperties();

			setupProperties();
			// macros created in setup properties
			if (macros.isMacrosEnabled()) {
				Macros.loadGroovy(macros);
			}
			setClipWriters();
			setClipItemListener();
			/* If extensions folder doesnt exist create it */
			File extensionDir = new File("extensions");
			if (!(extensionDir.exists() && extensionDir.isDirectory())) {
				extensionDir.mkdir();
			}

			/* Display the splash screen now */
			ACMGUI.setInitialMainDialogProperties();
			splash = new Splash(ACMGUI.mainDialog);
			splash.increaseProgress();

			try {
				Network.init(Network.networkPort);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// load temp files
			acm.getClips().loadTmpFiles();
			splash.increaseProgress();
			ACMGUI.setUpGUI();
			splash.increaseProgress();
			if (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1) {
				try {
					initKeyListener();
				} catch (Exception e) {
					System.out.println("System Error: Keyboard hooks not loaded. System restart maybe required");
				} catch (Error e) {
					System.out.println("System Error: Keyboard hooks not loaded. System restart maybe required");
				}
			}

			/*
			 * Redirect streams after GUI, so that errors will be printed to stdout.
			 */
			System.setOut(ps);
			System.setIn(is);
			if (!DEVELOPING) {
				System.setErr(ps);
			}

			if (!DEVELOPING && ACMGUI.automaticallyUpdate) {
				Updater.startNewInstance();
			}

			/* Upgrade the database isFile if necessary. */
			postUpgrades();
			splash.increaseProgress();

			/*
			 * By now, we assume that the database isFile MUST contain the isString "UTF8" (required by pruner)
			 */
			postInit();
			consoleMain();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method waits till a new clip item is registered into ACM. It returns the Clip Object for this new item
	 * 
	 * @return
	 */
	public static Clip waitForNextClipItem() {
		synchronized (nextItem) {
			try {
				nextItem.wait();
			} catch (InterruptedException e) {
			}
			Clip clip = getClip(nextItem.numSlot);
			return clip;
		}
	}

	/**
	 * Get the Clip object at a particular slot
	 * 
	 * @param numSlot
	 * @return
	 */
	public static Clip getClip(int numSlot) {
		return acm.getClips().getClip(numSlot);
	}

	/**
	 * Get the number of clips currently available
	 * 
	 * @return
	 */
	public static int getACMSize() {
		return acm.getSize();
	}

	/**
	 * Paste a clip
	 * 
	 * @param numSlot
	 */
	public static void pasteClip(int numSlot) {
		ACMGUI.pasteClipboard(true, acm.getClips().getButton(numSlot));
	}

	/**
	 * Currently only setting text is being opened up via static ACM methods. It is possible to save other types in clipboard but it may be dangerous. User can manually do this by creating a
	 * transerable object from com.acm.transferable and saving in clipboard
	 * 
	 * @param string
	 */
	public static void setClipboardText(String string) {
		acm.setClipboardWithString(string);
	}

	public static void deleteClip(int numSlot) {
		acm.getClips().getButton(numSlot).deleteItem();
	}

	private static class NextItem {
		int numSlot;
	}

	private static void setClipItemListener() {
		acm.addClipItemListener(new ClipItemListener() {

			@Override
			public void newClipItemProcessed(int numSlot) {
				ClipProperties.setProperty("last", "" + acm.getLast());
				acm.getClips().makeNewItem(numSlot);
				synchronized (nextItem) {
					nextItem.numSlot = numSlot;
					nextItem.notifyAll();
				}
			}

		});
	}

	/* Compare first version with the second one. */
	@SuppressWarnings("unused")
	private static boolean newerThanVersion(String firstVersion, String secondVersion) {
		int indexOfSpace = firstVersion.indexOf(' ');
		if (indexOfSpace == -1) {
			indexOfSpace = firstVersion.length();
		}
		firstVersion = firstVersion.substring(0, indexOfSpace);
		firstVersion = firstVersion.replaceAll("\\.", "");
		if (firstVersion.length() == 1) {
			firstVersion += "00";
		} else if (firstVersion.length() == 2) {
			firstVersion += "0";
		}
		int fV = Integer.parseInt(firstVersion);

		indexOfSpace = secondVersion.indexOf(' ');
		if (indexOfSpace == -1) {
			indexOfSpace = secondVersion.length();
		}
		secondVersion = secondVersion.substring(0, indexOfSpace);
		secondVersion = secondVersion.replaceAll("\\.", "");
		if (secondVersion.length() == 1) {
			secondVersion += "00";
		} else if (secondVersion.length() == 2) {
			secondVersion += "0";
		}
		int sV = Integer.parseInt(secondVersion);

		return fV > sV;
	}

	/**
	 * The exec command consists of space-separated flags. Appends developing flag if the current context is in developing mode
	 * 
	 * @param parameters
	 * @return
	 */
	public static String getExecCommand(String parameters) {
		String cmd = "javaw -jar ACM.jar";
		if (ACM.DEVELOPING) {
			cmd += " " + DEVELOPING_FLAG;
		}
		cmd += " " + parameters;
		return cmd;
	}

	private static void postACMCleanup() {
		try {
			if (kh != null) {
				kh.poll.cleanup();
			}
		} catch (Exception e) {
		} catch (Error e) {
		}
	}

	/**
	 * Use this method to exit ACM properly.
	 * 
	 * @see #getExecCommand(String)
	 */
	public static void closeACM() {
		postACMCleanup();
		System.exit(0);
	}

	private static void postUpgrades() {
		File oldDB = new File(DATABASE_FILE);
		Scanner scanner = null;
		try {
			scanner = new Scanner(oldDB);
		} catch (FileNotFoundException e) {
			try {
				oldDB.createNewFile();
				scanner = new Scanner(oldDB);
			} catch (IOException e1) {
				System.out.println("Something weird happened while attempting to upgrade the database.");
				return;
			}
		}
		String firstLine = null;
		if (!scanner.hasNextLine() || (scanner.hasNextLine() && !(firstLine = scanner.nextLine()).equals("UTF8"))) {
			/* Replace the DB with a newer one. */
			File tempDB = new File(DATABASE_FILE + ".tmp");
			try {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tempDB, true), "UTF8");
				osw.write("UTF8\n");
				if (firstLine != null) {
					osw.write(firstLine + "\n");
				}
				while (scanner.hasNextLine()) {
					osw.write(scanner.nextLine() + "\n");
				}
				osw.close();
				scanner.close();
				oldDB.delete();
				tempDB.renameTo(oldDB);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(">>>>>>>>>>>>>>>>>ERROR: Could not write text to isFile<<<<<<<<<<<<<<<<<<<");
			}
		} else {
			scanner.close();
		}
	}

	private static void loadLibraries() {
		File f = new File(KEYBOARD_WIN_HOOK_DLL);

		/*
		 * The syshook library injects itself into other programs when getting keyboard events. From what i can tell this is for easier access at a future time. Problem arises when you cleanup. The
		 * hooks are not removed from other applications till they remove it themselves or the programs are closed. So once ACM starts the probability is that the isFile can not be overwritten till
		 * system restart. This leaves little choice for when we want to re-load library if there are any new changes made to it. The best option is to overwrite it on every system restart. This also
		 * creates more issues since we do not want to create properties just to tell if the ACM restart is after a system restart. So the only option left is to overwrite the isFile every single time
		 * and ignore exceptions if there are any. This will ensure that the isFile gets overwritten sooner or later.
		 */

		// if (!f.exists()) {
		try {
			InputStream stream = ACM.class.getResourceAsStream("res/" + KEYBOARD_WIN_HOOK_DLL);
			FileOutputStream fos = new FileOutputStream(f);
			BufferedInputStream br = new BufferedInputStream(stream);
			int c;
			while ((c = br.read()) != -1) {
				fos.write(c);
			}
			fos.close();
			br.close();
		} catch (Exception e) {
			// e.printStackTrace();
		}
		// }
	}

	private static void initKeyListener() {
		kh = new KeyboardHook();
		kh.addEventListener(new ACMKeyListener());
	}

	private static void setupProperties() throws Exception {

		ACMGUI.verticalMode = Boolean.parseBoolean(ClipProperties.getProperty("verticalMode", "false"));
		/* Number of buttons. */
		int numButtons = 10;
		try {
			numButtons = Integer.parseInt(ClipProperties.getProperty("numButtons", "10"));
			int maxSize;
			if (ACMGUI.verticalMode) {
				if ((maxSize = (ACMGUI.screenSize.height - 110) / ACMClip.BUTTON_HEIGHT) < numButtons) {
					ClipProperties.setProperty("numButtons", "" + maxSize);
					numButtons = maxSize;
					System.err.println("Warning: At most " + maxSize + " buttons can fit on the screen.");
				} else if (numButtons < 1) {
					ClipProperties.setProperty("numButtons", "10");
					numButtons = 10;
				}
			} else {
				if ((maxSize = (ACMGUI.screenSize.width - 110) / ACMClip.BUTTON_WIDTH) < numButtons) {
					ClipProperties.setProperty("numButtons", "" + maxSize);
					numButtons = maxSize;
					System.err.println("Warning: At most " + maxSize + " buttons can fit on the screen.");
				} else if (numButtons < 1) {
					ClipProperties.setProperty("numButtons", "10");
					numButtons = 10;
				}
			}
		} catch (NumberFormatException e) {
			ClipProperties.setProperty("numButtons", "10");
		}

		/*****************************************************************
		 * INITIALIZE
		 * ***************************************************************/

		if (Network.networked) {
			try {
				monitorNetwork.setNote("Cloning...");
				monitorNetwork.setProgress(2);
				acm = new ACM(network.getReciever().recieveClipButtons());
				monitorNetwork.setNote("Downloading Images and Setting up...");
				monitorNetwork.setProgress(3);
				acm.getClips().initForNetwork();
				monitorNetwork.setNote("Done");
				monitorNetwork.setProgress(4);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error: Could not retrieve data from Networked ACM\nPerhaps the password is wrong...", "Error", JOptionPane.ERROR_MESSAGE);
				try {
					Runtime.getRuntime().exec(ACM.getExecCommand(ACM.RETRYING_FLAG));
				} catch (IOException ioe) {
				}
				ACM.closeACM();
			}
		} else {
			acm = new ACM(numButtons, new ACMClipSlots(numButtons));
			// Restore isLocked items.
			String lockedItemsStr = ClipProperties.getProperty("isLocked", "");
			String[] lockedItems = smartSplit(lockedItemsStr, ',');
			acm.getClips().restoreLockedItems(lockedItems);
		}

		try {
			int color = Integer.parseInt(ClipProperties.getProperty("color", "-1"));
			if (color != -1) {
				ACMClip.NORMAL_BUTTON_COLOR = new Color(color, true);
			}
		} catch (Exception e) {
			ClipProperties.setProperty("color", "-1");
		}

		ACMGUI.WIDTH = ACMClip.BUTTON_WIDTH * acm.getSize() + ACMGUI.TA_WIDTH;
		ACMGUI.setClipHeight();

		/* Are we in vertical mode? */
		if (ACMGUI.verticalMode) {
			int width = ACMGUI.WIDTH;
			ACMGUI.WIDTH = ACMClip.BUTTON_WIDTH;
			ACMGUI.HEIGHT = width;
		} else {
			ACMClip.BUTTON_HEIGHT = ACMGUI.HEIGHT;
		}

		/*
		 * Menu booleans. By default, if the value is anything OTHER than 'true', it is automatically 'false'.
		 */
		ACMGUI.isAlwaysOnTop = Boolean.parseBoolean(ClipProperties.getProperty("isAlwaysOnTop", "true"));
		ACMGUI.keyPasteEnabled = Boolean.parseBoolean(ClipProperties.getProperty("keyPasteEnabled", "true"));
		ACMGUI.lockInPlace = Boolean.parseBoolean(ClipProperties.getProperty("lockInPlace", "false"));
		acm.setStopMonitoring(Boolean.parseBoolean(ClipProperties.getProperty("stopMonitoring", "false")));
		acm.setStyledPaste(Boolean.parseBoolean(ClipProperties.getProperty("styledPaste", "true")));
		acm.setLoop(Boolean.parseBoolean(ClipProperties.getProperty("looping", "true")));
		ACMKeyListener.useShortcuts = Boolean.parseBoolean(ClipProperties.getProperty("useShortcuts", "true"));
		ACMGUI.automaticallyUpdate = Boolean.parseBoolean(ClipProperties.getProperty("automaticallyUpdate", "true"));
		ACMGUI.snowEffect = Boolean.parseBoolean(ClipProperties.getProperty("snowEffect", "true"));
		acm.setFileSupport(Boolean.parseBoolean(ClipProperties.getProperty("fileSupport", "false")));
		acm.setImageSupport(Boolean.parseBoolean(ClipProperties.getProperty("imageSupport", "false")));
		Network.networkSupport = Boolean.parseBoolean(ClipProperties.getProperty("networkSupport", "false"));
		Network.networkPassword = ClipProperties.getProperty("networkPassword", "");
		acm.setPollClipboard(Boolean.parseBoolean(ClipProperties.getProperty("pollClipboard", "false")));

		macros = new Macros();
		macros.setMacrosEnabled((Boolean.parseBoolean(ClipProperties.getProperty("macrosEnabled", "true"))));

		Network.networkPort = Integer.parseInt(ClipProperties.getProperty("networkPort", "6999"));
		if (Network.networkPort < 1 || Network.networkPort > 65535) {
			Network.networkPort = 6999;
		}

		ACMGUI.lastIp = ClipProperties.getProperty("lastIp", "name:127.0.0.1:6999");
		if (ACMGUI.validatePasswordIpPortString(ACMGUI.lastIp, false) == null) {
			ACMGUI.lastIp = "Name:127.0.0.1:6999";
		}

		ACMGUI.translucentValue = Integer.parseInt(ClipProperties.getProperty("translucentValue", "100"));
		if (ACMGUI.translucentValue < 10 || ACMGUI.translucentValue > 100) {
			ACMGUI.translucentValue = 100;
		}

		/* Restore the last location (if it fits on the screen). */
		int windowLocationX = Integer.parseInt(ClipProperties.getProperty("windowLocationX", "" + (ACMGUI.screenSize.width - ACMGUI.WIDTH) / 2));
		int windowLocationY = Integer.parseInt(ClipProperties.getProperty("windowLocationY", "0"));
		if (windowLocationX + ACMGUI.WIDTH > ACMGUI.screenSize.width || windowLocationX < 0) {
			windowLocationX = (ACMGUI.screenSize.width - ACMGUI.WIDTH) / 2;
			ClipProperties.setProperty("windowLocationX", "" + windowLocationX);
		}
		if (windowLocationY + ACMGUI.HEIGHT > ACMGUI.screenSize.height || windowLocationY < 0) {
			windowLocationY = 0;
			ClipProperties.setProperty("windowLocationY", "" + windowLocationY);
		}
		ACMGUI.location = new Point(windowLocationX, windowLocationY);
		ACMClipSlots.previousIndex = Integer.parseInt(ClipProperties.getProperty("previousIndex", "0"));
		if (ACMClipSlots.previousIndex >= acm.getSize()) {
			ACMClipSlots.previousIndex = 0;
			ClipProperties.setProperty("previousIndex", "0");
		}
		int last = Integer.parseInt(ClipProperties.getProperty("last", "0"));
		if (last >= acm.getSize()) {
			last = 0;
			ClipProperties.setProperty("last", "0");
		}
		acm.setLast(last);

		/* Read in the history isFile. */
		try {
			Scanner scanner = new Scanner(new File(COMMAND_TEXBOX_HISTORY_FILE));
			while (scanner.hasNextLine()) {
				ACMGUI.history.add(scanner.nextLine());
			}
			ACMGUI.historyIndex = ACMGUI.history.size();
			ACMGUI.partiallyEntered = ClipProperties.getProperty("partiallyEntered", "");
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Load all accepted extensions.
		String extensionsStr = ClipProperties.getProperty("extensions", "");
		String[] extensions = smartSplit(extensionsStr, ',');
		for (int j = 0; j < extensions.length; j++) {
			if (extensions[j] != null && extensions[j].length() > 0) {
				ExtensionDialog.extensions.add(extensions[j]);
			}
		}

		// Set the last pruned time to now (new ACM user, so prune a week from
		// now)
		Pruner.lastPrunedTime = Long.parseLong(ClipProperties.getProperty("lastPrunedTime", "" + System.currentTimeMillis()));
		Pruner.numDaysToPrune = Integer.parseInt(ClipProperties.getProperty("numDaysToPrune", "7"));
	}

	public static ClipWriter getACMClipWriter() {
		return new ClipWriter() {

			@Override
			public void writeFileList(List list, int slotNum) {
				writeTmpFileList(list, slotNum);
			}

			@Override
			public void writeImage(Image image, int slotNum) {
				writeTmpImage(image, slotNum);
			}

			@Override
			public void writeString(String string, int slotNum) {
				try {
					Scanner scan = new Scanner(string);
					OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(new File(DATABASE_FILE), true), "UTF8");
					String timeInM = "" + System.currentTimeMillis();
					osw.write(timeInM + "\n");
					while (scan.hasNextLine()) {
						osw.write(scan.nextLine() + "\n");
					}
					osw.write(timeInM + "\n");
					osw.close();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(">>>>>>>>>>>>>>>>>ERROR: Could not write text to isFile<<<<<<<<<<<<<<<<<<<");
				}
				writeTmpString(string, slotNum);
			}
		};
	}

	private static void setClipWriters() {
		acm.setDataBaseWriter(getACMClipWriter());
	}

	private static void writeTmpImage(Image image, int slotNum) {
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		g2.drawImage(image, 0, 0, null);
		g2.dispose();
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "bmp", new File("clip" + slotNum + ".tmp"));
			ImageIO.write(bi, "bmp", baos);
			byte[] imageInBytes = baos.toByteArray();
			baos.close();
			CRC32 crc = new CRC32();
			crc.update(imageInBytes);
			acm.getClips().getButton(slotNum).setCrcValue(crc.getValue());
		} catch (IOException ioe) {
			System.out.println("write: " + ioe.getMessage());
		}
		acm.getClips().getButton(slotNum).setNewImage(null, false);
	}

	private static void writeTmpString(String string, int slotNum) {

		/* Write to a tmp isFile with the new contents. */
		try {
			boolean plain = true;
			try {
				Transferable t = getClip(slotNum).clip;
				DataFlavor bestFlavor = DataFlavor.selectBestTextFlavor(t.getTransferDataFlavors());
				String bestFlavorMimeType = bestFlavor.getMimeType();
				if (bestFlavorMimeType.indexOf("text/rtf") != -1) {
					FileOutputStream fos = new FileOutputStream("clip" + slotNum + ".mtmp");
					Writer out = new OutputStreamWriter(fos, "UTF8");
					BufferedInputStream stream = new BufferedInputStream(((InputStream) t.getTransferData(bestFlavor)));
					int r;
					while ((r = stream.read()) != -1) {
						out.write(r);
					}
					out.flush();
					out.close();
					stream.close();
					acm.getClips().getButton(slotNum).setNewString(null, "rtf");
					plain = false;
				} else if (bestFlavorMimeType.indexOf("text/html") != -1) {
					FileOutputStream fos = new FileOutputStream("clip" + slotNum + ".mtmp");
					Writer out = new OutputStreamWriter(fos, "UTF8");
					BufferedReader reader = new BufferedReader((Reader) t.getTransferData(bestFlavor));
					int r;
					while ((r = reader.read()) != -1) {
						out.write(r);
					}
					out.flush();
					out.close();
					reader.close();
					acm.getClips().getButton(slotNum).setNewString(null, "html");
					plain = false;
				}
			} catch (Exception e) {
			}
			FileOutputStream fos = new FileOutputStream("clip" + slotNum + ".tmp");
			Writer out = new OutputStreamWriter(fos, "UTF8");
			out.write(string);
			out.close();
			if (plain) {
				acm.getClips().getButton(slotNum).setNewString(null, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void writeTmpFileList(List list1, int slotNum) {
		if (list1 != null) {
			try {
				FileOutputStream fos = new FileOutputStream("clip" + slotNum + ".tmp");
				Writer out = new OutputStreamWriter(fos, "UTF8");
				for (int i = 0; i < list1.size(); i++) {
					out.write(((File) list1.get(i)).getAbsolutePath() + "\n");
				}
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			acm.getClips().getButton(slotNum).setNewFile(null, false);
		}
	}

	private static void postInit() {
		if (!Network.networked) {
			acm.init();
		}

		// Start the pruner only if it is enabled
		if (Pruner.numDaysToPrune > 0) {
			Pruner.startNewInstance();
		}

		/* Finally, show the program. */
		splash.increaseProgress();
		ACMGUI.showACM();

		/* If updated then show the tray icon balloon */
		if (UPDATED) {
			ACMGUI.showBalloon(ACMGUI.MESSAGE_TYPE_UPDATED);
		}
		running = true;
	}

	/**
	 * 
	 */
	private static void consoleMain() {
		System.out.println("Welcome to " + TITLE + " " + VERSIONID + ".");
		System.out.println("Enter 'h' for Help.");

		int getCh;
		while (true) {

			getCh = readL();
			// System.out.println(getCh + " (" + (char) getCh + ")");
			synchronized (acm.getSafeClipLock()) {
				if (getCh >= '0' && getCh <= '9') {
					getCh = getCh - 48;
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					try {
						String tempNum = in.readLine();
						if (tempNum.length() > 0) {
							getCh = Integer.parseInt("" + getCh + tempNum);
						}
						if (getCh >= 0 && getCh < acm.getSize()) {
							ACMClip button = acm.getClips().getButton(getCh);
							if (!button.isEmpty()) {
								acm.setClipboardWithSlot(getCh);
							} else {
								System.out.println("Nothing in slot " + getCh + ".");
							}
						} else {
							System.out.println("Slot " + getCh + " does not exist.");
						}
					} catch (Exception e) {
						System.out.println("Trouble Reading Number");
					}
				} else if (getCh == 13 || getCh == 10) {
					System.out.println();
				} else if (getCh == -1) {
				} else if (getCh == 'd' && !Network.networked) {
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					String slotsStr = null;
					try {
						slotsStr = in.readLine();
						if (slotsStr.equals("")) {
							System.out.println("Please enter the slots to delete (e.g. 1,3-5,9): ");
							slotsStr = in.readLine();
						}
					} catch (Exception e) {
						System.out.println("Trouble Reading Slots to Delete");
					}
					Stack<Integer> slotsToDelete = getSlotsToDelete(slotsStr);
					if (slotsToDelete != null) {
						while (!slotsToDelete.empty()) {
							acm.getClips().getButton(slotsToDelete.pop()).deleteItem();
						}
					} else {
						System.err.println("The line \"" + slotsStr + "\" could not be processed.");
					}
				} else if (getCh == 'D' && !Network.networked) {
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					String slotsStr = null;
					try {
						slotsStr = in.readLine();
						if (slotsStr.equals("")) {
							System.out.println("Please enter the slots to not delete (e.g. 4,7-9): ");
							slotsStr = in.readLine();
						}
					} catch (Exception e) {
						System.out.println("Trouble Reading Slots to Delete");
					}
					Stack<Integer> slotsToDelete = getSlotsToDelete(slotsStr);
					if (slotsToDelete != null) {
						for (int i = 0; i < acm.getSize(); i++) {
							if (!slotsToDelete.contains(i)) {
								acm.getClips().getButton(i).deleteItem();
							}
						}
					} else {
						System.err.println("The line \"" + slotsStr + "\" could not be processed.");
					}
				} else if (getCh == 'h') {
					HelpDialog.openHelpDialog();
				} else if (getCh == 'z') {
					consoleSearch();
				} else if (getCh == 'a') {
					consoleSearch();
				} else if (getCh == 'j') {
					String join = acm.getClips().joinSlots();
					System.out.println("Choosing: " + join);
					acm.setIgnoreThisString(join);
					acm.setClipboardWithString(join);
				} else if (getCh == ' ') {
					String join = acm.getClips().joinSlotsWithSpace();
					acm.setIgnoreThisString(join);
					System.out.println("Choosing: " + join);
					acm.setClipboardWithString(join);
				} else if (getCh == 'n') {
					String join = acm.getClips().joinSlotsWithNewLine();
					acm.setIgnoreThisString(join);
					System.out.println("Choosing: " + join);
					acm.setClipboardWithString(join);
				} else if (getCh == 's') {
					acm.getClips().printSlots();
				} else if (getCh == 'm') {
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					String search = null;
					System.out.println("Please enter the time key (viewable with /): ");
					try {
						search = in.readLine();
						if (search.equals("")) {
							search = in.readLine();
						}
					} catch (Exception e) {
						System.out.println("Trouble Reading Search Term");
					}

					if (haventRefreshed) {
						System.out.println("Refreshing List first (Please run the / command beforehand to get previews and keys)");
						refreshStack();
						System.out.println(" ...Refreshed");
						haventRefreshed = false;
					}
					int i = 0;
					boolean found = true;
					try {
						i = Integer.parseInt(search);
						if (i > time.size()) {
							System.out.println("INVALID NUMBER - TOO BIG");
							found = false;
						} else if (i < 0) {
							System.out.println("INVALID NUMBER - TOO SMALL");
							found = false;
						}
					} catch (NumberFormatException e1) {
						found = false;
						System.out.println("INVALID NUMBER");
					}
					if (found) {
						try {
							Scanner file = new Scanner(new File(DATABASE_FILE), "UTF8");
							file.nextLine();
							String readLine = "";
							boolean gotit = false;
							while (file.hasNextLine() && !gotit) {
								String temp = file.nextLine();
								if (temp.trim().equals(time.get(i))) {
									gotit = true;
									while (file.hasNextLine()) {
										String temp1 = file.nextLine();
										if (!temp1.trim().equals(time.get(i))) {
											readLine += temp1 + "\n";
										} else {
											break;
										}
									}
								}
							}
							if (readLine.length() > 0) {
								readLine = readLine.substring(0, readLine.lastIndexOf("\n"));
								System.out.println("Choosing: " + readLine);
								acm.setClipboardWithString(readLine);
							}
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("Oops! i did it again");
						}
					}
				} else if (getCh == '/') {
					System.out.println("Below is the list of all the saves");
					System.out.println("Refreshing List first");
					refreshStack();
					System.out.println(" ...Refreshed");
					System.out.println("Total Items found = " + time.size());
					System.out.println("Please Choose from the times Below");
					for (int i = 0; i < time.size(); i++) {
						Date date = new Date(Long.parseLong(time.get(i)));
						System.out.println(i + "\t" + date.toString() + "\tPreview:\t" + preview.get(time.get(i)));
					}
				} else if (getCh == 'x' && !Network.networked) {
					acm.getClips().performAwk();
				} else if (getCh == '.') {
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					String macroStr = null;
					try {
						macroStr = in.readLine();
						if (macroStr.equals("")) {
							System.out.println("Please enter the macro name: ");
							macroStr = in.readLine();
						}
					} catch (Exception e) {
						System.out.println("Trouble Reading Macro name");
						continue;
					}
					if (macros.isMacrosEnabled()) {
						String[] splits = smartSplit(macroStr, ' ');
						String[] args = { "" };
						if (splits.length > 1) {
							args = Arrays.copyOfRange(splits, 1, splits.length);
						}
						if (macros.runMacro(splits[0], args) == null) {
							System.out.println("Could not run macro. Make sure " + splits[0] + ".groovy file exists in $ACM_INSTALL/macros/ folder");
							JOptionPane.showMessageDialog(null, "Could not run macro. Make sure " + splits[0] + ".groovy file exists in $ACM_INSTALL/macros/ folder", "Macro Error",
									JOptionPane.ERROR_MESSAGE);
						}
					} else {
						System.out.println("Macros are disabled");
						JOptionPane.showMessageDialog(null, "Macros are currently disabled. Please enable and try again", "Macros are Disabled", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					System.out.println("Sorry, the key '" + getCh + "' is invalid.");
				}
			}
		}
	}

	private static void consoleSearch() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			in.readLine();
			in.close();
		} catch (Exception e) {
			System.out.println("Trouble Disposing Search Term");
		}
	}

	private static int readL() {
		// if (isConsoleMode) {
		try {
			int temp = System.in.read();
			// System.out.println(temp);
			return temp;
		} catch (Exception e) {
			return -1;
		}
	}

	// public void refreshStackWithUnicode() {
	// time = new ArrayList<String>();
	// preview = new HashMap<String, String>();
	// try {
	// Scanner isFile = new Scanner(new File(fileToSave), "UTF8");
	// boolean found = false;
	// String temp = "";
	// Pattern pattern = Pattern.compile("^[0-9]{13}$");
	// while (isFile.hasNextLine()) {
	// if (pattern.matcher(temp = isFile.nextLine()).matches()) {
	// found = true;
	// boolean firstone = true;
	// while (isFile.hasNextLine()) {
	// String temp1 = isFile.nextLine();
	// if (temp1.indexOf(temp) == 0) {
	// time.add(temp);
	// found = false;
	// break;
	// } else if (firstone && temp1.trim().length() > 0) {
	// firstone = false;
	// preview.put(temp, temp1);
	// }
	// }
	// }
	// }
	// isFile.close();
	// if (found) {
	// System.out.println("The Save isFile seems to be messed up around the key "
	// + temp);
	// System.out.println("ACM strongly urges you to create new one or remove
	// the key from the isFile");
	// }
	// haventRefreshed = false;
	// } catch (Exception e) {
	// //FIXME
	// e.printStackTrace();
	// System.out.println("Some error reading from the saved isFile");
	// }
	// }

	private static void refreshStack() {
		time = new ArrayList<String>();
		preview = new HashMap<String, String>();
		try {
			Scanner file = new Scanner(new File(DATABASE_FILE), "UTF8");
			file.nextLine();
			boolean found = false;
			String temp = "";
			while (file.hasNextLine()) {
				temp = file.next("[0-9]{13}+");
				found = true;
				boolean firstone = true;
				while (file.hasNextLine()) {
					String temp1 = file.nextLine();
					if (temp1.indexOf(temp) != -1) {
						time.add(temp);
						found = false;
						break;
					} else if (firstone && temp1.trim().length() > 0) {
						firstone = false;
						preview.put(temp, temp1);
					}
				}
			}
			file.close();
			if (found) {
				System.out.println("The Save isFile seems to be messed up around the key " + temp);
				System.out.println("ACM strongly urges you to create new one or remove the key from the isFile");
			}
			haventRefreshed = false;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Some error reading from the saved isFile");
		}
	}

	private static Stack<Integer> getSlotsToDelete(String data) {
		String[] split = smartSplit(data, ',');
		if (split.length == 0) {
			System.out.println("Error: No numbers were provided.");
			return null;
		}
		for (int i = 0; i < split.length; i++) {
			if (split[i].trim().length() == 0) {
				System.out.println("Error: No slot number was found at position " + (i + 1) + ".");
				return null;
			}
		}
		Stack<Integer> slotsToDelete = new Stack<Integer>();
		for (int i = 0; i < split.length; i++) {
			String currentItem = split[i].trim();
			try {
				int num = Integer.parseInt(currentItem);
				if (!slotsToDelete.contains(num)) {
					if (num < 0 || num >= acm.getSize()) {
						System.out.println("Error: The number '" + currentItem + "' at position " + (i + 1) + " is not in the valid range of 0-" + (acm.getSize() - 1) + ".");
						return null;
					}
					slotsToDelete.add(num);
				}
			} catch (NumberFormatException e1) {
				if (currentItem.indexOf('-') != -1) {
					String[] splitNumbers = smartSplit(currentItem, '-');
					if (splitNumbers.length != 2) {
						System.out.println("Error: The range '" + currentItem + "' at position " + (i + 1) + " contains more than one dash.");
						return null;
					}
					int firstNum;
					try {
						firstNum = Integer.parseInt(splitNumbers[0].trim());
					} catch (NumberFormatException e) {
						System.out.println("Error: Was expecting a slot number on the left side of '" + currentItem + "' at position " + (i + 1) + ". Found '" + splitNumbers[0].trim() + "' instead.");
						return null;
					}
					int secondNum;
					try {
						secondNum = Integer.parseInt(splitNumbers[1].trim());
					} catch (NumberFormatException e) {
						System.out
								.println("Error: Was expecting a slot number on the right side of '" + currentItem + "' at position " + (i + 1) + ". Found '" + splitNumbers[1].trim() + "' instead.");
						return null;
					}
					if (firstNum < 0 || firstNum >= acm.getSize() || secondNum < 0 || secondNum >= acm.getSize()) {
						System.out.println("Error: The range '" + currentItem + "' at position " + (i + 1) + " is not within the valid range of 0-" + (acm.getSize() - 1) + ".");
						return null;
					}
					for (int j = Math.min(firstNum, secondNum); j <= Math.max(firstNum, secondNum); j++) {
						if (!slotsToDelete.contains(j)) {
							slotsToDelete.add(j);
						}
					}
				} else {
					System.out.println("Error: Found an invalid input '" + currentItem + "' at position " + (i + 1) + ".");
					return null;
				}
			}
		}
		return slotsToDelete;
	}

	/**
	 * Smart Split the isString with provided delimiter. Takes care of the delimiter at end problem.
	 * 
	 * @param isString
	 * @param delimiter
	 * @return
	 */
	private static String[] smartSplit(String string, char delimiter) {
		int length = string.length();
		if (length == 0) {
			return new String[0];
		}
		String s = "";
		ArrayList<String> array = new ArrayList<String>();
		boolean justSawDelimiter = false;
		int i;
		for (i = 0; i < length; i++) {
			char c = string.charAt(i);
			if (c == delimiter) {
				array.add(s);
				s = "";
				justSawDelimiter = true;
			} else {
				s += c;
				justSawDelimiter = false;
			}
		}
		if (justSawDelimiter && i == length || !justSawDelimiter) {
			array.add(s);
		}
		String[] results = new String[array.size()];
		for (i = 0; i < results.length; i++) {
			results[i] = array.get(i);
		}
		return results;
	}

	@SuppressWarnings("unused")
	private static void searchAndReplace(String string) {
		string = string.trim();
		String current = "";
		String slot = null;
		String reg = null;
		String sub = null;
		String[] target = null;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (slot == null) {
				if (c == '/') {
					slot = current;
					current = "";
					i++;
					c = string.charAt(i);
				} else {
					current += c;
				}
			}

			if (reg == null && slot != null) {
				if (c == '\\') {
					current += c;
					i++;
					c = string.charAt(i);
					current += c;
				} else if (c == '/') {
					reg = current;
					current = "";
					i++;
					c = string.charAt(i);
				} else {
					current += c;
				}
			}

			if (sub == null && slot != null && reg != null) {
				if (c == '\\') {
					current += c;
					i++;
					c = string.charAt(i);
					current += c;
				} else if (c == '/') {
					sub = current;
					current = "";
					i++;
					c = string.charAt(i);
				} else {
					current += c;
				}
			}

			if (slot != null && reg != null && sub != null) {
				if (i + 1 < string.length()) {
					current += c;
				} else {
					current += c;
					target = current.split(" ");
				}
			}

		}

		if (slot == null) {
			System.out.println("An incorrect Input Slot was found");
			return;
		}
		if (reg == null) {
			System.out.println("An incorrect Regular Expression was found");
			return;
		}
		if (sub == null) {
			System.out.println("An incorrect Substitution was found");
			return;
		}
		if (target == null) {
			System.out.println("Target Slot/s were not found");
			return;
		}
		int numSlot;
		int[] targetSlots = new int[target.length];
		boolean copyToClipboard = false;
		try {
			numSlot = Integer.parseInt(slot);
		} catch (NumberFormatException e) {
			System.out.println("Slot number required as first input");
			return;
		}
		if (numSlot < 0 || numSlot > acm.getSize()) {
			System.out.println("Slot number must be between 0 and " + acm.getSize());
			return;
		}
		for (int i = 0; i < target.length; i++) {
			try {
				targetSlots[i] = Integer.parseInt(target[i]);
			} catch (NumberFormatException e) {
				if (target[i].equalsIgnoreCase("c")) {
					targetSlots[i] = -1;
					copyToClipboard = true;
				} else {
					System.out.println("Target Slots were invalid");
					return;
				}
			}
			if (!copyToClipboard && (targetSlots[i] < 0 || targetSlots[i] > acm.getSize())) {
				System.out.println("Target Slot numbers must be between 0 and " + acm.getSize());
				return;
			}
		}
		System.out.println("input slot: " + numSlot);
		System.out.println("regex: " + reg);
		System.out.println("sub: " + sub);
		for (int i = 0; i < targetSlots.length; i++) {
			System.out.println("target slot: " + targetSlots[i]);
		}
		System.out.println("Copy to clipboard: " + copyToClipboard);
	}

}
