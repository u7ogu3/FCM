package com.acm.main.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.undo.UndoManager;

import com.acm.clip.Clip;
import com.acm.main.ACM;
import com.acm.main.ClipProperties;
import com.acm.transferable.FileSelection;
import com.acm.transferable.ImageSelection;
import com.acm.transferable.StringSelection;

public class ACMClip extends JButton {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final Color LOCKED_COLOR = new Color(247, 231, 200);
	public static final Border BUTTON_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(238, 238, 238)), BorderFactory.createEmptyBorder(1, 1, 1, 1));

	public static int BUTTON_WIDTH = 25;
	public static int BUTTON_HEIGHT = 25;
	public static Color NORMAL_BUTTON_COLOR = null;

	private Clip metaData = null;
	private boolean highlighted = false;
	private GradientPaint gradient = null;
	private transient MouseListener toolTipMouseListener;
	private transient boolean unlockAfterEditMode = false;

	public ACMClip(String string, Clip metaData) {
		super(string);
		this.metaData = metaData;
		toolTipMouseListener = createToolTipMouseListener();
	}

	public Clip getMetaData() {
		return metaData;
	}

	public boolean isHighlighted() {
		return highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
	}

	public boolean isLocked() {
		return metaData.isLocked;
	}

	public void setLocked(boolean locked) {
		unlockAfterEditMode = false;
		this.metaData.isLocked = locked;
	}

	public Transferable makeNewTransferable() {
		if (isString()) {
			return new StringSelection(metaData.stringObject);
		} else if (isFile()) {
			return new FileSelection(metaData.fileObject);
		} else if (isImage()) {
			return new ImageSelection(metaData.imageObject);
		}
		return null;
	}

	public Transferable getTransferable() {
		return metaData.clip;
	}

	public Object getClipObject() {
		if (!isEmpty()) {
			try {
				if (isString()) {
					return getTransferable().getTransferData(DataFlavor.stringFlavor);
				} else if (isImage()) {
					return getTransferable().getTransferData(DataFlavor.imageFlavor);
				} else if (isFile()) {
					return getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
				}
			} catch (Exception e) {
				// Never Thrown but we will return null either way.
			}
		}
		return null;
	}

	public void setNewString(StringSelection stringSelection, String mimeType) {
		if (stringSelection != null) {
			String string;
			try {
				string = (String) stringSelection.getTransferData(DataFlavor.stringFlavor);
				metaData.setNewString(string, stringSelection);
			} catch (Exception e) {// Since local StringSelection. This cannot happen
			}
		}
		ClipProperties.setProperty("clip" + metaData.id + ".tmp", "String" + (mimeType != null ? ("|" + mimeType) : ""));
	}

	public void setNewImage(Image data, boolean setTransferable) {
		if (setTransferable) {
			ImageSelection imageSel = new ImageSelection(data);
			metaData.setNewImage(data, imageSel);
		}
		ClipProperties.setProperty("clip" + metaData.id + ".tmp", "Image");
	}

	public void setNewFile(List<File> data, boolean setTransferable) {
		if (setTransferable) {
			FileSelection fileSel = new FileSelection(data);
			metaData.setNewFile(data, fileSel);
		}
		ClipProperties.setProperty("clip" + metaData.id + ".tmp", "List");
	}

	public boolean isEmpty() {
		return metaData.isEmpty;
	}

	public boolean isString() {
		return metaData.isString;
	}

	public boolean isImage() {
		return metaData.isImage;
	}

	public boolean isFile() {
		return metaData.isFile;
	}

	public int getId() {
		return metaData.id;
	}

	public void setId(int id) {
		this.metaData.id = id;
	}

	public void setGradient(GradientPaint gradient) {
		this.gradient = gradient;
		this.repaint();
	}

	public GradientPaint getGradient() {
		return gradient;
	}

	@Override
	public Point getToolTipLocation(MouseEvent event) {
		String previousTip = getToolTipText();
		final JToolTip tooltip = createToolTip();
		tooltip.setTipText(previousTip);
		final int x, y;
		if (ACMGUI.verticalMode) {
			if (tooltip.getPreferredSize().getHeight() > ACMGUI.screenSize.getHeight() - getLocationOnScreen().y) {
				y = ((int) ACMGUI.screenSize.getHeight()) - (int) tooltip.getPreferredSize().getHeight() - getLocationOnScreen().y;
			} else {
				y = 0;
			}
			if (tooltip.getPreferredSize().getWidth() > ACMGUI.mainDialog.getX()) {
				x = ACMGUI.WIDTH;
			} else {
				x = -1 * (int) tooltip.getPreferredSize().getWidth();
			}
		} else {
			if (tooltip.getPreferredSize().getWidth() > (ACMGUI.screenSize.getWidth() - getLocationOnScreen().x)) {
				x = ((int) ACMGUI.screenSize.getWidth()) - (int) tooltip.getPreferredSize().getWidth() - getLocationOnScreen().x;
			} else {
				x = 0;
			}
			if (tooltip.getPreferredSize().getHeight() > (ACMGUI.screenSize.getHeight() - ACMGUI.mainDialog.getY() + ACMGUI.HEIGHT)) {
				y = -1 * (int) tooltip.getPreferredSize().getHeight();
			} else {
				y = ACMGUI.HEIGHT;
			}
		}
		return new Point(x, y);
	}

	@Override
	public JToolTip createToolTip() {
		JToolTip tip = super.createToolTip();
		tip.addMouseListener(toolTipMouseListener);
		return tip;
	}

	private MouseListener createToolTipMouseListener() {
		return new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				ToolTipManager.sharedInstance().mouseClicked(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
				ToolTipManager.sharedInstance().mouseExited(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				final JToolTip tip = (JToolTip) e.getSource();
				int width = tip.getWidth();
				int height = tip.getHeight();
				int locX = tip.getLocationOnScreen().x;
				int locY = tip.getLocationOnScreen().y;
				Color color = tip.getBackground();
				ToolTipManager.sharedInstance().mousePressed(e);
				setupToolTipEditor(width, height, locX, locY, color, false);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				ToolTipManager.sharedInstance().mouseReleased(e);
			}

		};
	}

	private void setupToolTipEditor(final int width, final int height, final int locX, final int locY, final Color color, final boolean raw) {
		if (isString()) {
			@SuppressWarnings("serial")
			class JZoomablePane extends JEditorPane {
				public double scaleX = 1;
				public double scaleY = 1;

				public void paint(Graphics g) {
					Graphics2D gd = (Graphics2D) g;
					gd.scale(scaleX, scaleY);
					super.paint(gd);
				}

			}
			final JZoomablePane pane = new JZoomablePane();
			pane.setBackground(color);
			pane.setEditable(true);
			pane.setAutoscrolls(true);
			Transferable t = getMetaData().clip;
			DataFlavor[] flavors = t.getTransferDataFlavors();
			DataFlavor flavor = DataFlavor.selectBestTextFlavor(flavors);
			final String type = ClipProperties.getProperty("clip" + metaData.id + ".tmp", "String");
			try {
				if (!raw) {
					if (type.indexOf("rtf") != -1) {
						pane.setEditorKit(new RTFEditorKit());
						pane.read(flavor.getReaderForText(t), null);
					} else if (type.indexOf("html") != -1) {
						pane.setEditorKit(new HTMLEditorKit());
						try {
							pane.read(flavor.getReaderForText(t), null);
							((HTMLDocument) pane.getDocument()).setPreservesUnknownTags(false);
							String text = pane.getText();
							// Remove stupid fragment lines
							if (text.indexOf("<!--StartFragment-->") != -1 && text.indexOf("<!--EndFragment-->") != -1) {
								text = text.replace("<!--StartFragment-->", "");
								text = text.replace("<!--EndFragment-->", "");
							}
							// Remove stupid stuff before html
							int htmlIndex;
							if ((htmlIndex = text.lastIndexOf("<html>")) != 0) {
								text = text.substring(htmlIndex, text.length());
							}
							pane.setText(text.toString());
						} catch (Exception e1) {
							// Hmm couldnt read HTML properly. Lets try using the Document directly with IgnoreCharset
							HTMLEditorKit kit = new HTMLEditorKit();
							HTMLDocument doc = new HTMLDocument();
							doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
							doc.setPreservesUnknownTags(false);
							try {
								kit.read(flavor.getReaderForText(t), doc, 0);
								pane.setDocument(doc);
							} catch (Exception e2) {
								// hmm that didnt work either. lets just use plain text
								System.out.println("Some error getting HTML, Using normal text");
								pane.read(DataFlavor.getTextPlainUnicodeFlavor().getReaderForText(t), new String());
							}
						}
					} else {
						pane.read(DataFlavor.getTextPlainUnicodeFlavor().getReaderForText(t), new String());
					}
				} else {
					pane.read(flavor.getReaderForText(t), null);
				}
			} catch (Exception e2) {
				// If everything fails just set text
				pane.setText(getMetaData().stringObject);
			}
			final CUndecoratedResizeableDialog jd = new CUndecoratedResizeableDialog(10, 10);
			jd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			jd.setUndecorated(true);
			jd.setFocusable(true);
			jd.setFocusableWindowState(true);
			if (ACMGUI.isAlwaysOnTop) {
				jd.setAlwaysOnTop(true);
			}
			jd.setSize(width + 20, height + 20);
			jd.setLocation(locX, locY);
			final JScrollPane scrollPane = new JScrollPane(pane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			final UndoManager undo = new UndoManager();
			jd.getContentPane().add(scrollPane);
			jd.setVisible(true);
			if (!isLocked()) {
				setLocked(true);
				unlockAfterEditMode = true;
			}

			pane.grabFocus();
			pane.setCaretPosition(pane.viewToModel(MouseInfo.getPointerInfo().getLocation()));
			pane.setCursor(new Cursor(Cursor.TEXT_CURSOR));
			pane.getDocument().addUndoableEditListener(new UndoableEditListener() {

				@Override
				public void undoableEditHappened(UndoableEditEvent e) {
					pane.putClientProperty("askOnClose", "true");
					undo.addEdit(e.getEdit());
				}

			});

			pane.addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent e) {
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						JToolTip tip = new JToolTip();
						tip
								.setTipText("<html><center><b>Quick Help</b>" +
										"<br />Undo: Ctrl + Z" +
										"<br />Redo: Ctrl + Y" +
										"<br />Save to this Slot: Ctrl + S" +
										"<br />Save to Clipboard/New Slot: Ctrl + D" +
										"<br />Save to Clipboard/New Slot (PLAIN TEXT): Ctrl + E" +
										(!raw?"<br />Switch to Raw Rendering: Ctrl + R":"<br />Switch to Styled Rendering: Ctrl + R") +
										"<br />Resize: Ctrl + Mouse Wheel, drag black edges" +
										//"<br />Zoom: Alt + Mouse Wheel" +
										"<br />Exit: Click outside edit box</center></html>");
						
						final Popup popup = PopupFactory.getSharedInstance().getPopup(pane, tip, e.getLocationOnScreen().x, e.getLocationOnScreen().y);
						popup.show();
						tip.addMouseListener(new MouseListener() {

							@Override
							public void mouseClicked(MouseEvent e) {
							}

							@Override
							public void mouseEntered(MouseEvent e) {
							}

							@Override
							public void mouseExited(MouseEvent e) {
								popup.hide();
							}

							@Override
							public void mousePressed(MouseEvent e) {
							}

							@Override
							public void mouseReleased(MouseEvent e) {
							}

						});
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
				}

			});

			pane.addFocusListener(new FocusListener() {

				@Override
				public void focusGained(FocusEvent e) {
				}

				@Override
				public void focusLost(FocusEvent e) {
					if (Boolean.parseBoolean((String) pane.getClientProperty("askOnClose"))) {
						int option = JOptionPane.showConfirmDialog(jd, "Seems like there are some unsaved changes.\nWould you like to save in clipboard?", "Save?", JOptionPane.YES_NO_CANCEL_OPTION);
						if (option == JOptionPane.YES_OPTION) {
							copyPaneToClipboard(pane, raw);
						} else if (option == JOptionPane.CANCEL_OPTION) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									pane.grabFocus();
								}
							});
							return;
						}
					}
					if (unlockAfterEditMode) {
						setLocked(false);
					}

					if (e != null && e.getOppositeComponent() != null && e.getOppositeComponent().equals(scrollPane)) {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								if (!raw) {
									setupToolTipEditor(width, height, locX, locY, color, true);
								} else {
									setupToolTipEditor(width, height, locX, locY, color, false);
								}
							}

						});
					}

					jd.setVisible(false);
					jd.dispose();
				}

			});

			pane.addKeyListener(new KeyListener() {

				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_Z && e.isControlDown()) {
						if (undo.canUndo()) {
							undo.undo();
						}
					}
					if (e.getKeyCode() == KeyEvent.VK_Y && e.isControlDown()) {
						if (undo.canRedo()) {
							undo.redo();
						}
					}
					if (e.getKeyCode() == KeyEvent.VK_D && e.isControlDown()) {
						pane.putClientProperty("askOnClose", "false");
						copyPaneToClipboard(pane, raw);
					}
					if (e.getKeyCode() == KeyEvent.VK_S && e.isControlDown()) {
						try {
							String mimeType = null;
							if (type.indexOf("rtf") != -1) {
								mimeType = "rtf";
							} else if (type.indexOf("html") != -1) {
								mimeType = "html";
							}
							setNewString((StringSelection) getPaneTransferable(pane, raw), mimeType);
							ACM.acm.getClips().makeNewItem(getMetaData().id, false);
							ACM.getACMClipWriter().writeString(getMetaData().stringObject, getMetaData().id);
							pane.putClientProperty("askOnClose", "false");
						} catch (Exception ex) {
							System.out.println("Could Not Save: " + ex.getMessage());
						}
					}
					if (e.getKeyCode() == KeyEvent.VK_R && e.isControlDown()) {
						scrollPane.grabFocus();
					}
					if (e.getKeyCode() == KeyEvent.VK_E && e.isControlDown()) {
						try {
							ACM.acm.setClipboardWithString((String)getPaneTransferable(pane, raw).getTransferData(DataFlavor.stringFlavor));
						} catch (Exception e1) {
							System.out.println("Woops, Something wrong happened while saving to clipboard: " +  e1.getMessage());
						} 
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {

				}

				@Override
				public void keyTyped(KeyEvent e) {
				}

			});

			pane.addMouseWheelListener(new MouseWheelListener() {

				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					if (e.isControlDown()) {
						double wheel = e.getWheelRotation();
						jd.setSize((int) (jd.getSize().width * (1 + (wheel / 10))), (int) (jd.getSize().height * (1 + (wheel / 10))));
					} else if (e.isAltDown()) {
						//double wheel = e.getWheelRotation();
						//pane.scaleX = pane.scaleX * (1 + (wheel / 10));
						//pane.scaleY = pane.scaleY * (1 + (wheel / 10));
						//pane.repaint();
					}
				}

			});
		}
	}

	private Transferable getPaneTransferable(final JEditorPane pane, boolean raw) throws Exception {
		String type = ClipProperties.getProperty("clip" + metaData.id + ".tmp", "String");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		pane.getEditorKit().write(baos, pane.getDocument(), 0, pane.getDocument().getLength());
		final byte[] bytes = baos.toByteArray();
		baos.close();
		baos = new ByteArrayOutputStream();
		final String plain;
		if (!raw) {
			pane.getEditorKitForContentType("text/plain").write(baos, pane.getDocument(), 0, pane.getDocument().getLength());
		} else {
			// this is raw data so it must be rendered first to get plain data
			JEditorPane newPane = new JEditorPane();
			if (type.indexOf("rtf") != -1) { // Since editor pane hates RTF
				newPane.setEditorKit(new RTFEditorKit());
			} else if (type.indexOf("html") != -1) {
				newPane.setEditorKit(new HTMLEditorKit());
			} 
			newPane.setText(new String(bytes));
			newPane.getEditorKitForContentType("text/plain").write(baos, newPane.getDocument(), 0, newPane.getDocument().getLength());
		}
		plain = baos.toString();
		StringSelection selection;
		if (type.indexOf("rtf") != -1) { // Since editor pane hates RTF
			selection = new StringSelection(plain, bytes);
		} else if (type.indexOf("html") != -1) {
			selection = new StringSelection(plain, new String(bytes));
		} else {
			selection = new StringSelection(plain);
		}
		return selection;
	}

	private void copyPaneToClipboard(JEditorPane pane, boolean raw) {
		try {
			ACM.acm.setClipboard(getPaneTransferable(pane, raw));
		} catch (Exception ex) {
			try {
				int post = pane.getCaretPosition();
				pane.selectAll();
				pane.copy();
				pane.select(0, 0);
				pane.setCaretPosition(post);
			} catch (Exception ex1) {
				System.out.println("Could Not save to clipboard: " + ex1.getMessage());
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (gradient != null && isString()) {
			setContentAreaFilled(false);
			final Graphics2D g2 = (Graphics2D) g;
			int w = getWidth();
			int h = getHeight();
			g2.setPaint(gradient);
			g2.fillRect(0, 0, w, h);
			// g2.setPaint(Color.BLACK);
			// g2.fillRect(w-5, h-5, w, h);
		} else {
			setContentAreaFilled(true);
		}
		super.paintComponent(g);
	}

	public long getCrcValue() {
		return metaData.crcValue;
	}

	public void setCrcValue(long crcValue) {
		this.metaData.crcValue = crcValue;
	}

	/**
	 * @param i
	 *            the slot number
	 */
	public boolean loadTmpFile() {

		try {
			File file = new File("clip" + metaData.id + ".tmp");
			if (file.exists()) {
				String type = ClipProperties.getProperty("clip" + metaData.id + ".tmp", "String");
				if (type.indexOf("String") != -1) {
					StringSelection selection = null;
					StringBuffer buffer = new StringBuffer((int) file.length());
					Reader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
					int ch;
					while ((ch = in.read()) > -1) {
						buffer.append((char) ch);
					}
					in.close();
					file = new File("clip" + metaData.id + ".mtmp");
					String plainSting = buffer.toString();
					String mimeType = null;
					if (file.exists() && type.indexOf("rtf") != -1) {
						mimeType = "rtf";
						in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
						ByteArrayOutputStream os = new ByteArrayOutputStream((int) file.length());
						while ((ch = in.read()) > -1) {
							os.write(ch);
						}
						in.close();
						selection = new StringSelection(plainSting, os.toByteArray());
						os.close();
					} else if (file.exists() && type.indexOf("html") != -1) {
						mimeType = "html";
						in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
						buffer = new StringBuffer((int) file.length());
						while ((ch = in.read()) > -1) {
							buffer.append((char) ch);
						}
						in.close();
						selection = new StringSelection(plainSting, buffer.toString());
					} else {
						selection = new StringSelection(plainSting);
						if (type.equals("String")) {
							if (file.exists()) {
								file.delete();
							}
						}
					}
					setNewString(selection, mimeType);
					return true;
				} else if (type.equals("Image")) {
					try {
						BufferedImage bi = ImageIO.read(file);
						if (bi != null) {
							setNewImage(bi, true);

							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(bi, "bmp", baos);
							byte[] imageInBytes = baos.toByteArray();
							baos.close();
							CRC32 crc = new CRC32();
							crc.update(imageInBytes);
							setCrcValue(crc.getValue());
							return true;
						}
					} catch (MalformedURLException mue) {
						System.out.println("url " + mue.getMessage());
					} catch (IOException ioe) {
						System.out.println("read: " + ioe.getMessage());
					}
					return false;
				} else if (type.equals("List")) {
					FileInputStream fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis, "UTF8");
					BufferedReader in = new BufferedReader(isr);
					List<File> list = new LinkedList<File>();
					String fileName;
					while ((fileName = in.readLine()) != null) {
						list.add(new File(fileName));
					}
					setNewFile(list, true);
					in.close();
					return true;
				}
			}
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		setLocked(false);
		return false;
	}

	public void renameTmpFile() {
		if (isString()) {
			String type = ClipProperties.getProperty("clip" + metaData.id + ".tmp", "String");
			ClipProperties.setProperty("clip" + metaData.id + ".dtmp", type);
		} else if (isImage()) {
			ClipProperties.setProperty("clip" + metaData.id + ".dtmp", "Image");
		} else if (isFile()) {
			ClipProperties.setProperty("clip" + metaData.id + ".dtmp", "List");
		}
		File file = new File("clip" + metaData.id + ".tmp");
		if (file.exists()) {
			File file2 = new File("clip" + metaData.id + ".dtmp");
			file2.delete();
			file.renameTo(file2);
			file2 = new File("clip" + metaData.id + ".dmtmp");
			file2.delete();
		}
		if (isString()) {
			file = new File("clip" + metaData.id + ".mtmp");
			if (file.exists()) {
				File file2 = new File("clip" + metaData.id + ".dmtmp");
				file2.delete();
				file.renameTo(file2);
			}
		}
	}

	public boolean restoreDtmpFile() {
		File file = new File("clip" + metaData.id + ".dtmp");
		if (file.exists()) {
			String type = ClipProperties.getProperty("clip" + metaData.id + ".dtmp", "String");
			ClipProperties.setProperty("clip" + metaData.id + ".tmp", type);
			File file2 = new File("clip" + metaData.id + ".tmp");
			file2.delete();
			file.renameTo(file2);

			if (type.indexOf("String") != -1) {
				// check for dmtmp too
				file = new File("clip" + metaData.id + ".dmtmp");
				if (file.exists()) {
					file2 = new File("clip" + metaData.id + ".mtmp");
					file2.delete();
					file.renameTo(file2);
				}
			}

			return loadTmpFile();
		}
		return false;
	}

	public void deleteItemFromConsole() {
		deleteItem();
		if (!isEmpty()) {
			System.out.println("Deleted Item #" + metaData.id);
		}
	}

	public void deleteItem() {
		renameTmpFile();
		metaData.deleteClip();
		setToolTipText(null);
		if (getIcon() != null) {
			setIcon(null);
			setText("" + getId());
		}
		makeUnlockedButton();
		setBorder(BUTTON_BORDER);
		setGradient(null);
		setEnabled(false);
	}

	/**
	 * Makes locked button only if it wasnt locked due to edit mode. return true if made false otherwise
	 * @return
	 */
	public boolean makeLockedButton() {
		if (!unlockAfterEditMode) {
			GradientPaint gp = new GradientPaint(BUTTON_WIDTH / 4, BUTTON_HEIGHT / 4, Color.WHITE, BUTTON_WIDTH, BUTTON_HEIGHT, LOCKED_COLOR, false);
			setGradient(gp);
			setBorder(BorderFactory.createLineBorder(new Color(221, 157, 38)));
			return true;
		}
		return false;
		
	}

	public void makeUnlockedButton() {
		makeOldButton();
		setBackground(null);
	}

	public void makeOldButton() {
		if (NORMAL_BUTTON_COLOR != null) {
			GradientPaint gp = new GradientPaint(BUTTON_WIDTH / 4, BUTTON_HEIGHT / 4, Color.WHITE, BUTTON_WIDTH, BUTTON_HEIGHT, NORMAL_BUTTON_COLOR, false);
			setGradient(gp);
		} else {
			setGradient(null);
		}
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(50, 150, 225), 1), BorderFactory.createEmptyBorder()));
	}

	public void makeNewButton() {
		if (NORMAL_BUTTON_COLOR != null) {
			GradientPaint gp = new GradientPaint(BUTTON_WIDTH / 4, BUTTON_HEIGHT / 4, Color.WHITE, BUTTON_WIDTH, BUTTON_HEIGHT, NORMAL_BUTTON_COLOR, false);
			setGradient(gp);
		} else {
			setGradient(null);
		}
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.WHITE));
	}

	public void highlightButton(Color highlight, Color borderColor) {
		GradientPaint gp = new GradientPaint(BUTTON_WIDTH / 6, BUTTON_HEIGHT / 6, Color.WHITE, BUTTON_WIDTH, BUTTON_HEIGHT, highlight, false);
		setGradient(gp);
		setBorder(BorderFactory.createLineBorder(borderColor));
	}

}