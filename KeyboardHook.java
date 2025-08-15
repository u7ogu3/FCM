package com.acm.keyboard.win;

public class KeyboardHook {

	public PollThread poll = null;

	public KeyboardHook() {
		poll = new PollThread(this);
		poll.start();
	}

	protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();

	public void addEventListener(KeyboardEventListener listener) {
		listenerList.add(KeyboardEventListener.class, listener);
	}

	public void removeEventListener(KeyboardEventListener listener) {
		listenerList.remove(KeyboardEventListener.class, listener);
	}

	void keyPressed(KeyboardEvent event) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == KeyboardEventListener.class) {
				((KeyboardEventListener) listeners[i + 1]).GlobalKeyPressed(event);
			}
		}
	}

	void keyReleased(KeyboardEvent event) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == KeyboardEventListener.class) {
				((KeyboardEventListener) listeners[i + 1]).GlobalKeyReleased(event);
			}
		}
	}

	public class PollThread extends Thread {
		public native void checkKeyboardChanges();

		public native void cleanup();

		private KeyboardHook kbh;

		public PollThread(KeyboardHook kh) {
			kbh = kh;
			System.loadLibrary("syshook");
		}

		public void run() {
			try {
				for (;;) {
					checkKeyboardChanges();
					try {
						Thread.sleep(2);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (UnsatisfiedLinkError ule) {
				System.out.println("Keyboard Library failed to load. System may require restart");
			}
		}

		void Callback(boolean ts, int vk, boolean ap, boolean ek) {
			KeyboardEvent event = new KeyboardEvent(this, ts, vk, ap, ek);
			if (ts) {
				kbh.keyPressed(event);
			} else {
				kbh.keyReleased(event);
			}
		}
	}
}