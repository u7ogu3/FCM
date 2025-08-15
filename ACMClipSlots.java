package com.acm.main.gui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileSystemView;

import com.acm.clip.ClipSlots;
import com.acm.main.ACM;
import com.acm.main.ClipProperties;
import com.acm.main.util.Network;

public class ACMClipSlots extends ClipSlots {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/* Tooltip Defaults */
	private static final int NUM_LINES_TO_SHOW = 10;
	private static final int NUM_COLS_TO_SHOW = 80;

	private static final Color HIGHLIGHT_COLOR = new Color(230, 255, 230);
	private static final Color HIGHLIGHT_BORDER_COLOR = new Color(0, 200, 0);

	private boolean lockClick = false;
	public static int previousIndex = 0;
	private transient ACMClip[] clipButtons = null;

	public ACMClipSlots(int size) {
		super(size);
		clipButtons = new ACMClip[size];
	}

	@Override
	public void init() {
		super.init();
		for (int i = 0; i < size; i++) {
			setButton(i, new ACMClip("" + i, clips[i]));
			getButton(i).setId(i);
		}
	}

	public void initForNetwork() {
		clipButtons = new ACMClip[size];
		for (int i = 0; i < size; i++) {
			setButton(i, new ACMClip("" + i, clips[i]));
			getButton(i).setId(i);
			// init transient objects
			try {
				if (getButton(i).isImage()) {
					getButton(i).getMetaData().imageObject = ACM.network.getReciever().recieveImage(getButton(i));
				}
				getButton(i).getMetaData().clip = getButton(i).makeNewTransferable();
			} catch (Exception e) {
				getButton(i).deleteItem();
			}
		}
	}

	public void setButton(int i, ACMClip button) {
		clipButtons[i] = button;
	}

	public ACMClip getButton(int i) {
		return clipButtons[i];
	}

	public void loadTmpFiles() {
		if (!Network.networked) {
			for (int i = 0; i < size; i++) {
				getButton(i).loadTmpFile();
			}
		}
	}

	public void restoreLockedItems(String[] lockedItems) {
		for (int i = 0; i < lockedItems.length; i++) {
			int slotNo = Integer.parseInt(lockedItems[i]);
			if (slotNo >= size) {
				saveLockedItems();
				break;
			}
			getButton(slotNo).setLocked(true);
		}
	}

	public boolean saveLockedItems() {
		String result = "";
		for (int i = 0; i < size; i++) {
			if (getButton(i).isLocked()) {
				if (result.length() > 0) {
					result += ",";
				}
				result += i;
			}
		}
		return ClipProperties.setProperty("isLocked", result);
	}

	@Override
	public void deleteUnlockedSlots() {
		for (int i = 0; i < size; i++) {
			ACMClip button = getButton(i);
			if (!button.isLocked()) {
				button.deleteItem();
			}
		}
	}

	@Override
	public void deleteAllSlots() {
		for (int i = 0; i < size; i++) {
			ACMClip button = getButton(i);
			if (button.isLocked()) {
				saveLockedItems();
			}
			button.deleteItem();
		}
	}

	public void changeButtonColourToGradient(GradientPaint gp) {
		for (int i = 0; i < size; i++) {
			ACMClip button = getButton(i);
			if (!button.isEmpty() && !button.isLocked() && !button.isHighlighted()) {
				button.setGradient(gp);
			}
		}
	}

	public void changeButtonColourToOld() {
		for (int i = 0; i < size; i++) {
			ACMClip button = getButton(i);
			if (!button.isEmpty() && !button.isLocked() && !button.isHighlighted()) {
				button.makeOldButton();
			}
		}
	}

	public void restoreAll() {
		for (int i = 0; i < size; i++) {
			ACMClip button = getButton(i);
			/* Try to restore this item. */
			if (button.isEmpty() && button.restoreDtmpFile()) {
				if (button == getButton(previousIndex)) {
					makeNewItem(button, true);
				} else {
					makeNewItem(button, false);
					button.makeOldButton();
				}
			}
		}
	}

