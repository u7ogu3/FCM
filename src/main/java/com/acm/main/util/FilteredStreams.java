package com.acm.main.util;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.acm.main.gui.ACMGUI;

public class FilteredStreams {

	public static class FilteredInputStream extends FilterInputStream {
		public static Object lockTemp = new Object();

		public FilteredInputStream(InputStream arg0) {
			super(arg0);
		}

		@Override
		public int read(byte[] arg0) throws IOException {
			return super.read(arg0);
		}

		@Override
		public int read(byte[] arg0, int arg1, int arg2) throws IOException {
			int bytesRead = 0;
			try {
				for (bytesRead = 0; bytesRead < arg2; bytesRead++) {
					arg0[bytesRead] = (byte) read();
				}
			} catch (Exception e) {
			}
			return bytesRead;
		}

		@Override
		public int read() throws IOException {
			char result;
			synchronized (lockTemp) {
				if (FilteredStreams.unprocessedCharacters.length() == 0) {
					try {
						lockTemp.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				result = FilteredStreams.unprocessedCharacters.charAt(0);
				if (result == '\n') {
					FilteredStreams.unprocessedCharacters = "\r" + FilteredStreams.unprocessedCharacters.substring(1);
					return '\r';
				} else if (result == '\r') {
					FilteredStreams.unprocessedCharacters = "" + FilteredStreams.unprocessedCharacters.substring(1);
					throw new IOException();
				} else {
					FilteredStreams.unprocessedCharacters = FilteredStreams.unprocessedCharacters.substring(1);
				}
			}
			return result;
		}

		public static void notifyLock() {
			synchronized (lockTemp) {
				lockTemp.notifyAll();
			}
		}
	}

	public static class FilteredStream extends FilterOutputStream {
		public FilteredStream(OutputStream aStream) {
			super(aStream);
		}

		public void write(byte b[]) throws IOException {
			String aString = new String(b, "UTF8");

			ACMGUI.output.append(aString);
			ACMGUI.output.setCaretPosition(ACMGUI.output.getText().length());

		}

		public void write(byte b[], int off, int len) throws IOException {
			String aString = new String(b, off, len, "UTF8");
			ACMGUI.output.append(aString);
			ACMGUI.output.setCaretPosition(ACMGUI.output.getText().length());
		}
	}

	public static String unprocessedCharacters = new String("");

}
