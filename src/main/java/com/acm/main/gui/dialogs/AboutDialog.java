package com.acm.main.gui.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import com.acm.main.ACM;
import com.acm.main.gui.ACMGUI;

public class AboutDialog {
	public static JDialog aboutDialog;
	private static String changelogFile = "res/changelog.txt";
	private static final Color HIGHLIGHT_COLOR = new Color(234, 255, 234);
	private static JTable history = null;

	private static final String aboutMessage = "<html><head><style type=\"text/css\">body { font-family: Verdana; font-size: 12pt; }</style></head><body><h1>" + ACM.TITLE + "</h1>" + "<b>Version "
			+ ACM.VERSIONID + "</b><br />" + "<i>Released " + ACM.RELEASEDATE + "</i><br /><br />"
			+ "<b>Credits: </b><br /><table><tr><td width=30></td><td><b>Ashish Gupta</b> (<i>Co-Developer</i>)" + "<br /><b>David Chang</b> (<i>Co-Developer</i>)"
			+ "<br /><b>Puneet Jain</b> (<i>Contributor</i>)" + "<br /><b>Kohulan Perampalam</b> (<i>Inspirator</i>)</td></tr></table></body></html>";

	public static void setUpAboutDialog() {
		AboutDialog.aboutDialog = new JDialog(ACMGUI.mainDialog, "About ACM");
		AboutDialog.aboutDialog.setSize((int) ACMGUI.screenSize.getWidth() * 53 / 100, (int) ACMGUI.screenSize.getHeight() * 55 / 100);
		AboutDialog.aboutDialog.setAlwaysOnTop(true);
		AboutDialog.aboutDialog.setResizable(false);
		AboutDialog.aboutDialog.setBackground(Color.WHITE);
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2, 1));
		JPanel creditsPanel = new JPanel();
		creditsPanel.setBackground(Color.WHITE);
		creditsPanel.setLayout(new BoxLayout(creditsPanel, BoxLayout.X_AXIS));
		creditsPanel.add(Box.createHorizontalStrut(20));
		JLabel creditsIconLabel = new JLabel(ACMGUI.icon);
		creditsIconLabel.setAlignmentY(JLabel.BOTTOM_ALIGNMENT);
		creditsPanel.add(creditsIconLabel);
		creditsPanel.add(Box.createHorizontalStrut(50));

		JEditorPane credits = new JEditorPane();
		credits.setContentType("text/html");
		credits.setEditable(false);
		credits.setText(aboutMessage);
		creditsPanel.add(credits);
		JScrollPane scrollCreditsPanel = new JScrollPane(creditsPanel);
		panel.add(scrollCreditsPanel);

		/* Now the history part. */
		Vector<String> columnNames = new Vector<String>();
		columnNames.add("T");
		columnNames.add("Changes");
		columnNames.add("Version");
		Vector<Vector<String>> rowData = new Vector<Vector<String>>();
		populateRowData(rowData);
		history = new JTable(rowData, columnNames) {
			protected String[] columnToolTips = { "Type: B = bug fix, E = enhancement", null, null };

			/**
			 * 
			 */
			private static final long serialVersionUID = -5963027900915860752L;

			public boolean isCellEditable(int row, int column) {
				return false;
			}

			// Implement table header tool tips.
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					public String getToolTipText(MouseEvent e) {
						java.awt.Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						int realIndex = columnModel.getColumn(index).getModelIndex();
						return columnToolTips[realIndex];
					}
				};
			}

			/*
			 * Highlight the current table row if this change is for the current
			 * version.
			 */
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
				Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
				if (((String) getValueAt(rowIndex, 2)).equals(ACM.VERSIONID) && !isCellSelected(rowIndex, vColIndex)) {
					c.setBackground(HIGHLIGHT_COLOR);
				} else if (isCellSelected(rowIndex, vColIndex)) {
					// return the default highlighted row
				} else {
					// If not shaded, match the table's background
					c.setBackground(getBackground());
				}
				return c;
			}
		};
		history.setBackground(Color.WHITE);
		history.setFont(new Font("Verdana", Font.PLAIN, 11));
		history.setAutoCreateRowSorter(true);
		history.getColumnModel().getColumn(0).setCellRenderer(new CenterRenderer());
		history.getColumnModel().getColumn(2).setCellRenderer(new CenterRenderer());
		history.getColumnModel().getColumn(0).setPreferredWidth(35);
		history.getColumnModel().getColumn(1).setPreferredWidth(aboutDialog.getWidth() * 90 / 100);
		JScrollPane scrollHistory = new JScrollPane(history);
		scrollHistory.setBackground(Color.WHITE);
		scrollHistory.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		panel.setBackground(Color.WHITE);
		panel.add(scrollHistory);
		AboutDialog.aboutDialog.add(panel);
	}

	private static void populateRowData(Vector<Vector<String>> rowData) {
		InputStream stream = ACM.class.getResourceAsStream(changelogFile);
		BufferedInputStream br = new BufferedInputStream(stream);
		int c;
		String contents = "";
		try {
			while ((c = br.read()) != -1) {
				contents += (char) c;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Scanner scanner = new Scanner(contents);
		String version = "";
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
			if (line.length() > 0) {
				int index = 0;
				if (line.charAt(0) == '[' && (index = line.indexOf("]")) != -1) {
					version = line.substring(1, index);
				} else {
					/* We have a <B> or <E> tag. */
					String type = "";
					if (line.charAt(0) == '<' && (index = line.indexOf(">")) != -1) {
						type = "" + line.charAt(1);
						index += 1;
					}
					Vector<String> v = new Vector<String>();
					v.add(type);
					v.add(line.substring(index).trim());
					v.add(version);
					rowData.add(v);
				}
			}
		}
	}

	/**
	 * 
	 */
	public static void openAboutDialog() {
		aboutDialog.setLocation((int) (ACMGUI.screenSize.getWidth() / 2 - aboutDialog.getWidth() / 2), (int) (ACMGUI.screenSize.getHeight() / 2 - aboutDialog.getHeight() / 2));
		aboutDialog.setVisible(true);
	}
}

/*
 * Center the text
 */
class CenterRenderer extends DefaultTableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CenterRenderer() {
		setHorizontalAlignment(CENTER);
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		return this;
	}
}