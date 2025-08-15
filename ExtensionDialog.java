package com.acm.main.gui.dialogs;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.acm.main.ACM;
import com.acm.main.ClipProperties;
import com.acm.main.gui.ACMGUI;

public class ExtensionDialog {
	public static JDialog extensionDialog = new JDialog();
	public static Vector<String> extensions = new Vector<String>();

	public static void setUpExtensionDialog() {
		ExtensionDialog.extensionDialog = new JDialog(ACMGUI.mainDialog, "Configure Extensions");
		ExtensionDialog.extensionDialog.setSize((int) ACMGUI.screenSize.getWidth() * 4 / 10, (int) ACMGUI.screenSize.getHeight() * 2 / 5);
		ExtensionDialog.extensionDialog.setAlwaysOnTop(true);
		ExtensionDialog.extensionDialog.setResizable(false);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		String[] validExtensionNames = getJarFileNames(findValidExtensions());

		JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		if (validExtensionNames.length == 0) {
			headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));
			headerPanel.setAlignmentX(0.5f);
			headerPanel.add(Box.createHorizontalStrut(15));
			headerPanel.add(new JLabel("No extensions were found at this time."));
			JButton button = new JButton("Download Extensions now!");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						Desktop.getDesktop().browse(new URI("http://acmgr.sourceforge.net#extensions"));
						ExtensionDialog.extensionDialog.setVisible(false);
					} catch (Exception e1) {
					}
				}
			});
			headerPanel.add(Box.createHorizontalStrut(10));
			headerPanel.add(button);
			headerPanel.add(Box.createHorizontalStrut(15));
			panel.add(headerPanel);
		} else {
			panel.add(Box.createVerticalStrut(15));
			headerPanel.add(new JLabel("The following extensions are available: "));
			panel.add(headerPanel);
			Font smallFont = new Font("Arial", Font.BOLD, 11);
			Box b = new Box(BoxLayout.PAGE_AXIS);
			panel.add(Box.createVerticalStrut(5));
			for (int i = 0; i < validExtensionNames.length; i++) {
				b.add(Box.createVerticalStrut(5));
				final String checkBoxName = validExtensionNames[i];
				JCheckBox extensionCheckBox = new JCheckBox(checkBoxName, extensions.contains(checkBoxName));
				extensionCheckBox.setBackground(Color.WHITE);
				extensionCheckBox.setFont(smallFont);
				extensionCheckBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						JCheckBox source = (JCheckBox) e.getSource();
						String labelName = source.getText();
						if (source.isSelected()) {
							if (!extensions.contains(labelName)) {
								extensions.add(labelName);
							}
						} else {
							extensions.remove(labelName);
						}
					}
				});
				b.add(extensionCheckBox);
			}
			b.add(Box.createHorizontalStrut(15));
			JScrollPane scrollBox = new JScrollPane(b);
			scrollBox.getViewport().setBackground(Color.WHITE);
			scrollBox.getViewport().setOpaque(true);
			scrollBox.setPreferredSize(new Dimension(extensionDialog.getSize().width, extensionDialog.getSize().height * 8 / 10));
			panel.add(scrollBox);
			panel.add(Box.createVerticalStrut(10));

			JPanel closePanel = new JPanel();
			closePanel.setLayout(new BoxLayout(closePanel, BoxLayout.PAGE_AXIS));
			JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			messagePanel.add(new JLabel("Changes will take effect after ACM is restarted."));
			closePanel.add(messagePanel);
			JPanel closeButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

			JButton okButton = new JButton("OK - Restart Now");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ExtensionDialog.saveExtensions();
					extensionDialog.setVisible(false);
					synchronized (ACM.acm.getSafeClipLock()) {
						try {
							Runtime.getRuntime().exec(ACM.getExecCommand(ACM.RETRYING_FLAG));
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
						ACM.closeACM();
					}
				}
			});
			closeButtonPanel.add(okButton);

			JButton applyButton = new JButton("Apply Only - Don't Restart");
			applyButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ExtensionDialog.saveExtensions();
					extensionDialog.setVisible(false);
				}
			});
			closeButtonPanel.add(applyButton);

			// JButton downloadButton = new JButton("Get More Extensions");
			// downloadButton.addActionListener(new ActionListener() {
			// public void actionPerformed(ActionEvent e) {
			// try {
			// Desktop.getDesktop().browse(new
			// URI("http://acmgr.sourceforge.net#extensions"));
			// ExtensionDialog.extensionDialog.setVisible(false);
			// } catch (Exception e1) {
			// }
			// }
			// });
			// closeButtonPanel.add(downloadButton);

			closePanel.add(closeButtonPanel);

			panel.add(closePanel);
		}

		ExtensionDialog.extensionDialog.add(panel);
	}

	public static File[] findValidExtensions() {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		};
		File extFolder = new File("extensions/");
		return extFolder.listFiles(filter);
	}

	public static String[] getJarFileNames(File[] children) {
		if (children == null) {
			return null;
		}
		String[] results = new String[children.length];
		for (int i = 0; i < children.length; i++) {
			File jarFile = children[i];
			results[i] = jarFile.getName().substring(0, jarFile.getName().indexOf(".jar"));
		}
		return results;
	}

	public static boolean saveExtensions() {
		String extensionsStr = "";
		for (int i = 0; i < ExtensionDialog.extensions.size(); i++) {
			if (extensionsStr.length() > 0) {
				extensionsStr += ",";
			}
			extensionsStr += ExtensionDialog.extensions.get(i);
		}
		return ClipProperties.setProperty("extensions", extensionsStr);
	}
}
