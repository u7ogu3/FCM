package com.acm.main.gui;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.OverlayLayout;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

import com.acm.main.ACM;
import com.acm.main.ClipProperties;
import com.acm.main.gui.dialogs.AboutDialog;
import com.acm.main.gui.dialogs.ExtensionDialog;
import com.acm.main.gui.dialogs.FeedbackDialog;
import com.acm.main.gui.dialogs.HelpDialog;
import com.acm.main.keyboard.ACMKeyListener;
import com.acm.main.util.FilteredStreams;
import com.acm.main.util.Network;
import com.acm.main.util.Pruner;
import com.acm.main.util.Updater;
import com.sun.awt.AWTUtilities;

public class ACMGUI {

	/* Constants. */
	private static final long ALWAYS_ON_TOP_TIMER = 60000L;
	private static final int DESIRED_HEIGHT = 15;
	private static final ImageIcon icon_small = new ImageIcon(ACM.class.getResource("res/acm_small.gif"));
	private static final String unicodeFont = "Arial Unicode MS";
	private static final Font smallFont = new Font("Arial", Font.BOLD, 10);
	private static final Font outputWindowFont = new Font(unicodeFont, Font.PLAIN, 11);
	private static GridBagConstraints c = new GridBagConstraints();
	private static MouseMotionListener mml;

	public static final int TA_WIDTH = 100;
	public static final ImageIcon icon = new ImageIcon(ACM.class.getResource("res/acm.gif"));
	public static final int MESSAGE_TYPE_UPDATED = 1;
	public static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

	private static TrayIcon trayIcon = null;
	private static Font taFont = new Font(unicodeFont, Font.PLAIN, 10);
	private static JSlider slider = null;

	public static int WIDTH = 340;
	public static int HEIGHT = 0;
	public static ImageIcon folderIcon = new ImageIcon(ACM.class.getResource("res/folderImage.gif"));
	public static Font font = new Font("Verdana", Font.PLAIN, 10);
	public static Point location;
	public static MouseEvent pressed;
	public static String lastIp = null;
	public static int translucentValue = 100;

	/* Containers. */
	public static JDialog mainDialog = new JDialog();
	public static JDialog outputDialog = null;

	/* Text boxes and scrollers. */
	public static JTextArea ta = new JTextArea() {

		private static final long serialVersionUID = 1L;

		public Point getToolTipLocation(MouseEvent event) {
			if (verticalMode) {
				JToolTip tooltip = this.createToolTip();
				tooltip.setTipText(this.getToolTipText());
				Popup tooltipPopup = PopupFactory.getSharedInstance().getPopup(this, tooltip, this.getLocationOnScreen().x, this.getLocationOnScreen().y + ACMGUI.HEIGHT);
				tooltipPopup.show();
				tooltipPopup.hide();
				// Move this to the right side if ACM is on the left side
				if (tooltip.getWidth() > ACMGUI.mainDialog.getX()) {
					return new Point(ACMGUI.WIDTH, 0);
				}
				return new Point(-1 * (tooltip.getWidth() + 3), 0);
			}
			return null;
		}

	};

	private static JTabbedPane tab = new JTabbedPane();

	public static JTextArea output = new JTextArea();
	private static JScrollPane scrollTab = new JScrollPane(tab);
	private static JScrollPane scrollOutput = new JScrollPane(output);

	/* History. */
	public static ArrayList<String> history = new ArrayList<String>();
	public static int historyIndex = 0;
	public static String partiallyEntered = "";

	/* Properties. */
	public static boolean dragged = false;
	public static boolean isAlwaysOnTop = true;
	public static boolean translucencySupportedOnSystem = true;
	public static boolean keyPasteEnabled = true;
	public static boolean lockInPlace = false;
	public static boolean menuIsOpen = false;
	public static boolean focusLostDueToMenuButton = false;
	public static boolean automaticallyUpdate = true;
	public static boolean verticalMode = false;
	public static boolean snowEffect = false;

	public static void setUpGUI() {
		try {
			setupCrazyProperties();
			setupTrayIcon();
			mml = motionListener();
			setupMainDialogListeners();
			ACMGUI.mainDialog.setSize(ACMGUI.WIDTH, ACMGUI.HEIGHT);

			final JPopupMenu menu = setupMenu();

			/* Set up the buttons. */
			setupMenuButton(menu, c);
			if (verticalMode) {
				c.gridwidth = GridBagConstraints.REMAINDER;
				ACMGUI.mainDialog.add(Box.createVerticalStrut(2), c);
			} else {
				ACMGUI.mainDialog.add(Box.createHorizontalStrut(2));
			}
			setupClipboardButtons(c);
			setupConsoleInputArea(c);
			setupCloseMinimizeButtons(c);
			ACM.splash.increaseProgress();

			/* Now on to the output frame. */
			setupOutputDialog();
			setupTabs();
			setTranslucency(translucentValue * 0.01f);
			StayAlwaysOnTop saot = new StayAlwaysOnTop();
			saot.start();

			if (snowEffect) {
				setupSnowEffect();
			}
		} catch (Exception catchAll) {
			catchAll.printStackTrace();
			System.out.println("GUI could no be started");
		}
	}

