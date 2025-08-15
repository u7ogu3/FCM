package com.acm.clip;

/**
 * Listens to clips items that are registered.
 * 
 * @author Ashish Gupta, David Change
 * 
 */
public interface ClipItemListener {
	/**
	 * Called when a new item is registered.
	 * @param numSlot TODO
	 */
	public void newClipItemProcessed(int numSlot);
}
