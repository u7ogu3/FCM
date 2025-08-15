package com.acm.main.gui.dialogs;
import java.awt.Font;
import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.acm.main.ACM;
import com.acm.main.gui.ACMGUI;

public class HelpDialog {
	public static JDialog helpDialog;
	private static final String helpFile = "res/help.htm";

	public static void setUpHelpDialog() {
		HelpDialog.helpDialog = new JDialog(ACMGUI.mainDialog, "ACM Help");
		HelpDialog.helpDialog.setSize((int) ACMGUI.screenSize.getWidth() * 6 / 10, (int) ACMGUI.screenSize.getHeight() * 3 / 5);
		HelpDialog.helpDialog.setAlwaysOnTop(true);
		HelpDialog.helpDialog.setResizable(false);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		JEditorPane pane = new JEditorPane();
		pane.setContentType("text/html");
		pane.setEditable(false);
		Font smallFont = new Font("Courier New", Font.PLAIN, 12);
		pane.setFont(smallFont);
		try {
			InputStream stream = ACM.class.getResourceAsStream(helpFile);
			BufferedInputStream br = new BufferedInputStream(stream);
			int c;
			String contents = "";
			while ((c = br.read()) != -1) {
				contents += (char) c;
			}
			pane.setText(contents);
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		JScrollPane scrollText = new JScrollPane(pane);
		panel.add(scrollText);
		HelpDialog.helpDialog.add(panel);
	}

	/**
	 * 
	 */
	public static void openHelpDialog() {
		helpDialog.setLocation((int) (ACMGUI.screenSize.getWidth() / 2 - helpDialog.getWidth() / 2), (int) (ACMGUI.screenSize.getHeight() / 2 - helpDialog
				.getHeight() / 2));
		helpDialog.setVisible(true);
	}
}