	private static void setupSnowEffect() {
		final ImageIcon snow2 = new ImageIcon(ACM.class.getResource("res/snow/snow2.png"));
		final ImageIcon snow3 = new ImageIcon(ACM.class.getResource("res/snow/snow3.png"));
		final ImageIcon snow4 = new ImageIcon(ACM.class.getResource("res/snow/snow4.png"));
		mainDialog.setGlassPane(new JComponent() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			int lastX = (int) (Math.random() * ACMGUI.WIDTH);
			int lastY = -20;
			boolean switched = true;
			Image snowImage = getSnow();

			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				long time = System.currentTimeMillis() / 1000;
				g.drawImage(snowImage, lastX += (((int) (Math.random() * 10) % 5) == 1 ? 1 : -1), lastY++, null);
				if (time % 7 == 0 && !switched) {
					lastX = (int) (Math.random() * ACMGUI.WIDTH);
					switched = true;
					lastY = -20;
					snowImage = getSnow();
				} else if (time % 7 != 0) {
					switched = false;
				}
			}

			private Image getSnow() {
				int snowNum = ((int) (Math.random() * 10)) % 3;
				switch (snowNum) {
				case 0:
					return snow2.getImage();
				case 1:
					return snow3.getImage();
				default:
					return snow4.getImage();
				}
			}
		});

		mainDialog.getGlassPane().setVisible(true);
		mainDialog.getGlassPane().addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				mainDialog.getGlassPane().setVisible(false);
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

		});
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					while (snowEffect) {
						Thread.sleep(75);
						if (mainDialog.getGlassPane().isVisible()) {
							mainDialog.getGlassPane().repaint();
						} else {
							Thread.sleep(30000);
							if (snowEffect && mainDialog.getMousePosition() == null) {
								mainDialog.getGlassPane().setVisible(true);
							}
						}
					}
				} catch (Exception e) {
				}
			}

		}).start();
	}

	public static void setTranslucency(float opacity) {
		try {
			if (translucencySupportedOnSystem) {
				if (AWTUtilities.isTranslucencySupported(AWTUtilities.Translucency.TRANSLUCENT)) {
					Class<?> awtUtilitiesClass = Class.forName("com.sun.awt.AWTUtilities");
					Method mSetWindowOpacity = awtUtilitiesClass.getMethod("setWindowOpacity", Window.class, float.class);
					mSetWindowOpacity.invoke(null, mainDialog, Float.valueOf(opacity));
					mSetWindowOpacity.invoke(null, outputDialog, Float.valueOf(opacity));
				} else {
					translucencySupportedOnSystem = false;
					slider.setEnabled(false);
				}
			}
		} catch (Exception e) {
			translucencySupportedOnSystem = false;
			slider.setEnabled(false);
		}
	}

	private static void setupTabs() {
		// Add output back in before setupoutputdialog() is called
		tab.setFont(font);

		tab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tab.setPreferredSize(new Dimension((int) outputDialog.getSize().getWidth() - 4, (int) outputDialog.getSize().getHeight() - 4));
		tab.setTabPlacement(JTabbedPane.TOP);
		tab.addTab("Console", scrollOutput);
		// TODO
		// if (ACM.macros.isMacrosEnabled()) {
		// Class clazz;
		// try {
		// clazz = Class.forName("groovy.ui.Console");
		// Object groovy = clazz.newInstance();
		// JApplet applet = new JApplet();
		// clazz.getMethod("run", new Class[] { JApplet.class }).invoke(groovy, applet);
		// tab.addTab("GroovyConsole", applet);
		// } catch (Exception e1) {
		// tab.addTab("Console", scrollOutput);
		// }
		// } else {
		// tab.addTab("Console", scrollOutput);
		// }

		// Need to get all jar files in current directory and for loop through
		// changing the name...the jar isFile name and the isFile that extends a
		// component must have same name...thats it
		// also preferred size neeeds to be set otherwise it will expand
		// automatically to largest tab, most likely output window
		File[] validJarFiles = ExtensionDialog.findValidExtensions();
		String[] validJarFileNames = ExtensionDialog.getJarFileNames(validJarFiles);
		Vector<String> unusedExtensions = new Vector<String>();
		for (int i = 0; i < ExtensionDialog.extensions.size(); i++) {
			unusedExtensions.add(ExtensionDialog.extensions.get(i));
		}
		for (int i = 0; i < validJarFiles.length; i++) {
			File jarFile = validJarFiles[i];
			String jarFileName = validJarFileNames[i];
			if (jarFile != null) {
				if (ExtensionDialog.extensions.contains(jarFileName)) {
					try {
						URLClassLoader ucl = new URLClassLoader(new URL[] { jarFile.toURI().toURL() });
						Component component = (Component) Class.forName(jarFileName, true, ucl).newInstance();

						// scrollbar for the component
						JScrollPane scrollComponent = new JScrollPane(component);
						scrollComponent.setPreferredSize(new Dimension(ACMGUI.WIDTH - 20, ACMGUI.HEIGHT - 20));
						scrollComponent.getVerticalScrollBar().setSize(5, scrollComponent.getMaximumSize().height);
						scrollComponent.getVerticalScrollBar().setUnitIncrement(20);
						scrollComponent.getVerticalScrollBar().setBlockIncrement(20);
						scrollComponent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
						scrollComponent.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
						scrollComponent.setWheelScrollingEnabled(true);
						ACMGUI.tab.addTab(jarFileName, scrollComponent);
					} catch (Exception e) {
						e.printStackTrace();
					}
					unusedExtensions.remove(jarFileName);
				}
			} else {
				// JAR isFile no longer exists - remove it
				ExtensionDialog.extensions.remove(jarFileName);
				unusedExtensions.remove(jarFileName);
			}
		}

		// prune any unopened extensions
		for (int i = 0; i < unusedExtensions.size(); i++) {
			ExtensionDialog.extensions.remove(unusedExtensions.get(i));
		}
		unusedExtensions.clear();
		ExtensionDialog.saveExtensions();
	}

	/**
	 * 
	 */
	private static void setupOutputDialog() {
		ACMGUI.outputDialog = new JDialog(ACMGUI.mainDialog);
		ACMGUI.outputDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		ACMGUI.outputDialog.setUndecorated(true);
		if (verticalMode) {
			ACMGUI.outputDialog.setSize(3, ACMGUI.HEIGHT);
			ACMGUI.outputDialog.setLocation((int) ACMGUI.mainDialog.getLocation().getX() - 3, (int) ACMGUI.mainDialog.getLocation().getY());
		} else {
			ACMGUI.outputDialog.setSize(ACMGUI.WIDTH, 3);
			ACMGUI.outputDialog.setLocation((int) ACMGUI.mainDialog.getLocation().getX(), (int) ACMGUI.mainDialog.getLocation().getY() + ACMGUI.HEIGHT);
		}

		ACMGUI.output.setEditable(false);
		ACMGUI.output.setLineWrap(true);
		ACMGUI.output.setWrapStyleWord(true);
		ACMGUI.output.setFont(outputWindowFont);
		ACMGUI.output.setCursor(new Cursor(Cursor.TEXT_CURSOR));

		ACMGUI.scrollOutput.getVerticalScrollBar().setSize(5, ACMGUI.scrollOutput.getMaximumSize().height);
		ACMGUI.scrollOutput.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		ACMGUI.scrollOutput.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		ACMGUI.scrollOutput.setWheelScrollingEnabled(true);
		ACMGUI.scrollTab.setToolTipText("<html><b>Pull me out!</b></html>");
		ACMGUI.scrollTab.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray), BorderFactory.createLineBorder(Color.black)));
		try {
			if (verticalMode) {
				ACMGUI.scrollTab.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
			} else {
				ACMGUI.scrollTab.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}
		tab.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		ACMGUI.scrollTab.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent me) {
				if (me.getClickCount() >= 2) {
					if (verticalMode) {
						int width = (int) (ACMGUI.mainDialog.getLocation().getX());

						/*
						 * Double-clicking should open the window. If the window is already expanded, then close it.
						 */
						if (ACMGUI.outputDialog.getWidth() == width) {
							ACMGUI.outputDialog.setSize(3, ACMGUI.HEIGHT);
							ACMGUI.outputDialog.setLocation((int) ACMGUI.mainDialog.getLocation().getX() - 3, (int) ACMGUI.mainDialog.getLocation().getY());
							ACMGUI.outputDialog.validate();
						} else {
							ACMGUI.outputDialog.setSize((int) (ACMGUI.mainDialog.getLocation().getX()), ACMGUI.HEIGHT);
							ACMGUI.outputDialog.setSize(width, ACMGUI.HEIGHT);
							ACMGUI.outputDialog.setLocation(0, (int) ACMGUI.mainDialog.getLocation().getY());
							ACMGUI.outputDialog.validate();
						}
					} else {
						int height = (int) (ACMGUI.screenSize.height - ACMGUI.mainDialog.getLocation().getY() - ACMGUI.HEIGHT);

						/*
						 * Double-clicking should open the window. If the window is already expanded, then close it.
						 */
						if (ACMGUI.outputDialog.getHeight() == height) {
							ACMGUI.outputDialog.setSize(ACMGUI.WIDTH, 3);
							ACMGUI.outputDialog.validate();
						} else {
							ACMGUI.outputDialog.setSize(ACMGUI.WIDTH, height);
							ACMGUI.outputDialog.validate();
						}
					}
				}
			}

			public void mouseEntered(MouseEvent arg0) {
			}

			public void mouseExited(MouseEvent arg0) {
			}

			public void mousePressed(MouseEvent me) {
				ACMGUI.pressed = me;
			}

			public void mouseReleased(MouseEvent arg0) {
			}
		});
		ACMGUI.scrollTab.addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged(MouseEvent me) {
				if (verticalMode) {
					int widthChange = ACMGUI.pressed.getX() - me.getX();
					// ClipGUI.pressed = me;
					if (widthChange != 0) {
						if (widthChange < 0 && (ACMGUI.outputDialog.getSize().width - 3 + widthChange) < 0) {
							ACMGUI.outputDialog.setSize(3, ACMGUI.HEIGHT);
							ACMGUI.outputDialog.setLocation((int) ACMGUI.mainDialog.getLocation().getX() - 3, (int) ACMGUI.mainDialog.getLocation().getY());
						} else {
							if ((ACMGUI.outputDialog.getSize().width + widthChange) < (int) (ACMGUI.mainDialog.getLocation().getX())) {
								ACMGUI.outputDialog.setSize(ACMGUI.outputDialog.getSize().width + widthChange, ACMGUI.HEIGHT);
								ACMGUI.outputDialog.setLocation((int) (ACMGUI.outputDialog.getLocation().getX() - widthChange), (int) ACMGUI.outputDialog.getLocation().getY());
							} else {
								ACMGUI.outputDialog.setSize((int) (ACMGUI.mainDialog.getLocation().getX()), ACMGUI.HEIGHT);
								ACMGUI.outputDialog.setLocation(0, (int) ACMGUI.mainDialog.getLocation().getY());
							}
						}
					}
				} else {
					int heightChange = me.getY() - ACMGUI.pressed.getY();
					ACMGUI.pressed = me;
					if (heightChange < 0 && (ACMGUI.outputDialog.getSize().height - 3 + heightChange) < 0) {
						ACMGUI.outputDialog.setSize(ACMGUI.WIDTH, 3);
					} else {
						if ((ACMGUI.outputDialog.getSize().height + heightChange + ACMGUI.HEIGHT) < ACMGUI.screenSize.height) {
							ACMGUI.outputDialog.setSize(ACMGUI.WIDTH, ACMGUI.outputDialog.getSize().height + heightChange);
						} else {
							ACMGUI.outputDialog.setSize(ACMGUI.WIDTH, (int) (ACMGUI.screenSize.height - (ACMGUI.mainDialog.getLocation().getY() + ACMGUI.HEIGHT)));
						}
					}
				}
				ACMGUI.outputDialog.validate();
			}

			public void mouseMoved(MouseEvent e) {
			}
		});

		ACMGUI.scrollTab.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		ACMGUI.scrollTab.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		ACMGUI.scrollTab.setWheelScrollingEnabled(true);
		ACMGUI.outputDialog.add(ACMGUI.scrollTab);
	}

	/**
	 * @param menu
	 *            the popup menu associated with the button
	 * @param c
	 *            the constraints to use
	 */
	private static void setupMenuButton(final JPopupMenu menu, GridBagConstraints c) {
		JButton button;
		/* Set up the menu. */
		button = new JButton(new ImageIcon(ACMGUI.icon.getImage().getScaledInstance(ACMClip.BUTTON_WIDTH - 2, ACMClip.BUTTON_HEIGHT - 2, Image.SCALE_SMOOTH)));

		// Since we have the heights and widths of buttons by now we can set the
		// Image of the folder to be scaled to proper dimension so it is not
		// done everytime.
		folderIcon = new ImageIcon(folderIcon.getImage().getScaledInstance(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT, Image.SCALE_SMOOTH));

		button.setSize(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT);
		button.setMaximumSize(new Dimension(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT));
		button.setBackground(Color.WHITE);
		button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.WHITE, Color.BLACK));

		// to remove the 'selected' border when loading up
		button.setFocusable(false);
		button.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent arg0) {
			}

			public void mouseEntered(MouseEvent arg0) {
				ACMGUI.mainDialog.setFocusableWindowState(true);
				ACMGUI.focusLostDueToMenuButton = true;
			}

			public void mouseExited(MouseEvent arg0) {
				ACMGUI.focusLostDueToMenuButton = false;
			}

			public void mousePressed(MouseEvent e) {
				ACMGUI.mainDialog.setFocusableWindowState(true);
				if (ACMGUI.menuIsOpen) {
					menu.setVisible(false);
					ACMGUI.menuIsOpen = false;
				} else {
					if (verticalMode) {
						if (menu.getWidth() > ACMGUI.mainDialog.getX()) {
							menu.show(e.getComponent(), ACMGUI.WIDTH, 0);
						} else {
							menu.show(e.getComponent(), (-1) * (menu.getWidth()), 0);
						}
					} else {
						menu.show(e.getComponent(), 0, ACMClip.BUTTON_HEIGHT + 1);
					}
					ACMGUI.menuIsOpen = true;
				}
			}

			public void mouseReleased(MouseEvent e) {

				/*
				 * If the menu button was not clicked, then allow the user to drag down to a menu item of their choice. The menu item that the mouse released on is determined based on the location of
				 * the mouse click on the screen relative to the top-left point of the menu on the screen.
				 */
				if (ACMGUI.dragged) {
					if (ACMGUI.menuIsOpen) {
						Point ePoint = e.getLocationOnScreen();
						Point menuPoint = menu.getLocationOnScreen();
						JComponent comp = (JComponent) (menu.getComponentAt((int) (ePoint.getX() - menuPoint.getX()), (int) (ePoint.getY() - menuPoint.getY())));
						if (comp != null && comp instanceof JMenuItem) {
							((JMenuItem) comp).doClick();
							menu.setVisible(false);
						}
					}
					ACMGUI.fireMouseDoneDragging();
				}
			}
		});

		menu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent arg0) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
				if (!ACMGUI.focusLostDueToMenuButton) {
					ACMGUI.menuIsOpen = false;
				}
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
			}
		});

		button.addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged(MouseEvent arg0) {
				ACMGUI.dragged = true;
			}

			public void mouseMoved(MouseEvent arg0) {
			}
		});
		button.setToolTipText("<html><b>ACM Menu</b></html>");
		if (verticalMode) {
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.gridheight = 1;
			c.weighty = 0.0;
		} else {
			c.gridwidth = 1;
			c.weightx = 0.0;
		}
		c.fill = GridBagConstraints.BOTH;
		ACMGUI.mainDialog.add(button, c);
	}

	public static String[] validatePasswordIpPortString(String passwordIpPortString, boolean showMessage) {
		String[] values = passwordIpPortString.split(":");
		if (values.length < 2 || values.length > 3) {
			if (showMessage) {
				JOptionPane.showMessageDialog(null, "Incorrect value for password:ip:port or ip:port", "Parse error", JOptionPane.WARNING_MESSAGE);
			}
			return null;
		}
		try {
			InetAddress.getByName(values[values.length == 2 ? 0 : 1]);
		} catch (UnknownHostException e1) {
			if (showMessage) {
				JOptionPane.showMessageDialog(null, "Unknown Host " + values[values.length == 2 ? 0 : 1], "Unknown Host Exception", JOptionPane.WARNING_MESSAGE);
			}
			return null;
		}
		int port = 0;
		try {
			port = Integer.parseInt(values[values.length == 2 ? 1 : 2]);
		} catch (NumberFormatException nfe) {
			if (showMessage) {
				JOptionPane.showMessageDialog(null, "Illegal Port " + values[values.length == 2 ? 1 : 2], "Illegal Port", JOptionPane.WARNING_MESSAGE);
			}
			return null;
		}
		if (port < 1 || port > 65535) {
			if (showMessage) {
				JOptionPane.showMessageDialog(null, "Illegal Port " + values[values.length == 2 ? 1 : 2], "Illegal Port", JOptionPane.WARNING_MESSAGE);
			}
			return null;
		}
		return values;
	}

	/**
	 * @return
	 */
	private static JPopupMenu setupMenu() {
		final JPopupMenu menu = new JPopupMenu();
		HelpDialog.setUpHelpDialog();
		JMenu sideMenu = new JMenu("Help");
		sideMenu.setFont(font);
		JMenuItem menuItem = new JMenuItem("General Help");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				HelpDialog.openHelpDialog();
			}
		});
		sideMenu.add(menuItem);

		// Unfinished: display menu tooltips here.
		// menuItem = new JCheckBoxMenuItem("Display Menu Help",
		// displayMenuHelp);
		// menuItem.setFont(font);
		// menuItem.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent arg0) {
		// Component [] meh = menu.getComponents();
		// for (int i = 0; i < meh.length; i++) {
		// if (meh[i] instanceof JMenuItem){
		// System.out.println(((JMenuItem)meh[i]));
		// }
		// }
		// JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
		// if (item.isSelected()) {
		// ClipGUI.displayMenuHelp = true;
		// ClipProperties.setProperty("displayMenuHelp", "true");
		// } else {
		// ClipProperties.setProperty("displayMenuHelp", "false");
		// ClipGUI.displayMenuHelp = false;
		// }
		// }
		// });
		// menu.add(menuItem);

		AboutDialog.setUpAboutDialog();
		menuItem = new JMenuItem("About ACM");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				AboutDialog.openAboutDialog();
			}
		});
		sideMenu.add(menuItem);

		FeedbackDialog.setUpFeedbackDialog();
		menuItem = new JMenuItem("Send Feedback");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FeedbackDialog.feedbackDialog.setLocation((int) (ACMGUI.screenSize.getWidth() / 2 - FeedbackDialog.feedbackDialog.getWidth() / 2),
						(int) (ACMGUI.screenSize.getHeight() / 2 - FeedbackDialog.feedbackDialog.getHeight() / 2));
				FeedbackDialog.feedbackDialog.setVisible(true);
			}
		});
		sideMenu.add(menuItem);

		menu.add(sideMenu);
		sideMenu = new JMenu("Updates");
		sideMenu.setFont(font);

		menuItem = new JMenuItem("Check for Updates");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				menu.setVisible(false);
				Updater.checkForUpdates(true);
			}
		});
		sideMenu.add(menuItem);

		menuItem = new JCheckBoxMenuItem("Automatically Update", automaticallyUpdate);
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
				if (item.isSelected()) {
					ACMGUI.automaticallyUpdate = true;
					ClipProperties.setProperty("automaticallyUpdate", "true");
					if (!Updater.instanceRunning()) {
						Updater.startNewInstance();
					}
				} else {
					ClipProperties.setProperty("automaticallyUpdate", "false");
					ACMGUI.automaticallyUpdate = false;
					Updater.stopInstance();
				}
			}
		});
		sideMenu.add(menuItem);

		menu.add(sideMenu);
		sideMenu = new JMenu("Network");
		sideMenu.setFont(font);

		if (!Network.networked) {
			menuItem = new JCheckBoxMenuItem("Enable", Network.networkSupport);
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
					if (item.isSelected()) {
						Network.networkSupport = true;
						ClipProperties.setProperty("networkSupport", "true");
						try {
							if (ACM.network != null) {
								ACM.network.close();
								ACM.network = null;
							}
							Network.init(Network.networkPort);
						} catch (Exception e) {
							JOptionPane.showMessageDialog(mainDialog, "Network init failed. Pick different port.", "Network Init Failed", JOptionPane.WARNING_MESSAGE);
							ClipProperties.setProperty("networkSupport", "false");
							Network.networkSupport = false;
							item.setSelected(false);
						}
					} else {
						ClipProperties.setProperty("networkSupport", "false");
						Network.networkSupport = false;
						if (ACM.network != null) {
							ACM.network.close();
							ACM.network = null;
						}
					}
				}
			});
			sideMenu.add(menuItem);

			menuItem = new JMenuItem("Port:" + Network.networkPort);
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				int port = 0;

				public void actionPerformed(ActionEvent arg0) {
					String tempString = JOptionPane.showInputDialog(mainDialog, "Please enter local port number (Disable/Enable Network to take effect):");
					if (tempString != null && tempString.length() > 0) {
						try {
							port = Integer.parseInt(tempString);
						} catch (NumberFormatException nfe) {
							JOptionPane.showMessageDialog(mainDialog, "Illegal Port " + tempString, "Illegal Port", JOptionPane.WARNING_MESSAGE);
						}
						if (port < 1 || port > 65535) {
							port = 6999;
						}
						ClipProperties.setProperty("networkPort", "" + port);
					}
				}
			});
			sideMenu.add(menuItem);

			menuItem = new JMenuItem("Password");
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (menu.isVisible()) {
						menu.setVisible(false);
					}
					String tempString = JOptionPane.showInputDialog(mainDialog, "Please enter your network password:", Network.networkPassword);
					if (tempString != null && tempString.length() > 0) {
						ClipProperties.setProperty("networkPassword", tempString);
						Network.networkPassword = tempString;
					}
				}
			});
			sideMenu.add(menuItem);

			menuItem = new JMenuItem("Connect");
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					String tempString = JOptionPane.showInputDialog(mainDialog, "Please enter password:ip:port or ip:port for no password", lastIp);
					if (tempString != null && tempString.length() > 0) {
						String[] values = validatePasswordIpPortString(tempString, true);
						if (values == null) {
							return;
						}
						ClipProperties.setProperty("lastIp", "" + tempString);
						try {
							File file;
							if (!(file = new File(ACM.NETWORK_HISTORY_FILE)).exists()) {
								file.createNewFile();
							}
							BufferedReader br = new BufferedReader(new FileReader(file));
							String readLine = null;
							boolean write = true;
							while ((readLine = br.readLine()) != null) {
								if (readLine.equals(tempString)) {
									write = false;
								}
							}
							if (write) {
								PrintWriter pw = new PrintWriter(new FileWriter(file, true));
								pw.println(tempString);
								pw.close();
							}
						} catch (IOException e1) {
						}
						try {
							Runtime.getRuntime().exec(
									ACM.getExecCommand(ACM.NETWORKING_FLAG + " " + ACM.NETWORKING_IP_FLAG + values[values.length == 2 ? 0 : 1] + " " + ACM.NETWORKING_PORT_FLAG
											+ values[values.length == 2 ? 1 : 2])
											+ " " + ACM.NETWORKING_PASSWORD_FLAG + (values.length == 2 ? "" : values[0]));
						} catch (Exception e1) {
						}
						ACM.closeACM();
					}
				}

			});
			sideMenu.add(menuItem);

			try {
				File file;
				if ((file = new File(ACM.NETWORK_HISTORY_FILE)).exists()) {
					BufferedReader br = new BufferedReader(new FileReader(file));
					String tempString = null;
					while ((tempString = br.readLine()) != null) {
						final String[] values = validatePasswordIpPortString(tempString, true);
						if (values == null) {
							// TODO remove this item from the list since it is invalid
						} else {
							ClipProperties.setProperty("lastIp", "" + tempString);
							menuItem = new JMenuItem(tempString);
							menuItem.setFont(font);
							menuItem.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									try {
										Runtime.getRuntime().exec(
												ACM.getExecCommand(ACM.getExecCommand(ACM.NETWORKING_FLAG + " " + ACM.NETWORKING_IP_FLAG + values[values.length == 2 ? 0 : 1] + " "
														+ ACM.NETWORKING_PORT_FLAG + values[values.length == 2 ? 1 : 2])
														+ " " + ACM.NETWORKING_PASSWORD_FLAG + (values.length == 2 ? "" : values[0])));
									} catch (IOException e1) {
									}
									ACM.closeACM();
								}

							});

							sideMenu.add(menuItem);
						}
					}
					br.close();
				}
			} catch (IOException ioe) {
			}

		} else {
			menuItem = new JMenuItem("Save All Slots Locally");
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					for (int i = 0; i < ACM.acm.getSize(); i++) {
						ACMClip clip = ACM.acm.getClips().getButton(i);
						if (!clip.isEmpty()) {
							if (clip.isString()) {
								ACM.getACMClipWriter().writeString(clip.getMetaData().stringObject, i);
							} else if (clip.isImage()) {
								ACM.getACMClipWriter().writeImage(clip.getMetaData().imageObject, i);
							} else if (clip.isFile()) {
								ACM.getACMClipWriter().writeFileList(clip.getMetaData().fileObject, i);
							}
						}
					}
					try {
						Runtime.getRuntime().exec(ACM.getExecCommand(ACM.RETRYING_FLAG));
					} catch (IOException e) {
					}
					ACM.closeACM();
				}
			});
			sideMenu.add(menuItem);

			menuItem = new JMenuItem("DISCONNECT");
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						Runtime.getRuntime().exec(ACM.getExecCommand(ACM.RETRYING_FLAG));
					} catch (IOException e) {
					}
					ACM.closeACM();
				}
			});
			sideMenu.add(menuItem);
		}

		menu.add(sideMenu);

		sideMenu = new JMenu("Settings");
		sideMenu.setFont(font);

		if (!Network.networked) {
			menuItem = new JCheckBoxMenuItem("Image Support", ACM.acm.areImagesSupported());
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					menu.setVisible(false);
					JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
					if (item.isSelected()) {
						JOptionPane
								.showMessageDialog(
										mainDialog,
										"Please Note: Images will not be saved into the database.\nCopying large images may consume larger amounts of memory.\nWhen polling clipboard Ctrl+v for images will be disabled",
										"Image Support Warning", JOptionPane.WARNING_MESSAGE);
						ACM.acm.setImageSupport(true);
						ClipProperties.setProperty("imageSupport", "true");
					} else {
						ClipProperties.setProperty("imageSupport", "false");
						ACM.acm.setImageSupport(false);
					}
				}
			});
			sideMenu.add(menuItem);

			menuItem = new JCheckBoxMenuItem("File Support", ACM.acm.areFilesSupported());
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
					if (item.isSelected()) {
						ACM.acm.setFileSupport(true);
						ClipProperties.setProperty("fileSupport", "true");
					} else {
						ClipProperties.setProperty("fileSupport", "false");
						ACM.acm.setFileSupport(false);
					}
				}
			});
			sideMenu.add(menuItem);
			sideMenu.addSeparator();
		}

		if (!Network.networked) {
			menuItem = new JMenuItem("No. of Slots: " + ACM.acm.getSize());
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (menu.isVisible()) {
						menu.setVisible(false);
					}
					int desiredButtons;
					try {
						String tempString = JOptionPane.showInputDialog(mainDialog, "Please enter how many slots you want:");
						if (tempString != null && tempString.length() > 0) {
							desiredButtons = Integer.parseInt(tempString);
							ClipProperties.setProperty("numButtons", "" + desiredButtons);
							synchronized (ACM.acm.getSafeClipLock()) {
								Runtime.getRuntime().exec(ACM.getExecCommand(ACM.RETRYING_FLAG));
								ACM.closeACM();
							}
						}
					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(mainDialog, "Sorry, that wasn't a number. Doh!", "Doh!", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			sideMenu.add(menuItem);
		}
		if (!Network.networked) {
			final JMenuItem pruningMenuItem = new JMenuItem("Days to Prune: " + (Pruner.numDaysToPrune == 0 ? "(disabled)" : Pruner.numDaysToPrune));
			pruningMenuItem.setFont(font);
			pruningMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (menu.isVisible()) {
						menu.setVisible(false);
					}
					int desiredDays;
					try {
						String tempString = JOptionPane.showInputDialog(mainDialog, "Please enter the cut-off pruning interval in days (0 to disable):");
						if (tempString != null && tempString.length() > 0) {
							desiredDays = Integer.parseInt(tempString);
							if (desiredDays < 0) {
								JOptionPane.showMessageDialog(mainDialog, "Sorry, no negative numbers are allowed.", "Doh!", JOptionPane.ERROR_MESSAGE);
							} else {
								ClipProperties.setProperty("numDaysToPrune", "" + desiredDays);
								ClipProperties.setProperty("lastPrunedTime", "" + 0);
								Pruner.numDaysToPrune = desiredDays;
								Pruner.lastPrunedTime = 0;
								synchronized (ACM.acm.getSafeClipLock()) {
									if (desiredDays == 0) {
										Pruner.stopInstance();
									} else {
										// Start a new thread.
										Pruner.stopInstance();
										Pruner.startNewInstance();
									}
								}
								pruningMenuItem.setText("Days to Prune: " + (Pruner.numDaysToPrune == 0 ? "(disabled)" : Pruner.numDaysToPrune));
							}
						}
					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(mainDialog, "Sorry, that wasn't a valid number. Doh!", "Doh!", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			sideMenu.add(pruningMenuItem);
		}

		menuItem = new JCheckBoxMenuItem("Paste on Click", keyPasteEnabled);
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
				if (item.isSelected()) {
					ACMGUI.keyPasteEnabled = true;
					ClipProperties.setProperty("keyPasteEnabled", "true");
				} else {
					ClipProperties.setProperty("keyPasteEnabled", "false");
					ACMGUI.keyPasteEnabled = false;
				}
			}
		});
		sideMenu.add(menuItem);

		if (!Network.networked) {
			menuItem = new JCheckBoxMenuItem("Stop Monitoring", ACM.acm.hasStoppedMonitoring());
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
					if (item.isSelected()) {
						ClipProperties.setProperty("stopMonitoring", "true");
						ACM.acm.setStopMonitoring(true);
					} else {
						ClipProperties.setProperty("stopMonitoring", "false");
						ACM.acm.setStopMonitoring(false);
					}
				}
			});
			sideMenu.add(menuItem);
		}

		if (!Network.networked) {
			menuItem = new JCheckBoxMenuItem("Styled Paste", ACM.acm.isStyledPasteOn());
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
					if (item.isSelected()) {
						ClipProperties.setProperty("styledPaste", "true");
						ACM.acm.setStyledPaste(true);
					} else {
						ClipProperties.setProperty("styledPaste", "false");
						ACM.acm.setStyledPaste(false);
					}
				}
			});
			sideMenu.add(menuItem);
		}

		if (!Network.networked) {
			menuItem = new JCheckBoxMenuItem("Looping", ACM.acm.isLooping());
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
					if (item.isSelected()) {
						ClipProperties.setProperty("looping", "true");
						ACM.acm.setLoop(true);
					} else {
						ClipProperties.setProperty("looping", "false");
						ACM.acm.setLoop(false);
					}
				}
			});
			sideMenu.add(menuItem);
		}

		menuItem = new JCheckBoxMenuItem("Use Shortcuts", ACMKeyListener.useShortcuts);
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
				if (item.isSelected()) {
					ClipProperties.setProperty("useShortcuts", "true");
					ACMKeyListener.useShortcuts = true;
				} else {
					ClipProperties.setProperty("useShortcuts", "false");
					ACMKeyListener.useShortcuts = false;
				}
			}
		});
		sideMenu.add(menuItem);
		if (!Network.networked) {
			sideMenu.addSeparator();
			ButtonGroup group = new ButtonGroup();
			menuItem = new JRadioButtonMenuItem("Poll System Clipboard");
			menuItem.setFont(font);
			if (ACM.acm.isPollClipboard()) {
				menuItem.setSelected(true);
			}
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					JRadioButtonMenuItem item = (JRadioButtonMenuItem) arg0.getSource();
					if (item.isSelected()) {
						ClipProperties.setProperty("pollClipboard", "true");
						ACM.acm.setPollClipboardAndInit(true);
					} else {
						ClipProperties.setProperty("pollClipboard", "false");
						ACM.acm.setPollClipboardAndInit(false);
					}
				}
			});
			group.add(menuItem);
			sideMenu.add(menuItem);

			menuItem = new JRadioButtonMenuItem("Own System Clipboard");
			menuItem.setFont(font);
			if (!ACM.acm.isPollClipboard()) {
				menuItem.setSelected(true);
			}
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					JRadioButtonMenuItem item = (JRadioButtonMenuItem) arg0.getSource();
					if (item.isSelected()) {
						ClipProperties.setProperty("pollClipboard", "false");
						ACM.acm.setPollClipboardAndInit(false);
					} else {
						ClipProperties.setProperty("pollClipboard", "true");
						ACM.acm.setPollClipboardAndInit(true);
					}
				}
			});
			group.add(menuItem);
			sideMenu.add(menuItem);
		}

		menu.add(sideMenu);
		sideMenu = new JMenu("Window");
		sideMenu.setFont(font);

		menuItem = new JMenuItem("Change Colors");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				menu.setVisible(false);
				ACMClip.NORMAL_BUTTON_COLOR = JColorChooser.showDialog(mainDialog, "Choose a new button color", ACMClip.NORMAL_BUTTON_COLOR);
				if (ACMClip.NORMAL_BUTTON_COLOR != null) {
					GradientPaint gp = new GradientPaint(ACMClip.BUTTON_WIDTH / 4, ACMClip.BUTTON_HEIGHT / 4, Color.WHITE, ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT, ACMClip.NORMAL_BUTTON_COLOR,
							false);
					ACM.acm.getClips().changeButtonColourToGradient(gp);
					ClipProperties.setProperty("color", "" + ACMClip.NORMAL_BUTTON_COLOR.getRGB());
				} else {
					ClipProperties.setProperty("color", "-1");
					ACM.acm.getClips().changeButtonColourToOld();
				}
			}

		});
		sideMenu.add(menuItem);

		slider = new JSlider(10, 100, translucentValue);
		menuItem = new JMenuItem("Opacity");
		menuItem.setFont(font);
		menuItem.setSize(slider.getPreferredSize());
		slider.setBackground(Color.WHITE);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider) e.getSource();
				setTranslucency(slider.getValue() * 0.01f);
				if (!slider.getValueIsAdjusting()) {
					translucentValue = slider.getValue();
					ClipProperties.setProperty("translucentValue", "" + slider.getValue());
				}
			}

		});
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(60));
		box.add(slider);
		menuItem.add(box);
		sideMenu.add(menuItem);

		menuItem = new JCheckBoxMenuItem("Always on Top", isAlwaysOnTop);
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
				if (item.isSelected()) {
					ClipProperties.setProperty("isAlwaysOnTop", "true");
					isAlwaysOnTop = true;
					ACMGUI.mainDialog.setAlwaysOnTop(isAlwaysOnTop);
				} else {
					ClipProperties.setProperty("isAlwaysOnTop", "false");
					isAlwaysOnTop = false;
					ACMGUI.mainDialog.setAlwaysOnTop(isAlwaysOnTop);
				}
			}
		});
		sideMenu.add(menuItem);

		menuItem = new JCheckBoxMenuItem("Lock in Place", lockInPlace);
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
				if (item.isSelected()) {
					ClipProperties.setProperty("lockInPlace", "true");
					ACMGUI.lockInPlace = true;
				} else {
					ClipProperties.setProperty("lockInPlace", "false");
					ACMGUI.lockInPlace = false;
				}
			}
		});
		sideMenu.add(menuItem);

		menuItem = new JCheckBoxMenuItem("Vertical Mode", verticalMode);
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
				if (item.isSelected()) {
					ClipProperties.setProperty("verticalMode", "true");
				} else {
					ClipProperties.setProperty("verticalMode", "false");
				}
				synchronized (ACM.acm.getSafeClipLock()) {
					try {
						Runtime.getRuntime().exec(ACM.getExecCommand(ACM.RETRYING_FLAG));
					} catch (IOException e) {
						e.printStackTrace();
					}
					ACM.closeACM();
				}
			}
		});
		sideMenu.add(menuItem);

		menuItem = new JCheckBoxMenuItem("Snow Effect", snowEffect);
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
				if (item.isSelected()) {
					ClipProperties.setProperty("snowEffect", "true");
					snowEffect = true;
					setupSnowEffect();
				} else {
					ClipProperties.setProperty("snowEffect", "false");
					snowEffect = false;
					mainDialog.getGlassPane().setVisible(false);
				}
			}
		});
		sideMenu.add(menuItem);

		menu.add(sideMenu);
		sideMenu = new JMenu("Output");
		sideMenu.setFont(font);

		menuItem = new JMenuItem("Open Output");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (verticalMode) {
					ACMGUI.outputDialog.setLocation(0, (int) ACMGUI.mainDialog.getLocation().getY());
					ACMGUI.outputDialog.setSize((int) (ACMGUI.mainDialog.getLocation().getX()), ACMGUI.HEIGHT);
				} else {
					ACMGUI.outputDialog.setSize(ACMGUI.WIDTH, (int) (ACMGUI.screenSize.height - ACMGUI.mainDialog.getLocation().getY() - ACMGUI.HEIGHT));
				}
				ACMGUI.outputDialog.validate();
			}
		});
		sideMenu.add(menuItem);
		menuItem = new JMenuItem("Close Output");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (verticalMode) {
					ACMGUI.outputDialog.setSize(3, ACMGUI.HEIGHT);
					ACMGUI.outputDialog.setLocation((int) ACMGUI.mainDialog.getLocation().getX() - 3, (int) ACMGUI.mainDialog.getLocation().getY());
				} else {
					ACMGUI.outputDialog.setSize(ACMGUI.WIDTH, 3);
				}
				ACMGUI.outputDialog.validate();
			}
		});
		sideMenu.add(menuItem);
		menuItem = new JMenuItem("Clear Output");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// TODO These lines will become unnecessary after pruning.
				ACMGUI.output.setText("");
				System.gc();
			}
		});
		sideMenu.add(menuItem);
		menu.add(sideMenu);
		sideMenu = new JMenu("Data");
		sideMenu.setFont(font);

		if (!Network.networked) {
			menuItem = new JMenuItem("Delete All Slots");
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					ACM.acm.getClips().deleteAllSlots();
				}
			});
			sideMenu.add(menuItem);

			menuItem = new JMenuItem("Delete Unlocked Slots");
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					ACM.acm.getClips().deleteUnlockedSlots();
				}
			});
			sideMenu.add(menuItem);

			menuItem = new JMenuItem("Restore All Slots");
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					ACM.acm.getClips().restoreAll();
				}
			});
			sideMenu.add(menuItem);

			menuItem = new JMenuItem("Delete Everything");
			menuItem.setFont(font);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (menu.isVisible()) {
						menu.setVisible(false);
					}
					if (JOptionPane.YES_OPTION == JOptionPane
							.showConfirmDialog(
									mainDialog,
									"<html>Warning! This operation will delete all copied items from ACM permanently.<br><font size=+1>This operation is not reversible!</font><br><br>Are you sure you wish to proceed?</html>",
									"Warning!", JOptionPane.YES_NO_OPTION)) {
						// First delete the slots
						ACM.acm.getClips().deleteAllSlots();

						// Make sure to delete the .dtmp files and .tmp files
						String[] children;
						FilenameFilter filter = new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.startsWith("clip") && (name.endsWith(".tmp") || name.endsWith(".dtmp"));
							}
						};
						children = new File(".").list(filter);
						for (int i = 0; i < children.length; i++) {
							new File(children[i]).delete();
						}

						// Remove the database.
						File db = new File(ACM.DATABASE_FILE);
						db.delete();
						OutputStreamWriter osw;
						try {
							db.createNewFile();
							osw = new OutputStreamWriter(new FileOutputStream(db, false), "UTF8");
							osw.write("UTF8\n");
							osw.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			sideMenu.add(menuItem);
		}

		menu.add(sideMenu);
		sideMenu = new JMenu("Macros");
		sideMenu.setFont(font);

		menuItem = new JCheckBoxMenuItem("Enable", ACM.macros.isMacrosEnabled());
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final JCheckBoxMenuItem item = (JCheckBoxMenuItem) arg0.getSource();
				if (item.isSelected()) {
					ClipProperties.setProperty("macrosEnabled", "true");
					new Thread() {
						public void run() {
							if (!ACM.macros.setMacrosEnabled(true)) {
								item.setSelected(false);
							}
						}
					}.start();
				} else {
					ClipProperties.setProperty("macrosEnabled", "false");
					ACM.macros.setMacrosEnabled(false);
				}
			}
		});
		sideMenu.add(menuItem);

		menuItem = new JMenuItem("New GroovyConsole");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (ACM.macros.isMacrosEnabled()) {
					new Thread() {
						@SuppressWarnings("unchecked")
						@Override
						public void run() {
							Class clazz;
							try {
								clazz = Class.forName("groovy.ui.Console");
								Object groovy = clazz.newInstance();
								clazz.getMethod("run", new Class[] {}).invoke(groovy);
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(null, "GroovyConsole failed to load", "Error", JOptionPane.ERROR_MESSAGE);
							}
						}

					}.start();
				} else {
					JOptionPane.showMessageDialog(null, "Macros Not Enabled. Please Enable to use Console", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		sideMenu.add(menuItem);

		menu.add(sideMenu);

		menuItem = new JMenuItem("Extensions");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ExtensionDialog.setUpExtensionDialog();
				ExtensionDialog.extensionDialog.setLocation((int) (ACMGUI.screenSize.getWidth() / 2 - ExtensionDialog.extensionDialog.getWidth() / 2),
						(int) (ACMGUI.screenSize.getHeight() / 2 - ExtensionDialog.extensionDialog.getHeight() / 2));
				ExtensionDialog.extensionDialog.setVisible(true);
			}
		});

		menu.add(menuItem);

		menuItem = new JMenuItem("Exit");
		menuItem.setFont(font);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ACM.closeACM();
			}
		});
		menu.add(menuItem);

		/* Make all the menu items white. */
		menu.setBackground(Color.WHITE);
		Component[] menuItems = menu.getComponents();
		for (int i = 0; i < menuItems.length; i++) {
			menuItems[i].setBackground(Color.WHITE);
			if (menuItems[i] instanceof JMenu) {
				for (Component component : ((JMenu) menuItems[i]).getMenuComponents()) {
					component.setBackground(Color.WHITE);
				}
			}
		}

		// HACK: Open and close the menu to calculate the desired menu width.
		menu.setVisible(true);
		menu.setVisible(false);
		return menu;
	}

	private static void setupConsoleInputArea(GridBagConstraints c) {
		ACMGUI.ta.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent arg0) {
			}

			public void mouseEntered(MouseEvent arg0) {
				ACMGUI.mainDialog.setFocusableWindowState(true);
			}

			public void mouseExited(MouseEvent arg0) {
			}

			public void mousePressed(MouseEvent arg0) {
			}

			public void mouseReleased(MouseEvent arg0) {
				ACMGUI.fireMouseDoneDragging();
			}
		});
		taFont = new Font(taFont.getFontName(), Font.PLAIN, ACMClip.BUTTON_HEIGHT * 2 / 3);
		ACMGUI.ta.setFont(taFont);
		ACMGUI.ta.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				int size = history.size();
				if (arg0.getKeyCode() == KeyEvent.VK_UP) {
					if (historyIndex == size) {
						partiallyEntered = ta.getText();
					}
					if (historyIndex >= 0) {
						if (historyIndex > 0) {
							historyIndex--;
						} else if (historyIndex == 0) {
							Toolkit.getDefaultToolkit().beep();
						}
						if (size > 0) {
							ta.setText(history.get(historyIndex));
						}
					}
				} else if (arg0.getKeyCode() == KeyEvent.VK_DOWN) {
					if (historyIndex < size) {
						if (historyIndex < size - 1) {
							historyIndex++;
							ta.setText(history.get(historyIndex));
						} else if (historyIndex == size - 1) {
							historyIndex++;
							Toolkit.getDefaultToolkit().beep();
							ta.setText(partiallyEntered);
							partiallyEntered = "";
						}
					} else if (historyIndex == size || size == 0) {
						Toolkit.getDefaultToolkit().beep();
					}
				}
			}

			public void keyReleased(KeyEvent arg0) {
				String entered = ta.getText();
				if (arg0.getKeyChar() == KeyEvent.VK_ENTER) {
					entered = entered.replaceAll("\n", "");
					FilteredStreams.unprocessedCharacters += entered + "\n";
					FilteredStreams.FilteredInputStream.notifyLock();

					history.add(entered);
					historyIndex = history.size();

					if (entered.length() > 0) {
						try {
							PrintWriter pw = new PrintWriter(new FileOutputStream(new File(ACM.COMMAND_TEXBOX_HISTORY_FILE), true));
							pw.print(entered + "\n");
							pw.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					partiallyEntered = "";
					ta.setText("");
				} else if (arg0.getKeyChar() == KeyEvent.VK_ESCAPE) {
					if (partiallyEntered.length() == 0) {
						partiallyEntered = ta.getText();
						ClipProperties.setProperty("partiallyEntered", partiallyEntered);
						ta.setText("");
					} else {
						ta.setText(partiallyEntered);
						partiallyEntered = "";
					}
				}
				entered = ta.getText();
				boolean isSearching = false;
				if (entered.length() > 0) {
					if (entered.charAt(0) == 'a') {
						ACM.acm.getClips().guiSearch(entered.replaceAll("\n", "").substring(1), true);
						isSearching = true;
					} else if (entered.charAt(0) == 'z') {
						ACM.acm.getClips().guiSearch(entered.replaceAll("\n", "").substring(1), false);
						isSearching = true;
					}
				}
				if (!isSearching) {
					ACM.acm.getClips().guiSearch("", true);
				}
			}

			public void keyTyped(KeyEvent arg0) {
			}
		});
		if (verticalMode) {
			ta.setWrapStyleWord(false);
			ta.setLineWrap(true);
		}
		ta.setToolTipText("<html><b>Command Textbox:</b><br />" + "Type 'h' to see a complete list of prefixes.<br /><br />" + "<b>Example</b>: <br />" + "<code>atext</code><br/>"
				+ "will perform a case-sensitive search on the <br />word 'text' in all of the slots.</html>");
		JScrollPane scroll = new JScrollPane(ACMGUI.ta);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(new Dimension(ACMClip.BUTTON_WIDTH * 3, ACMClip.BUTTON_HEIGHT));
		scroll.setMaximumSize(new Dimension(ACMClip.BUTTON_WIDTH * 3, ACMClip.BUTTON_HEIGHT));
		scroll.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		if (verticalMode) {
			c.gridheight = 3;
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1.0;
			c.gridwidth = GridBagConstraints.REMAINDER;
		} else {
			c.gridwidth = 3;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
		}
		ACMGUI.mainDialog.add(scroll, c);
	}

	private static void setupCloseMinimizeButtons(GridBagConstraints c) {
		JPanel windowControlsPanel;
		if (verticalMode) {
			windowControlsPanel = new JPanel(new GridLayout(1, 2));
		} else {
			windowControlsPanel = new JPanel(new GridLayout(2, 1));
		}

		JButton close = new JButton(" x ");
		close.setBorder(null);
		close.setForeground(Color.WHITE);
		close.setBackground(Color.BLACK);
		close.setFont(smallFont);
		close.setToolTipText("<html><b>Close</b></html>");
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ACMGUI.hideACM();
				ACM.closeACM();
			}
		});

		JButton minimize = new JButton("– ");
		minimize.setBorder(null);
		minimize.setForeground(Color.WHITE);
		minimize.setBackground(Color.BLACK);
		minimize.setFont(smallFont);
		minimize.setToolTipText("<html><b>Minimize to Tray</b></html>");
		minimize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ACMGUI.hideACM();
			}
		});

		if (verticalMode) {
			windowControlsPanel.add(minimize);
			windowControlsPanel.add(close);
		} else {
			windowControlsPanel.add(close);
			windowControlsPanel.add(minimize);
		}

		Box theLastBox;
		if (verticalMode) {
			theLastBox = new Box(BoxLayout.PAGE_AXIS);
			theLastBox.add(Box.createVerticalStrut(2));
		} else {
			theLastBox = new Box(BoxLayout.LINE_AXIS);
			theLastBox.add(Box.createHorizontalStrut(2));
		}
		theLastBox.add(windowControlsPanel);

		if (verticalMode) {
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			c.weighty = 0.0;
		} else {
			c.gridwidth = 1;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 0.0;
			c.weighty = 1.0;
		}
		ACMGUI.mainDialog.add(theLastBox, c);
	}

	private static void setupClipboardButtons(GridBagConstraints c) {

		int previouslyLoadedIndex = ACMClipSlots.previousIndex;
		JPanel overlayPanel = new JPanel();
		overlayPanel.setLayout(new OverlayLayout(overlayPanel));
		JPanel clipboardItemButtonsPanel;
		if (verticalMode) {
			clipboardItemButtonsPanel = new JPanel(new GridLayout(ACM.acm.getSize(), 1));
			clipboardItemButtonsPanel.setSize(ACMClip.BUTTON_WIDTH, ACMClip.BUTTON_HEIGHT * ACM.acm.getSize());
		} else {
			clipboardItemButtonsPanel = new JPanel(new GridLayout(1, ACM.acm.getSize()));
			clipboardItemButtonsPanel.setSize(ACMClip.BUTTON_WIDTH * ACM.acm.getSize(), ACMClip.BUTTON_HEIGHT);
		}
		ACM.acm.getClips().setupButtons(mml, clipboardItemButtonsPanel);
		if (!ACM.acm.getClips().getButton(previouslyLoadedIndex).isEmpty()) {
			ACM.acm.getClips().makeNewItem(previouslyLoadedIndex);
		}

		overlayPanel.add(clipboardItemButtonsPanel);

		if (verticalMode) {
			c.gridheight = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 0.0; // 2.5
		} else {
			c.gridwidth = 1;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 0.0; // 2.5
		}
		ACMGUI.mainDialog.add(overlayPanel, c);
	}

	/**
	 * All crazy tool-tip properties which took days to figure out.
	 */
	private static void setupCrazyProperties() {
		// make sure tool tips show up immediately
		ToolTipManager.sharedInstance().setInitialDelay(0);

		// make sure tool tips last forever (or at least four years)
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		// the following line enables tool tips for an unfocused window (NOT
		// A BUG!!!)
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6178004
		UIManager.getDefaults().put("ToolTipManager.enableToolTipMode", "");

		// change the tool tip font and font
		UIManager.put("ToolTip.font", new FontUIResource(unicodeFont, Font.PLAIN, 11));
		UIManager.put("ToolTip.background", new ColorUIResource(255, 255, 225));

		// change the menu hover colors
		UIManager.put("MenuItem.selectionBackground", new ColorUIResource(49, 106, 197));
		UIManager.put("MenuItem.selectionForeground", new ColorUIResource(255, 255, 255));
		UIManager.put("CheckBoxMenuItem.selectionBackground", new ColorUIResource(49, 106, 197));
		UIManager.put("CheckBoxMenuItem.selectionForeground", new ColorUIResource(255, 255, 255));
	}

	/**
	 * This method is public so it can be called early on by ACM.java so that we can display splash screen
	 */
	public static void setInitialMainDialogProperties() {
		ACMGUI.mainDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		// To be fixed by Sun, some day. The following displays an icon in
		// the task bar.
		ACMGUI.mainDialog.setFocusableWindowState(true);

		// Set the frame's isImage icon
		// http://java.sun.com/docs/books/tutorial/uiswing/components/frame.html
		ACMGUI.mainDialog.setIconImage(ACMGUI.icon.getImage());
		ACMGUI.mainDialog.setUndecorated(true);
		ACMGUI.mainDialog.setResizable(false);

		ACMGUI.mainDialog.setAlwaysOnTop(isAlwaysOnTop);
		ACMGUI.mainDialog.setLocation(location);
		ACMGUI.mainDialog.setLayout(new GridBagLayout());
	}

	/**
	 * 
	 */
	private static void setupMainDialogListeners() {
		/* If minimized from the taskbar, make sure the icon disappears. */
		ACMGUI.mainDialog.addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent arg0) {
			}

			public void windowClosed(WindowEvent arg0) {
				ACM.closeACM();
			}

			public void windowClosing(WindowEvent arg0) {
			}

			public void windowDeactivated(WindowEvent arg0) {
			}

			public void windowDeiconified(WindowEvent arg0) {
			}

			public void windowIconified(WindowEvent arg0) {
				/* Restore the frame back to deiconified mode. */
				ACMGUI.hideACM();
			}

			public void windowOpened(WindowEvent arg0) {
			}
		});

		// moves JFrame by clicking anywhere in the frame
		// http://forum.java.sun.com/thread.jspa?forumID=57&threadID=406114
		ACMGUI.mainDialog.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent me) {
				ACMGUI.pressed = me;
			}

			public void mouseClicked(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
				ACMGUI.fireMouseDoneDragging();
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}
		});

		ACMGUI.mainDialog.addMouseMotionListener(mml);
	}

	/**
	 * Determine the size of this frame to be the height of a normal window's title bar.
	 */
	public static void setClipHeight() {
		JFrame temp = new JFrame();
		temp.setSize(ACMGUI.WIDTH, ACMGUI.DESIRED_HEIGHT);
		temp.add(new JButton("temp"));
		temp.setLocation(0 - 2 * ACMGUI.WIDTH, 0 - 2 * ACMGUI.DESIRED_HEIGHT);
		temp.pack();
		ACMGUI.HEIGHT = temp.getHeight() - temp.getContentPane().getHeight() - 11; // 11
		// is a magic number here
	}

	/**
	 * @return
	 */
	private static MouseMotionListener motionListener() {
		MouseMotionListener mml = new MouseMotionListener() {
			public void mouseDragged(MouseEvent me) {
				ACMGUI.dragged = true;
				if (!ACMGUI.lockInPlace) {
					ACMGUI.location = ACMGUI.mainDialog.getLocation(ACMGUI.location);
					int x = ACMGUI.location.x - ACMGUI.pressed.getX() + me.getX();
					int y = ACMGUI.location.y - ACMGUI.pressed.getY() + me.getY();
					if (verticalMode) {
						if (x < ACMGUI.outputDialog.getWidth()) {
							x = ACMGUI.outputDialog.getWidth();
						} else if (x > ACMGUI.screenSize.width - ACMGUI.mainDialog.getWidth()) {
							x = ACMGUI.screenSize.width - ACMGUI.mainDialog.getWidth();
						}
						if (y < 0) {
							y = 0;
						} else if (y > ACMGUI.screenSize.height - ACMGUI.mainDialog.getHeight()) {
							y = ACMGUI.screenSize.height - ACMGUI.mainDialog.getHeight();
						}
					} else {
						if (x < 0) {
							x = 0;
						} else if (x > ACMGUI.screenSize.width - ACMGUI.mainDialog.getWidth()) {
							x = ACMGUI.screenSize.width - ACMGUI.mainDialog.getWidth();
						}
						if (y < 0) {
							y = 0;
						} else if (y > ACMGUI.screenSize.height - ACMGUI.mainDialog.getHeight() - ACMGUI.outputDialog.getHeight()) {
							y = ACMGUI.screenSize.height - ACMGUI.mainDialog.getHeight() - ACMGUI.outputDialog.getHeight();
						}
					}
					ACMGUI.mainDialog.setLocation(x, y);
					// Set the output window to the left if in vertical mode
					if (verticalMode) {
						ACMGUI.outputDialog.setLocation((int) (ACMGUI.mainDialog.getLocation().getX() - ACMGUI.outputDialog.getWidth()), (int) ACMGUI.mainDialog.getLocation().getY());
					} else {
						ACMGUI.outputDialog.setLocation((int) ACMGUI.mainDialog.getLocation().getX(), (int) ACMGUI.mainDialog.getLocation().getY() + ACMGUI.HEIGHT);
					}
				}
			}

			public void mouseMoved(MouseEvent e) {
			}
		};
		return mml;
	}

	/**
	 * @param smallFont
	 */
	private static void setupTrayIcon() {
		/* Create a popup menu. */
		final PopupMenu popup = new PopupMenu();
		final MenuItem openPopupItem = new MenuItem("Open ACM");
		openPopupItem.setFont(smallFont);
		openPopupItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!ACMGUI.mainDialog.isVisible()) {
					ACMGUI.showACM();
				} else {
					ACMGUI.hideACM();
				}
			}
		});
		popup.add(openPopupItem);
		popup.addSeparator();

		MenuItem popupItem = new MenuItem("Exit");
		popupItem.setFont(smallFont);
		popupItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ACMGUI.hideACM();
				ACM.closeACM();
			}
		});
		popup.add(popupItem);

		/* Add a tray icon to the system tray area. */
		trayIcon = new TrayIcon(ACMGUI.icon_small.getImage(), "ACM", popup);
		trayIcon.setImageAutoSize(true);
		trayIcon.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent me) {
				trayIcon.setActionCommand(null);
				if (me.getButton() == MouseEvent.BUTTON1) {
					if (!ACMGUI.mainDialog.isVisible()) {
						ACMGUI.showACM();
					}
					ACMGUI.mainDialog.toFront();
				}
			}

			public void mouseEntered(MouseEvent arg0) {
			}

			public void mouseExited(MouseEvent arg0) {
			}

			public void mousePressed(MouseEvent me) {
			}

			public void mouseReleased(MouseEvent me) {
				if (me.isPopupTrigger()) {
					if (!ACMGUI.mainDialog.isVisible()) {
						openPopupItem.setLabel("Show ACM");
					} else {
						openPopupItem.setLabel("Hide ACM");
					}
				}
			}
		});
		trayIcon.setToolTip("ACM");

		try {
			java.awt.SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	public static void hideACM() {
		ACMGUI.mainDialog.setVisible(false);
		ACMGUI.outputDialog.setVisible(false);
	}

	public static void showACM() {
		ACMGUI.mainDialog.setVisible(true);
		ACMGUI.outputDialog.setVisible(true);
		hackAlwaysOnTop();
	}

	public static void fireMouseDoneDragging() {
		dragged = false;
		ClipProperties.setProperty("windowLocationX", "" + (int) location.getX());
		ClipProperties.setProperty("windowLocationY", "" + (int) location.getY());
	}

	/**
	 * We must call setAlwaysOnTop twice - once for the outputDialog (child of mainDialog), and once for the mainDialog. setAlwaysOnTop() needs to be toggled from false to true to take effect (see
	 * Java source).
	 */
	public static void hackAlwaysOnTop() {
		if (!(FeedbackDialog.feedbackDialog.isVisible() || AboutDialog.aboutDialog.isVisible() || HelpDialog.helpDialog.isVisible() || ExtensionDialog.extensionDialog.isVisible())
				&& mainDialog.isVisible() && isAlwaysOnTop) {
			// If none of the components in ACM already have focus (ex. the
			// tabbed components, then set always on top)
			if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == null) {
				mainDialog.setAlwaysOnTop(false);
				mainDialog.setAlwaysOnTop(true);
				mainDialog.setAlwaysOnTop(false);
				mainDialog.setAlwaysOnTop(true);
			}
		}
	}

	private static class StayAlwaysOnTop extends Thread {
		public void run() {
			while (true) {
				hackAlwaysOnTop();
				System.gc();
				try {
					sleep(ALWAYS_ON_TOP_TIMER);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Only release the control if not using a hotkey.
	 * 
	 * @param button
	 */
	public static void pasteClipboard(boolean releaseCtrl, ACMClip button) {
		if (!(Network.networked && button.isFile())) {
			try {
				Robot robo = new Robot();
				robo.keyPress(KeyEvent.VK_CONTROL);
				robo.keyPress(KeyEvent.VK_V);
				if (releaseCtrl) {
					robo.keyRelease(KeyEvent.VK_CONTROL);
				}
				robo.keyRelease(KeyEvent.VK_V);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void showBalloon(int messageType) {
		switch (messageType) {
		case MESSAGE_TYPE_UPDATED:
			trayIcon.setActionCommand("updating");
			trayIcon.displayMessage("ACM Updated to Version " + ACM.VERSIONID, "Click here to view the changes.", TrayIcon.MessageType.INFO);
			/* If a balloon popped up, do something with it. */
			trayIcon.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() != null && e.getActionCommand().equals("updating")) {
						trayIcon.setActionCommand(null);
						AboutDialog.openAboutDialog();
					}
				}
			});
			break;
		default:
			break;
		}
	}
}
