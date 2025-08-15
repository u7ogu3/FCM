package com.acm.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Stack;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import com.acm.main.util.Updater;
import com.acm.util.ClassPathHacker;

public class Macros {

	private static final String INTERNAL_MACROS_PATH = "res/macros/";
	private static final String MACROS_PATH = "macros/";
	private static final String GROOVY_EXT = ".groovy";
	private static final String TEMP_MACRO_FILE_NAME = "macro.groovy";

	private boolean macrosEnabled;
	private boolean startingGroovy = false;
	private Object lock = new Object();
	private Method groovyRun;
	private Object groovy;
	private Stack<Thread> macroThreads = new Stack<Thread>();

	public Macros() {
		File file = new File(MACROS_PATH);
		if (!file.exists()) {
			file.mkdir();
		}
	}

	/**
	 * This method initiates the download of Groovy Library.
	 * 
	 * @param macros
	 */
	public static void loadGroovy(Macros macros) {
		File f = new File(Updater.GROOVY_JAR_FILE_NAME);
		if (f.exists()) {
			String localmd5 = Updater.md5File(Updater.GROOVY_JAR_FILE_NAME);
			if (!Updater.GROOVY_MD5.equals(localmd5)) {
				f.delete();
			}
		}
		if (!f.exists()) {
			ProgressMonitor monitor = new ProgressMonitor(null, "ACM "+ ACM.VERSIONID +" Update:", "Downloading Groovy for Macros. Please wait...", 0, Updater.GROOVY_JAR_SIZE);
			monitor.setMillisToDecideToPopup(10);
			monitor.setMillisToPopup(10);
			if (!Updater.downloadGroovy(monitor)) {
				JOptionPane.showMessageDialog(null,
						"Groovy Download failed! \nACM has disabled macros. Enable from menu to retry download!\nOr manually download groovy-all-1.6.5.jar in ACM Install Folder\nmd5sum: "
								+ Updater.GROOVY_MD5, "ACM Download Failed", JOptionPane.ERROR_MESSAGE);
				ClipProperties.setProperty("macrosEnabled", "false");
				macros.setMacrosEnabled(false);
				return;
			}
		}
		macros.initGroovy();

	}

	/**
	 * Loads groovy library into current context and enables methods to run groovy scripts
	 */
	public void initGroovy() {
		Runnable runnable = new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {
					startingGroovy = true;
					ClassPathHacker.addFile(Updater.GROOVY_JAR_FILE_NAME);
					Class clazz = Class.forName("groovy.lang.GroovyShell");
					groovy = clazz.newInstance();
					groovyRun = clazz.getMethod("run", new Class[] { File.class, String[].class });
					groovyRun.setAccessible(true);
					synchronized (lock) {
						lock.notifyAll();
					}
					startingGroovy = false;
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "Groovy Load Failed! Disabling Macros", "Groovy Failed", JOptionPane.ERROR_MESSAGE);
					ClipProperties.setProperty("macrosEnabled", "false");
					macrosEnabled = false;
					e.printStackTrace();
				}
			}

		};
		new Thread(runnable).start();
	}

	/**
	 * Runs Macro @name with argument @args. The method will look for scripts in internal macro library and then the macros folder, with internal library of scripts getting priority.
	 * 
	 * @param name
	 * @param args
	 * @return
	 */
	public Thread runMacro(final String name, final String[] args) {
		if (startingGroovy) {
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		class Invoke extends Thread {

			File file;

			Invoke(File file) {
				this.file = file;
			}

			@Override
			public void run() {
				try {
					groovyRun.invoke(groovy, file, args);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (macrosEnabled) {
			Invoke macro;
			try {
				InputStream stream;
				if ((stream = ACM.class.getResourceAsStream(INTERNAL_MACROS_PATH + name + GROOVY_EXT)) != null) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
					PrintWriter pw = new PrintWriter(new FileWriter(TEMP_MACRO_FILE_NAME), true);
					int c;
					while ((c = reader.read()) != -1) {
						pw.write(c);
					}
					pw.close();
					reader.close();
					macro = new Invoke(new File(TEMP_MACRO_FILE_NAME));
				} else {
					File file = new File(MACROS_PATH + name + GROOVY_EXT);
					if (!file.exists()) {
						return null;
					}
					macro = new Invoke(file);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			macro.start();
			macroThreads.push(macro);
			return macro;
		}
		return null;
	}

	/**
	 * Disable or enable macros. If macros are set to enabled then the method will attempt to load the groovy library If they are set to disabled then it will send interrupt signal to all currently
	 * running scripts.
	 * 
	 * @param macrosEnabled
	 * @return
	 */
	public boolean setMacrosEnabled(boolean macrosEnabled) {
		this.macrosEnabled = macrosEnabled;
		if (macrosEnabled) {
			loadGroovy(this);
		} else {
			for (Thread macro : macroThreads) {
				if (macro != null) {
					macro.interrupt();
				}
			}
		}
		return this.macrosEnabled;
	}

	/**
	 * Check if macros are enabled
	 * 
	 * @return
	 */
	public boolean isMacrosEnabled() {
		return macrosEnabled;
	}

}
