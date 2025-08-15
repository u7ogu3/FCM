package com.acm.clip;

import java.awt.datatransfer.Transferable;

public interface ClipBoardOperations {
	
	/**
	 * Set the clipboard with given String
	 * 
	 * @param aString
	 */
	public abstract void setClipboardWithString(String aString);
	
	
	/**
	 * Call this method before setting the clipboard contents if the isString is
	 * not be registered by ACM.
	 * 
	 * @param
	 */
	public void setIgnoreThisString(String ignoreThisString);

	/**
	 * Set the clipboard the given Transferable. Use FileSelection,
	 * StringSelection ,ImageSelection Objects
	 * 
	 * @param aString
	 */
	public abstract void setClipboard(Transferable aString);

	/**
	 * Get the contents of clipboard as String, Image or List<File>. This method
	 * will also set ACM.lastTransfer Transferable
	 * 
	 * @return
	 */
	public abstract Object getClipboardContents();
	
	/**
	 * Set clipboard with contents of the slot
	 * @param slotNum
	 */
	public void setClipboardWithSlot(int slotNum);
	
	/**
	 * Fired when a NEW clips item is registered (not detected).
	 */
	public void fireClipItemProcesssed(int numSlot);

}