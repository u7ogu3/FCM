package com.acm.main.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import com.acm.clip.ClipSlots;
import com.acm.main.ACM;
import com.acm.main.gui.ACMClip;
import com.acm.main.gui.ACMClipSlots;

public class Network extends Thread {

	private static final String GET_CLIP_BUTTONS = "getClipButtons";

	public static boolean networked = false;
	public static int networkPort = 6999;
	public static boolean networkSupport = false;
	public static String networkPassword = "";
	public static int refreshTime = 60000;

	private Reciever reciever;
	private ServerSocket ss = null;
	private LinkedList<Sender> senders = null;

	public static void init(int port) throws Exception {
		if (!networked && networkSupport) {
			ACM.network = new Network(false, null, port, null);
		}
	}

	public Network(boolean connect, String ip, int port, String password) throws Exception {
		if (!connect) {
			ss = new ServerSocket(port);
			senders = new LinkedList<Sender>();
			this.start();
		} else {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(ip, port), 30000);
			reciever = new Reciever(socket, password);
		}
	}

	public LinkedList<Sender> getSenders() {
		return senders;
	}

	public Reciever getReciever() {
		return reciever;
	}

	public void close() {
		for (Sender sender : senders) {
			try {
				sender.socket.close();
			} catch (IOException e) {
			}
		}
		try {
			ss.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void run() {
		while (networkSupport) {
			try {
				Socket socket = ss.accept();
				Sender sender = new Sender(socket);
				sender.start();
				senders.add(sender);
				sleep(100);
			} catch (Exception e) {
				System.out.println("Could not accept connection");
				break;
			}
		}
	}

	public class Reciever {

		Socket socket = null;
		PrintWriter pw;
		ObjectInputStream oi;
		String password;

		public Reciever(Socket socket, String password) throws Exception {
			this.socket = socket;
			this.password = password;
			pw = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), true);
			oi = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		}

		public ACMClipSlots recieveClipButtons() throws Exception {
			pw.println(password);
			pw.println(GET_CLIP_BUTTONS);
			ACMClipSlots aCMClipSlots = (ACMClipSlots) oi.readObject();
			return aCMClipSlots;
		}

		public Image recieveImage(ACMClip button) throws Exception {
			pw.println(button.getId());
			return ImageIO.read(new BufferedInputStream(socket.getInputStream()));
		}
	}

	public class Sender extends Thread {

		Socket socket = null;
		BufferedReader br;
		ObjectOutputStream oo;

		public Sender(Socket socket) throws Exception {
			this.socket = socket;
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			oo = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			oo.flush();
		}

		public void run() {
			try {
				String command;
				command = br.readLine();
				if (!command.equals(networkPassword)) {
					br.close();
					oo.close();
					socket.close();
				} else {
					while ((command = br.readLine()) != null) {
						if (command.equals(GET_CLIP_BUTTONS)) {
							sendClipButtons(ACM.acm.getClips());
						} else {
							int numButton = Integer.parseInt(command);
							sendImage(ACM.acm.getClips().getButton(numButton).getMetaData().imageObject);
						}
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		}

		public void sendClipButtons(ClipSlots clips) throws Exception {
			oo.writeObject(clips);
			oo.flush();
		}

		public void sendImage(Image image) throws Exception {
			ImageIO.write((BufferedImage) image, "bmp", new BufferedOutputStream(socket.getOutputStream()));
		}
	}
}
