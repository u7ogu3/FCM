package com.acm.clip;

import java.awt.Image;
import java.util.List;

/**
 * Used by ClipboardOperator to presist any clips or do any Gui operation. Whenever a new clips is registered
 * the proper method for writing the object will be called based on its type.
 * Note: All methods are called after the clips objects have been set
 * @author Ashish Gupta, David Chang
 *
 */
public interface ClipWriter {
	/**
	 * Write String to database
	 * @param isString
	 */
	public void writeString(String string, int slotNum);

	/**
	 * Write isImage to database
	 * @param isImage
	 */
	public void writeImage(Image image, int slotNum);

	/**
	 * Write isFile list to database
	 * @param list
	 */
	public void writeFileList(List list, int slotNum);
}
