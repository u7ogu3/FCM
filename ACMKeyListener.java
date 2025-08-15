package com.acm.main.keyboard;

// Test.java

import java.awt.Point;
import java.awt.event.KeyEvent;

import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import com.acm.keyboard.win.KeyboardEvent;
import com.acm.keyboard.win.KeyboardEventListener;
import com.acm.main.ACM;
import com.acm.main.gui.ACMClip;
import com.acm.main.gui.ACMClipSlots;
import com.acm.main.gui.ACMGUI;

public class ACMKeyListener implements KeyboardEventListener {
	public static final int WINDOWS_KEY = 91;
	public static boolean useShortcuts = true;

	boolean ctrlPressed = false;
	boolean winPressed = false;
	int lastPasted = ACMClipSlots.previousIndex;
	int MINUS = 0xBD;
	int PLUS = 0xBB;
	Popup tooltipPopup = null;
	int lastToolTip = -1;
	long previousKc = -1;

	public void GlobalKeyPressed(KeyboardEvent event) {
		long kc = event.getVirtualKeyCode();
		if (tooltipPopup != null) {
			tooltipPopup.hide();
			tooltipPopup = null;
		}
		if (!ctrlPressed && kc == KeyEvent.VK_CONTROL) {
			ctrlPressed = true;
		} else if (!winPressed && kc == WINDOWS_KEY) {
			winPressed = true;
		} else if (winPressed && ctrlPressed) {
			if (kc == KeyEvent.VK_BACK_QUOTE) {
				if (previousKc == KeyEvent.VK_BACK_QUOTE) {
					getNextSlot(true);
				}
				final ACMClip button = ACM.acm.getClips().getButton(lastPasted);
				ACM.acm.getClips().makeNewItem(lastPasted);
				String previousTip = button.getToolTipText();
				final JToolTip tooltip = button.createToolTip();
				tooltip.setTipText(previousTip);
				tooltip.setLocation(button.getToolTipLocation(null));
				Point location = tooltip.getLocation();
				location.translate(button.getLocationOnScreen().x, button.getLocationOnScreen().y);
				tooltipPopup = PopupFactory.getSharedInstance().getPopup(button, tooltip, location.x, location.y);
				tooltipPopup.show();
				if (lastToolTip == -1) {
					lastToolTip = lastPasted;
				}
			} else if (kc >= KeyEvent.VK_0 && kc <= KeyEvent.VK_9 || kc == MINUS || kc == PLUS || (kc >= 0x60 && kc <= 0x69)) {
				int slotNo = 0;
				if (kc == MINUS) {
					if (previousKc == MINUS) {
						slotNo = getNextSlot(false);
					} else {
						slotNo = lastPasted;
					}
				} else if (kc == PLUS) {
					if (previousKc == PLUS) {
						slotNo = getNextSlot(true);
					} else {
						slotNo = lastPasted;
					}
				} else if ((kc >= 0x60 && kc <= 0x69)) {
					slotNo = (int) (kc - 0x60);
				} else {
					slotNo = (int) (kc - KeyEvent.VK_0);
				}
				if (slotNo < ACM.acm.getSize() && !ACM.acm.getClips().getButton(slotNo).isEmpty()) {
					ACM.acm.setClipboardWithSlot(slotNo);
					ACM.acm.getClips().makeNewItem(slotNo);
					if (useShortcuts) {
						ACMGUI.pasteClipboard(false, ACM.acm.getClips().getButton(slotNo));
					}
					lastPasted = slotNo;
					lastToolTip = lastPasted;
				}
			}
		}
		previousKc = kc;
	}

	private int getNextSlot(boolean forward) {
		if (forward) {
			for (int i = 0; i < ACM.acm.getSize(); i++) {
				lastPasted++;
				if (lastPasted == ACM.acm.getSize()) {
					lastPasted = 0;
				}
				if (!ACM.acm.getClips().getButton(lastPasted).isEmpty()) {
					break;
				}
			}
		} else {
			for (int i = 0; i < ACM.acm.getSize(); i++) {
				lastPasted--;
				if (lastPasted < 0) {
					lastPasted = ACM.acm.getSize() - 1;
				}
				if (!ACM.acm.getClips().getButton(lastPasted).isEmpty()) {
					break;
				}
			}
		}
		return lastPasted;
	}

	public void GlobalKeyReleased(KeyboardEvent event) {
		long kc = event.getVirtualKeyCode();
		if (kc == KeyEvent.VK_CONTROL) {
			ctrlPressed = false;
		} else if (kc == WINDOWS_KEY) {
			winPressed = false;
		}
		if ((!winPressed || !ctrlPressed) && lastToolTip != -1) {
			if (tooltipPopup != null) {
				tooltipPopup.hide();
				tooltipPopup = null;
			}
			lastPasted = lastToolTip;
			lastToolTip = -1;
			ACM.acm.getClips().makeNewItem(lastPasted);
		}
	}
}
