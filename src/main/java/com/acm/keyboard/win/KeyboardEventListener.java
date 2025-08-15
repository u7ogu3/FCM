package com.acm.keyboard.win;

// KeyboardEventListener.java
import java.util.EventListener;
 
public interface KeyboardEventListener extends EventListener
{
	public void GlobalKeyPressed( KeyboardEvent event );
	public void GlobalKeyReleased( KeyboardEvent event );
}