	/**
	 * @param caseSensitive
	 *            whether to consider case when searching
	 */
	public void guiSearch(String keywords, boolean caseSensitive) {
		if (keywords == null) {
			return;
		}

		boolean[] itemsChanged = new boolean[size];
		if (keywords.length() == 0) {
			for (int i = 0; i < size; i++) {
				ACMClip button = getButton(i);
				if (button.isHighlighted()) {
					itemsChanged[i] = true;
					button.setHighlighted(false);
				} else {
					itemsChanged[i] = false;
				}
			}
		} else {
			/* Revert all highlighted matches. */
			for (int i = 0; i < size; i++) {
				itemsChanged[i] = false;
			}
			if (caseSensitive) {
				for (int i = 0; i < size; i++) {
					ACMClip button = getButton(i);
					if (button.isString()) {
						if (((String) button.getClipObject()).indexOf(keywords) != -1) {
							if (!button.isHighlighted()) {
								itemsChanged[i] = true;
							}
							button.setHighlighted(true);
						} else {
							if (button.isHighlighted()) {
								itemsChanged[i] = true;
							}
							button.setHighlighted(false);
						}
					}
				}
			} else {
				for (int i = 0; i < size; i++) {
					ACMClip button = getButton(i);
					if (button.isString()) {
						if (((String) button.getClipObject()).toLowerCase().indexOf(keywords.toLowerCase()) != -1) {
							if (!button.isHighlighted()) {
								itemsChanged[i] = true;
							}
							button.setHighlighted(true);
						} else {
							if (button.isHighlighted()) {
								itemsChanged[i] = true;
							}
							button.setHighlighted(false);
						}
					}
				}
			}
		}

		for (int i = 0; i < size; i++) {
			ACMClip button = getButton(i);
			if (itemsChanged[i]) {
				ACMClip thisButton = button;
				if (button.isHighlighted()) {
					// highlight the matching box
					thisButton.highlightButton(HIGHLIGHT_COLOR, HIGHLIGHT_BORDER_COLOR);
					if (thisButton == getButton(previousIndex)) {
						thisButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.WHITE));
					}
				} else {
					if (button.isLocked()) {
						thisButton.makeLockedButton();
						if (thisButton == getButton(previousIndex)) {
							thisButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.WHITE));
						}
					} else {
						thisButton.makeUnlockedButton();
						if (thisButton == getButton(previousIndex)) {
							thisButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.WHITE));
						}
					}
				}
			}
		}
	}

	public void makeNewItem(int index) {
		makeNewItem(index, true);
	}

	// currently publicly not used since restore methods are in this class
	public void makeNewItem(int index, boolean changeBorder) {
		makeNewItem(getButton(index), changeBorder);
	}

	private void makeNewItem(ACMClip button, boolean changeBorder) {
		// Update the new item indicator.
		if (changeBorder) {
			// changes previous button's border/colour
			ACMClip previousButton = getButton(previousIndex);
			if (previousButton != null && previousButton.isEnabled()) {
				if (!(previousButton.isLocked() && previousButton.makeLockedButton())) {
					previousButton.makeOldButton();
				}
			}
		}

		/*
		 * Prepare the tool tip. Trim to show only a certain number of lines and columns.
		 */
		String temp = "", trimmed = "";
		Object clip = button.getClipObject();
		if (button.isString()) {
			temp = (String) clip;
			if (button.getIcon() != null) {
				button.setIcon(null);
				button.setText("" + button.getId());
			}
		} else if (button.isImage()) {
			temp = "Image";
			Image image = (Image) clip;
			image = ((BufferedImage) clip).getScaledInstance(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT, Image.SCALE_SMOOTH);
			button.setIcon(new ImageIcon(image));
			button.setText(null);
		} else if (button.isFile()) {
			List list = (List) clip;
			for (int i = 0; i < list.size(); i++) {
				temp += ((File) list.get(i)).getName() + "\n";
			}
			button.setText(null);

			// Set Icon
			if (Network.networked) {
				button.setIcon(ACMGUI.folderIcon);
			} else {
				if (list.size() != 1) {
					button.setIcon(ACMGUI.folderIcon);
				} else {
					try {
						FileSystemView fsw = FileSystemView.getFileSystemView();
						button.setIcon(fsw.getSystemIcon((File) list.get(0)));
					} catch (Exception e) {
						button.setIcon(ACMGUI.folderIcon);
					}
				}
			}
		}
		int index;
		for (int i = 0; i < NUM_LINES_TO_SHOW; i++) {
			if ((index = temp.indexOf("\n")) == -1) {
				index = temp.length();
				trimmed += temp.substring(0, (index > NUM_COLS_TO_SHOW ? NUM_COLS_TO_SHOW : index)) + "\n";
				break;
			}
			trimmed += temp.substring(0, (index > NUM_COLS_TO_SHOW ? NUM_COLS_TO_SHOW : index)) + "\n";
			temp = temp.substring(index + 1);
		}

		/* Make the isString multi-lined. */
		String html = trimmed.substring(0, trimmed.lastIndexOf("\n"));
		html = html.replaceAll("&", "&amp;");
		html = html.replaceAll("<", "&lt;");
		html = html.replaceAll(">", "&gt;");
		html = html.replaceAll(" ", "&nbsp;");
		html = html.replaceAll("\"", "&quot;");
		html = html.replaceAll("/", "&#47;");
		html = html.replaceAll("\t", "&#9;");
		html = html.replaceAll("\n", "<br />");
		button.setToolTipText("<html>" + html + "</html>");
		button.setEnabled(true);
		if (changeBorder) {
			if (button.isLocked()) {
				button.makeLockedButton();
				button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.WHITE));
			} else {
				button.makeNewButton();
			}
			previousIndex = button.getId();
			ClipProperties.setProperty("previousIndex", "" + previousIndex);
		}
	}

	private void performAltAction(String search) {
		// Determine if this is a isFile
		try {
			File f = new File(search);
			if (f.exists()) {
				Desktop.getDesktop().open(f);
				return;
			}
		} catch (Exception e) {
		}
		if (search.startsWith("http://") || search.startsWith("www.")) {
			try {
				Desktop.getDesktop().browse(new URI(search));
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error attempting to launch web browser" + ":\n" + e.getLocalizedMessage());
			}
		} else {
			Scanner scanner = new Scanner(search);
			try {
				String google = "http://www.google.com/search?q=" + URLEncoder.encode(scanner.nextLine(), "UTF-8") + "&hl=en&ie=UTF-8&oe=UTF8";
				Desktop.getDesktop().browse(new URI(google));
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error attempting to launch web browser" + ":\n" + e.getLocalizedMessage());
			}
		}
	}

	public void setupButtons(MouseMotionListener mml, JPanel clipboardItemButtonsPanel) {
		for (int i = 0; i < size; i++) {
			ACMClip button = getButton(i);
			setupButton(button);
			button.addMouseMotionListener(mml);
			clipboardItemButtonsPanel.add(button);
		}
	}

	private void setupButton(final ACMClip button) {
		button.setSize(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT);
		button.setMaximumSize(new Dimension(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT));
		button.setMinimumSize(new Dimension(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT));
		button.setPreferredSize(new Dimension(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT));
		button.setBorder(ACMClip.BUTTON_BORDER);

		button.setFont(ACMGUI.font);
		if (!button.isEmpty()) {
			makeNewItem(button, true);
		} else {
			button.setEnabled(false);
		}

		if (button.isLocked()) {
			button.makeLockedButton();
		}

		button.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent arg0) {
			}

			public void mouseEntered(MouseEvent arg0) {
				// Releases the current focus back to the previously
				// stolen window, only if the textarea currently does not
				// have the focus.
				if (!ACMGUI.ta.hasFocus()) {
					ACMGUI.mainDialog.setFocusableWindowState(false);
				}
			}

			public void mouseExited(MouseEvent arg0) {
			}

			public void mousePressed(MouseEvent me) {
				ACMGUI.pressed = me;
			}

			public void mouseReleased(MouseEvent e) {
				if (!ACMGUI.dragged) {
					ACMClip thisButton = (ACMClip) e.getSource();
					boolean altPressed = e.isAltDown() && !Network.networked;
					boolean shiftPressed = e.isShiftDown() && !Network.networked;
					boolean ctrlPressed = e.isControlDown() && !Network.networked;

					/*
					 * Regular click: copy to clipboard (and paste it, based on property)
					 */
					// 0 - Nothing is on
					// 1 - Alt + left click: execute pattern matching
					// filters (e.g. if regex matches, open FireFox
					// to this specific site; for now, search Google)
					// 2 - Ctrl + left click: paste it, but
					// DON'T copy to clipboard
					// 3 - Shift + left click: 'sticky' (lock
					// the current button - don't allow overwriting)
					// Right click again on button should restore deleted
					// item.
					if (e.getButton() == MouseEvent.BUTTON1 && thisButton.isEnabled()) {
						if (shiftPressed) {
							if (!button.isLocked()) {
								thisButton.makeLockedButton();
								if (thisButton == getButton(previousIndex)) {
									thisButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.WHITE));
								}
								button.setLocked(true);
								saveLockedItems();
							} else {
								thisButton.makeUnlockedButton();
								if (thisButton == getButton(previousIndex)) {
									thisButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.WHITE));
								}
								button.setLocked(false);
								saveLockedItems();
							}
						} else if (altPressed) {
							if (button.isString()) {
								performAltAction((String) button.getClipObject());
							} else if (button.isFile()) {
								List list = (List) button.getClipObject();
								File f = (File) list.get(0);
								if (list.size() == 1 && f.exists()) {
									try {
										Desktop.getDesktop().open(f);
									} catch (IOException e1) {
										e1.printStackTrace();
									}
								}
							}
						} else {
							if (!lockClick) {
								lockClick = true;
								if (button.getClipObject() != null) {
									Transferable tempT = null;
									if (ctrlPressed) {
										if (ACM.acm.getLastTransfer() == null || ACM.acm.hasStoppedMonitoring()) {
											ACM.acm.getClipboardContents();
										}
										tempT = ACM.acm.getLastTransfer();
									}
									ACM.acm.setClipboardWithSlot(button.getId());

									if (ACMGUI.keyPasteEnabled || ctrlPressed) {
										ACMGUI.pasteClipboard(true, button);
									}

									if (ctrlPressed && tempT != null) {
										// restore clipboard contents (only
										// when the previous clipboard was
										// not isEmpty)
										try {
											ACM.acm.setClipboard(tempT);
										} catch (RuntimeException e1) {
											int i;
											for (i = 0; i < 10; i++) {
												try {
													Thread.sleep(50);
													ACM.acm.setClipboard(tempT);
													break;
												} catch (Exception e2) {
												}
											}
											if (i == 10) {
												System.out.println("Setting Clipboard Failed. Here are the previous contents\n");
												boolean hasTransferableText = (tempT != null) && tempT.isDataFlavorSupported(DataFlavor.stringFlavor);
												if (hasTransferableText) {
													try {
														System.out.println((String) tempT.getTransferData(DataFlavor.stringFlavor));
													} catch (Exception ex) {
													}
												}
											}
										}
									} else {
										makeNewItem(button, true);
									}
								}
								lockClick = false;
							}
						}
					} else if (e.getButton() == MouseEvent.BUTTON3) {
						if (!Network.networked) {
							if (thisButton.isEnabled()) {
								/* Delete this item. */
								saveLockedItems();
								button.deleteItem();
							} else {
								/* Try to restore this item. */
								if (button.restoreDtmpFile()) {
									if (thisButton == getButton(previousIndex)) {
										makeNewItem(button, true);
									} else {
										makeNewItem(button, false);
										thisButton.makeOldButton();
									}
								}
							}
						}
					}
				}
				ACMGUI.fireMouseDoneDragging();
			}
		});
	}
}
