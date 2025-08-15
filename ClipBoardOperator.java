package com.acm.clip;

import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import com.acm.transferable.StringSelection;

/**
 * ClipBoardOperator is the class that interacts with the clipboard and retieves data based on properties defined in this class. Once the {@link #init()} for ClipBoardOperator is called, it
 * effectively becomes the owner of the clipboard till the application closes. Due to this there should NEVER be two instances running of ClipboardOperator at the same time. This class is meant to be
 * extended by the top level clipboard management entity. If anyone wishes to however create a local member then they must manually make sure that it is not initalized multiple times.
 * 
 * Clipboard monitoring and data retrieval is synchronized on object {@link #getSafeClipLock()} This should be used to synchronize if the value for fields such as last, lastSaved needs to be really
 * accurate. In human terms it is highly unlikely that two copies can come so fast as to create any issues so avoiding synchronization is recommended whereever not needed.
 * 
 * @author Ashish Gupta, David Chang
 * 
 */
public class ClipBoardOperator implements ClipBoardOperations, ClipboardOwner {

	static ClipBoardOperations clipOperation;

	private String ignoreThisString = null;
	private Transferable lastTransfer = null;
	private ClipSlots clipSlots = null;
	private Object lastSaved = null;
	private ClipWriter clipWriter = null;
	private final Object safeClipLock = new Object();
	private List<ClipItemListener> clipItemListeners = new LinkedList<ClipItemListener>();

	/* Properties */
	private boolean loop = true;
	private boolean fileSupport = false;
	private boolean imageSupport = false;
	private boolean stopMonitoring = false;
	private boolean styledPaste = true;
	private int last = 0;
	private boolean pollClipboard = false;

	/**
	 * Initializes a new ClipSlots for given size
	 * 
	 * @param size
	 */
	public ClipBoardOperator(int size) {
		clipSlots = new ClipSlots(size);
		clipSlots.init();
		clipOperation = this;
	}

	public ClipBoardOperator(int size, ClipSlots clipSlots) throws Exception {
		if (size != clipSlots.getSize()) {
			throw new Exception("Size Mismatch");
		}
		this.clipSlots = clipSlots;
		clipSlots.init();
		clipOperation = this;
	}

	/**
	 * Sets the clipSlots given. DOES NOT INITIALIZE.
	 * 
	 * @see ClipSlots#init() @see ACMClipSlots#initForNetwork()
	 * @param clipSlots
	 */
	public ClipBoardOperator(ClipSlots clipSlots) {
		this.clipSlots = clipSlots;
	}

	/**
	 * Get the number of slots currently available.
	 * 
	 * @see ClipSlots#getSize()
	 * @return
	 */
	public int getSize() {
		return clipSlots.getSize();
	}

