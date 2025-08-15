package com.acm.main.gui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.acm.main.ACM;
import com.acm.main.gui.ACMGUI;


public class FeedbackDialog {

	private static boolean sendMessage(String content) {
		try {
			URL url = new URL(FeedbackDialog.REPORTING_URL + "?" + content + "&ip=" + InetAddress.getLocalHost().getHostAddress());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			if (connection.getResponseCode() != 200) {
				return false;
			}
			return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void setUpFeedbackDialog() {
		FeedbackDialog.feedbackDialog = new JDialog(ACMGUI.mainDialog, "Send Feedback");
		FeedbackDialog.feedbackDialog.setSize((int) ACMGUI.screenSize.getWidth() * 3 / 10, (int) ACMGUI.screenSize.getHeight() * 2 / 5);
		FeedbackDialog.feedbackDialog.setAlwaysOnTop(true);
		FeedbackDialog.feedbackDialog.setResizable(false);
	
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
	
		Font smallFont = new Font("Arial", Font.BOLD, 11);
		JRadioButton bugsButton = new JRadioButton("Bugs");
		bugsButton.setFont(smallFont);
		bugsButton.setFocusable(false);
		bugsButton.setActionCommand("bugs");
		JRadioButton feedbackButton = new JRadioButton("Feedback");
		feedbackButton.setActionCommand("feedback");
		feedbackButton.setFocusable(false);
		feedbackButton.setFont(smallFont);
		JRadioButton suggestionsButton = new JRadioButton("Suggestions");
		suggestionsButton.setActionCommand("suggestions");
		suggestionsButton.setFont(smallFont);
		suggestionsButton.setFocusable(false);
		final ButtonGroup group = new ButtonGroup();
		group.add(bugsButton);
		group.add(feedbackButton);
		group.setSelected(feedbackButton.getModel(), true);
		group.add(suggestionsButton);
	
		JPanel radioPanel = new JPanel(new FlowLayout());
		radioPanel.add(bugsButton);
		radioPanel.add(feedbackButton);
		radioPanel.add(suggestionsButton);
		panel.add(radioPanel);
	
		JPanel subjectPanel = new JPanel();
		subjectPanel.setLayout(new BoxLayout(subjectPanel, BoxLayout.LINE_AXIS));
		final JTextField subjectField = new JTextField("");
		Dimension panelDimensions = new Dimension(FeedbackDialog.feedbackDialog.getWidth(), (int) subjectField.getPreferredSize().getHeight());
		subjectPanel.setPreferredSize(panelDimensions);
		subjectPanel.setSize(panelDimensions);
		JLabel subjectLabel = new JLabel("<html><b>Subject</b> <i>(Optional)</i>: </html>");
		subjectLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		subjectPanel.add(Box.createHorizontalStrut(5));
		subjectPanel.add(subjectLabel);
		Dimension subjectFieldDimensions = new Dimension((int) (subjectPanel.getWidth() - subjectLabel.getPreferredSize().getWidth() - 20), (int) subjectField.getPreferredSize().getHeight());
		subjectField.setPreferredSize(subjectFieldDimensions);
		subjectField.setSize(subjectFieldDimensions);
		subjectPanel.add(subjectField);
		subjectPanel.add(Box.createHorizontalStrut(5));
		panel.add(subjectPanel);
	
		panel.add(Box.createVerticalStrut(5));
	
		JPanel emailPanel = new JPanel();
		emailPanel.setLayout(new BoxLayout(emailPanel, BoxLayout.LINE_AXIS));
		final JTextField emailField = new JTextField("");
		emailPanel.setPreferredSize(panelDimensions);
		emailPanel.setSize(panelDimensions);
		JLabel emailLabel = new JLabel("<html><b>Email</b> <i>(Optional)</i>: </html>");
		emailLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		emailPanel.add(Box.createHorizontalStrut(5));
		emailPanel.add(emailLabel);
		int spacing = (int) (subjectLabel.getPreferredSize().getWidth() - emailLabel.getPreferredSize().getWidth());
		emailPanel.add(Box.createHorizontalStrut(spacing));
		Dimension emailFieldDimensions = new Dimension((int) (emailPanel.getWidth() - emailLabel.getPreferredSize().getWidth() - 20 - spacing), (int) emailField.getPreferredSize().getHeight());
		emailField.setPreferredSize(emailFieldDimensions);
		emailField.setSize(emailFieldDimensions);
		emailPanel.add(emailField);
		emailPanel.add(Box.createHorizontalStrut(5));
		panel.add(emailPanel);
	
		panel.add(Box.createVerticalStrut(5));
	
		final JTextArea feedbackBox = new JTextArea();
		feedbackBox.setBorder(BorderFactory.createLineBorder(new Color(50, 150, 225), 2));
	
		JScrollPane feedbackScroll = new JScrollPane(feedbackBox);
		feedbackScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		feedbackBox.setLineWrap(true);
		feedbackBox.setWrapStyleWord(true);
		feedbackScroll.setPreferredSize(new Dimension(FeedbackDialog.feedbackDialog.getWidth(), FeedbackDialog.feedbackDialog.getHeight() * 200));
		feedbackScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 150, 225), 2));
		panel.add(feedbackScroll);
	
		JButton sendButton = new JButton("Send");
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String message = feedbackBox.getText();
				if (message.trim().length() == 0) {
					JOptionPane.showMessageDialog(FeedbackDialog.feedbackDialog, "Please enter a message before sending.", "Feedback needed", JOptionPane.ERROR_MESSAGE);
					return;
				}
	
				String URL = "";
				try {
					URL = "&type=" + group.getSelection().getActionCommand() + "&version=" + URLEncoder.encode(ACM.VERSIONID, "UTF-8") + "&subject=" + URLEncoder.encode(subjectField.getText(), "UTF-8") + "&email="
							+ URLEncoder.encode(emailField.getText(), "UTF-8") + "&message=" + URLEncoder.encode(message, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					JOptionPane.showMessageDialog(FeedbackDialog.feedbackDialog, "Sorry, an error occurred during sending of your message.\nPlease try again later.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
	
				if (sendMessage(URL)) {
					JOptionPane.showMessageDialog(FeedbackDialog.feedbackDialog, "Your message has been successfully sent. Thanks for your feedback!", "Feedback Sent!", JOptionPane.INFORMATION_MESSAGE);
					subjectField.setText("");
					feedbackBox.setText("");
					feedbackBox.grabFocus();
				} else {
					JOptionPane.showMessageDialog(FeedbackDialog.feedbackDialog, "Sorry, an error occurred during sending of your message.\nPlease try again later.", "Error", JOptionPane.ERROR_MESSAGE);
				}
				FeedbackDialog.feedbackDialog.setVisible(false);
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				feedbackBox.setText("");
				feedbackBox.grabFocus();
				FeedbackDialog.feedbackDialog.setVisible(false);
			}
		});
		JPanel buttonPanel = new JPanel(new GridLayout(1, 0));
		buttonPanel.add(sendButton);
		buttonPanel.add(cancelButton);
		panel.add(buttonPanel);
	
		FeedbackDialog.feedbackDialog.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent arg0) {
				feedbackBox.grabFocus();
			}
	
			public void windowLostFocus(WindowEvent arg0) {
			}
		});
	
		FeedbackDialog.feedbackDialog.add(panel);
	}

	private static final String REPORTING_URL = "http://gaardian.com/cgi-bin/test.pl";
	public static JDialog feedbackDialog;

}
