package com.acm.transferable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * Supports text/html and text/rtf with plain text
 * 
 * @author ashishg
 * 
 */
public class StringSelection implements Transferable {
	private static final String TEXT_RTF = "text/rtf; class=java.io.InputStream";
	private static final String TEXT_HTML = "text/html; class=java.io.Reader";
	private String string;
	private byte [] rtf;
	private String html;

	public StringSelection(String string) {
		this.string = string;
		this.rtf = null;
		this.html = null;
	}

	public StringSelection(String string, byte [] rtf) {
		this.string = string;
		this.rtf = rtf;
		this.html = null;
	}

	public StringSelection(String string, String html) {
		this.string = string;
		this.html = html;
		this.rtf = null;
	}

	// Returns supported flavors
	public DataFlavor[] getTransferDataFlavors() {
		try {
			if (html != null) {
				return new DataFlavor[] { new DataFlavor(TEXT_HTML), DataFlavor.stringFlavor };
			} else if (rtf != null) {
				return new DataFlavor[] { new DataFlavor(TEXT_RTF), DataFlavor.stringFlavor };
			}
		} catch (ClassNotFoundException e) { // these 2 should be supported
		}
		return new DataFlavor[] { DataFlavor.stringFlavor };
	}

	// Returns true if flavor is supported
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return DataFlavor.stringFlavor.equals(flavor) || (flavor.getMimeType().indexOf(TEXT_RTF) != -1 && rtf != null) || (flavor.getMimeType().indexOf(TEXT_HTML) != -1 && html != null);
	}

	// Returns isImage
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (flavor.getMimeType().indexOf(TEXT_RTF) != -1 && rtf != null) {
			return new ByteArrayInputStream(rtf);
		} else if (flavor.getMimeType().indexOf(TEXT_HTML) != -1 && html != null) {
			return new StringReader(html);
		} else if (DataFlavor.stringFlavor.equals(flavor)) {
			return string;
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}
}