	/**
	 * Initializes this ClipboardOperator as the ClipboardOwner of the system clipboard Or Poll the Clipboard depending on setting. This method must be called to start up this ClipboardOperator
	 * otherwise it will not collect any clipSlots.
	 */
	public void init() {
		if (isPollClipboard()) {
			startPolling();
		} else {
			lostOwnership(null, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.datatransfer.ClipboardOwner#lostOwnership(java.awt.datatransfer .Clipboard, java.awt.datatransfer.Transferable)
	 */
	@Override
	public final void lostOwnership(Clipboard aClipboard, Transferable aContents) {
		if (!isPollClipboard()) {
			boolean done = false;
			int increaseTime = 0;
			while (!done) {
				try {
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					Transferable contents = clipboard.getContents(null);
					// Incase 100 is not enough lets start increasing time
					Thread.sleep(100 + increaseTime);
					increaseTime += 20;
					setClipboard(contents);
					done = true;
				} catch (Exception e) {
					if (increaseTime > 200) {
						System.out.println("5 attempts to claim onwnership of system clipboard have failed. Switching to polling");
						setPollClipboard(true);
						init();
						return;
					}
				}
			}
		}
	}

	private final void startPolling() {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					while (isPollClipboard()) {
						Thread.sleep(350);
						performNewClipOperations();
					}
				} catch (Exception e) {
					System.out.println("Exception encountered while polling. Turning ownership on");
					setPollClipboard(false);
					init();
				}
			}

		};

		Thread thread = new Thread(runnable);
		thread.start();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acm.clip.ClipBoardOperations#setClipboardWithString(java.lang.String)
	 */
	public void setClipboardWithString(String aString) {
		StringSelection stringSelection = new StringSelection(aString);
		setClipboard(stringSelection);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.acm.clip.ClipBoardOperations#setClipboard(java.awt.datatransfer. Transferable)
	 */
	public void setClipboard(Transferable object) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(object, this);
		if (!isPollClipboard()){
			performNewClipOperations();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acm.clip.ClipBoardOperations#getClipboardContents()
	 */
	public Object getClipboardContents() {
		Object result = null;
		Transferable contents = null;
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			contents = clipboard.getContents(null);
			lastTransfer = contents;
		} catch (HeadlessException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
		}
		boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		boolean hasTransferableImage = false;
		boolean hasTransferableFile = false;
		if (imageSupport) {
			hasTransferableImage = (contents != null) && contents.isDataFlavorSupported(DataFlavor.imageFlavor);
		}
		if (fileSupport) {
			hasTransferableFile = (contents != null) && contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		}
		if (hasTransferableText) {
			try {
				result = contents.getTransferData(DataFlavor.stringFlavor);
			} catch (Exception ex) {
				// highly unlikely since we are using a standard DataFlavor
			}
		} else if (hasTransferableImage) {
			try {
				result = contents.getTransferData(DataFlavor.imageFlavor);
			} catch (Exception ex) {
				// highly unlikely since we are using a standard DataFlavor
			}
		} else if (hasTransferableFile) {
			try {
				result = contents.getTransferData(DataFlavor.javaFileListFlavor);
			} catch (Exception ex) {
				// highly unlikely since we are using a standard DataFlavor
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acm.clip.ClipBoardOperations#setClipboardWithSlot(int)
	 */
	public void setClipboardWithSlot(int slotNum) {
		if (clipSlots.getClip(slotNum).isString) {
			Object clip = clipSlots.getClip(slotNum).stringObject;
			System.out.println("Choosing: \"" + clip + "\".");
			if (!isStyledPasteOn()) {
				setClipboardWithString((String) clip);
				return;
			}
		}
		setClipboard(clipSlots.getClip(slotNum).clip);
	}

	@SuppressWarnings("unchecked")
	private void performNewClipOperations() {
		try {
			if (!stopMonitoring) {
				synchronized (safeClipLock) {
					Object clipContentsBeforeType = getClipboardContents();
					if (clipContentsBeforeType != null) {
						String tempClipString = null;
						Image tempClipImage = null;
						List tempClipFileList = null;
						if (clipContentsBeforeType instanceof String) {
							tempClipString = (String) clipContentsBeforeType;
						} else if (clipContentsBeforeType instanceof Image && imageSupport) {
							tempClipImage = (Image) clipContentsBeforeType;
						} else if (clipContentsBeforeType instanceof List && fileSupport) {
							tempClipFileList = (List) clipContentsBeforeType;
						}
						if (lastSaved == null || tempClipString != null || tempClipImage != null || tempClipFileList != null) {
							boolean set = true;
							if (tempClipString != null) {
								if (lastSaved == null || !(lastSaved instanceof String) || !tempClipString.equals(lastSaved)) {
									lastSaved = tempClipString;
									if (ignoreThisString != null) {
										if (ignoreThisString.equals(lastSaved)) {
											set = false;
										}
									}
									if (set) {
										if (((String) lastSaved).length() == 0) {
											set = false;
										}
										if (set) {
											for (int i = 0; i < getSize(); i++)
												if (clipSlots.getClip(i).isString)
													if (lastSaved.equals(clipSlots.getClip(i).stringObject))
														set = false;
											if (set) {
												System.out.println("\n         New Clipboard Content");
												System.out.println("**************************************************");
												System.out.println(lastSaved);
												System.out.println("**************************************************");

											}
										}
									}
								} else {
									set = false;
								}
							} else if (tempClipImage != null && imageSupport) {
								lastSaved = tempClipImage;
								long crcToCheck = crcOfImage(tempClipImage);
								for (int i = 0; i < getSize(); i++) {
									if (clipSlots.getClip(i).isImage) {
										if (clipSlots.getClip(i).crcValue == crcToCheck) {
											set = false;
										}
									}
								}
								// This is to prevent continuous image manipulation
								if (isPollClipboard()) {
									setClipboardWithString("");
								}
							} else if (tempClipFileList != null && fileSupport) {
								if (lastSaved == null || !(lastSaved instanceof List) || !fileListCompare(tempClipFileList, (List) lastSaved)) {
									lastSaved = tempClipFileList;
									for (int i = 0; i < getSize(); i++) {
										if (clipSlots.getClip(i).isFile) {
											if (fileListCompare((List) clipSlots.getClip(i).fileObject, (List) lastSaved)) {
												set = false;
											}
										}
									}
								} else {
									set = false;
								}
							} else {
								set = false;
							}

							if (set) {
								for (int i = 0; i < getSize(); i++) {
									if (!clipSlots.getClip(i).isLocked) {
										if (clipSlots.getClip(i).isEmpty) {
											/*
											 * Write to a tmp isFile with the new contents.
											 */
											if (lastSaved instanceof String) {
												setIgnoreThisString((String) lastSaved);
												clipSlots.getClip(i).setNewString((String) lastSaved, lastTransfer);
												if (clipWriter != null) {
													clipWriter.writeString((String) lastSaved, i);
												}
											} else if (lastSaved instanceof Image) {
												clipSlots.getClip(i).setNewImage((Image) lastSaved, lastTransfer);
												if (clipWriter != null) {
													clipWriter.writeImage((Image) lastSaved, i);
												}
											} else if (lastSaved instanceof List) {
												clipSlots.getClip(i).setNewFile((List) lastSaved, lastTransfer);
												if (clipWriter != null) {
													clipWriter.writeFileList((List) lastSaved, i);
												}
											}

											last = (i + 1) % getSize();
											fireClipItemProcesssed(i);
											set = false;
											break;
										}
									}
								}
							}
							for (int i = 0; i < getSize() && clipSlots.getClip(last).isLocked; i++) {
								if (clipSlots.getClip(last).isLocked) {
									last++;
									if (last == getSize()) {
										last = 0;
									}
								}
							}

							if (set && loop) {
								if (!clipSlots.getClip(last).isLocked) {

									/*
									 * Write to a tmp isFile with the new contents.
									 */
									if (lastSaved instanceof String) {
										setIgnoreThisString((String) lastSaved);
										clipSlots.getClip(last).setNewString((String) lastSaved, lastTransfer);
										if (clipWriter != null) {
											clipWriter.writeString((String) lastSaved, last);
										}
									} else if (lastSaved instanceof Image) {
										clipSlots.getClip(last).setNewImage((Image) lastSaved, lastTransfer);
										if (clipWriter != null) {
											clipWriter.writeImage((Image) lastSaved, last);
										}
									} else if (lastSaved instanceof List) {
										clipSlots.getClip(last).setNewFile((List) lastSaved, lastTransfer);
										if (clipWriter != null) {
											clipWriter.writeFileList((List) lastSaved, last);
										}
									}
									int tempLast = last;
									last++;
									if (last == getSize()) {
										last = 0;
									}
									fireClipItemProcesssed(tempLast);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean fileListCompare(List list1, List list2) {
		if (list1 != null && list2 != null) {
			if (list1.size() == list2.size()) {
				for (int i = 0; i < list1.size(); i++) {
					if (!((File) list1.get(i)).getAbsolutePath().equals(((File) list2.get(i)).getAbsolutePath())) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	private long crcOfImage(Image image) {
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		g2.drawImage(image, 0, 0, null);
		g2.dispose();
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "bmp", baos);
			byte[] imageInBytes = baos.toByteArray();
			baos.close();
			CRC32 crc = new CRC32();
			crc.update(imageInBytes);
			return crc.getValue();
		} catch (IOException ioe) {
			System.out.println("write: " + ioe.getMessage());
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acm.clip.ClipBoardOperations#setIgnoreThisString(java.lang.String)
	 */
	public void setIgnoreThisString(String ignoreThisString) {
		this.ignoreThisString = ignoreThisString;
	}

	/**
	 * Get the currently ignored isString which will not be copied to ACM from clipboard
	 * 
	 * @return
	 */
	public String getIgnoreThisString() {
		return ignoreThisString;
	}

	/**
	 * See if looping of slots is allowed
	 * 
	 * @return
	 */
	public boolean isLooping() {
		return loop;
	}

	/**
	 * The copies to slots loops around and overwrites the first unlocked slot. If set to false then the slots will not be overwritten. Only isEmpty slots will be filled till end of slots is reached
	 * and the clipboard monitoring will effectively be stopped.
	 * 
	 * @param loop
	 */
	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	/**
	 * See if the isFile copies are supported
	 * 
	 * @return
	 */
	public boolean areFilesSupported() {
		return fileSupport;
	}

	/**
	 * If set to false then isFile copies will be ignored.
	 * 
	 * @param fileSupport
	 */
	public void setFileSupport(boolean fileSupport) {
		this.fileSupport = fileSupport;
	}

	/**
	 * See if the isImage copies are supported
	 * 
	 * @return
	 */
	public boolean areImagesSupported() {
		return imageSupport;
	}

	/**
	 * If set to false then isImage copies will be ignored
	 * 
	 * @param imageSupport
	 */
	public void setImageSupport(boolean imageSupport) {
		this.imageSupport = imageSupport;
	}

	/**
	 * Check if clipboard is no longer being monitored
	 * 
	 * @return
	 */
	public boolean hasStoppedMonitoring() {
		return stopMonitoring;
	}

	/**
	 * Setting stopMonitoring to true will effectively stop clipboard monitoring. ACM will still remain the clipboardOwner till it is closed but the clipSlots will no longer be registered.
	 * 
	 * @param stopMonitoring
	 */
	public void setStopMonitoring(boolean stopMonitoring) {
		this.stopMonitoring = stopMonitoring;
	}

	/**
	 * Use this Object to synchronize any parts of code where clips retrieval is necessary but may break due to sudden shutdown or any other condition. All data from clipboard is retrieved within
	 * blocks synchronized on this object.
	 * 
	 * @return
	 */
	public Object getSafeClipLock() {
		return safeClipLock;
	}

	/**
	 * Get the slot number where the last clips was copied
	 * 
	 * @return
	 */
	public int getLast() {
		return last;
	}

	/**
	 * Set the slot number where the last slot was copied into. This method is provided only to overwrite the current slot finding mechanism. In any case the next isEmpty slot will always be used to
	 * copy first.
	 * 
	 * @param last
	 */
	public void setLast(int last) {
		this.last = last;
	}

	/**
	 * Get the last Transferable object that copied. This Transferable is not an instance of com.acm.transferable.* objects provided. This transferable is what was retrieved from the system and may
	 * contain additional info such as MIME Types for strings etc. When pasting the last copied item it is always recommended to use the transferable instead of the object as it will be a lot more
	 * complete.
	 * 
	 * @see #setClipboard(Transferable)
	 * @return
	 */
	public Transferable getLastTransfer() {
		return lastTransfer;
	}

	/**
	 * Get the current clipSlots
	 * 
	 * @return
	 */
	public ClipSlots getClips() {
		return clipSlots;
	}

	/**
	 * Whenever a new clips is registered (not detected) the clipWriter will be called with the appropriate String, Image or List. It is upto user to define how and where the data is written.
	 * Initially clipWriter is null then no action will be taken
	 * 
	 * @param clipWriter
	 */
	public void setDataBaseWriter(ClipWriter clipWriter) {
		this.clipWriter = clipWriter;
	}

	/**
	 * Get the current ClipWriter
	 * 
	 * @return
	 */
	public ClipWriter getDataBaseWriter() {
		return clipWriter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acm.clip.ClipBoardOperations#fireClipItemProcesssed(int)
	 */
	public void fireClipItemProcesssed(int numSlot) {
		for (ClipItemListener cil : clipItemListeners) {
			cil.newClipItemProcessed(numSlot);
		}
	}

	/**
	 * Add new ClipItemListener
	 * 
	 * @param e
	 * @return
	 */
	public boolean addClipItemListener(ClipItemListener e) {
		return clipItemListeners.add(e);
	}

	/**
	 * Remove an existing ClipItemListener
	 * 
	 * @param o
	 * @return
	 */
	public boolean removeClipItemListener(Object o) {
		return clipItemListeners.remove(o);
	}

	/**
	 * Get list of current ClipItemListener(s)
	 * 
	 * @return
	 */
	public List<ClipItemListener> getClipItemListener() {
		return clipItemListeners;
	}

	/**
	 * Check if styled paste is on
	 * 
	 * @return
	 */
	public boolean isStyledPasteOn() {
		return styledPaste;
	}

	public void setStyledPaste(boolean styledPaste) {
		this.styledPaste = styledPaste;
	}

	/**
	 * Set clipboard monitoring method. false means we own the clipboard
	 * 
	 * @param pollClipboard
	 *            the pollClipboard to set
	 */
	public void setPollClipboard(boolean pollClipboard) {
		this.pollClipboard = pollClipboard;
	}

	/**
	 * Set and Re init clipboard monitoring method
	 * 
	 * @param pollClipboard
	 *            the pollClipboard to set
	 */
	public void setPollClipboardAndInit(boolean pollClipboard) {
		this.pollClipboard = pollClipboard;
		init();
	}

	/**
	 * Is polling clipboard. False means ACM owns clipboard
	 * 
	 * @return the pollClipboard
	 */
	public boolean isPollClipboard() {
		return pollClipboard;
	}

}