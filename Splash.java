package com.acm.main.gui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

public class Splash extends Window {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int BORDERSIZE = 8;
	private static final Color BORDERCOLOR = Color.WHITE;
	private Image splashImage;
	private int imgWidth, imgHeight;
	private final int MAX_PROGRESS = 6;
	private int progress = 0;
	private int progressRatio = 0;

	public Splash(JDialog f) {
		super(f);
		splashImage = loadSplashImage();
		showSplashScreen();
		f.addWindowListener(new WindowListener());
	}

	private Image loadSplashImage() {
		MediaTracker tracker = new MediaTracker(this);
		Image result;
		result = ACMGUI.icon.getImage();
		tracker.addImage(result, 0);
		try {
			tracker.waitForAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
		imgWidth = result.getWidth(this);
		progressRatio = imgWidth / MAX_PROGRESS;
		imgHeight = result.getHeight(this);
		return (result);
	}

	private void showSplashScreen() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setBackground(BORDERCOLOR);
		int w = imgWidth + (BORDERSIZE * 2);
		int h = imgHeight + (BORDERSIZE * 2) + 20;
		int x = (screenSize.width - w) / 2;
		int y = (screenSize.height - h) / 2;
		setBounds(x, y, w, h);
		setAlwaysOnTop(true);
		setVisible(true);
	}

	@Override
	public void paint(Graphics g) {
		g.drawImage(splashImage, BORDERSIZE, BORDERSIZE, imgWidth, imgHeight, this);
		g.drawRect(0, 0, imgWidth + (BORDERSIZE * 2) - 1, imgHeight + (BORDERSIZE * 2) + 20 - 1);
		g.drawRect(BORDERSIZE - 1, imgHeight + BORDERSIZE + 5, imgWidth, 10);
	}

	public void increaseProgress() {
		if (progress + 1 == MAX_PROGRESS) {
			this.getGraphics().fillRect(BORDERSIZE - 1, imgHeight + BORDERSIZE + 5, imgWidth, 10);
		} else {
			this.getGraphics().fillRect(BORDERSIZE - 1, imgHeight + BORDERSIZE + 5, progressRatio * ++progress, 10);
		}
	}

	private class WindowListener extends WindowAdapter {
		// was windowActivated, thanks to H.Grippa for the fix!
		public void windowOpened(WindowEvent we) {
			setVisible(false);
			dispose();
			ACMGUI.hackAlwaysOnTop();
		}
	}
}