/*
 * FreeHost3270 a suite of terminal 3270 access utilities.
 * Copyright (C) 1998, 2001  Art Gillespie
 * Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *                        Project Contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.sf.freehost3270.proxy;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.Vector;
import java.util.logging.Logger;

/**
 * This class provides the mechanism by which host and client (applet) are able
 * to communicate.
 * 
 * @since 0.1
 */
public class Proxy implements ConnectionMonitor, Runnable {
	private static final Logger log = Logger.getLogger(Proxy.class.getName());

	/** The socket used for listening for client requests. */
	protected ServerSocket mainSocket = null;

	/** The vector of active connections. */
	protected Vector connections = null;

	/** Encryption true/false. */
	boolean encrypt;

	/**
	 * Instantiates a new FreeHost3270 Proxy.
	 * 
	 * @param portNumber
	 *            integer representing the port on which to listen for clients.
	 * @param encryption
	 *            default encryption on or off(t/f).
	 * 
	 * @throws IOException
	 *             DOCUMENT ME!
	 */
	public Proxy(int portNumber, boolean encryption) throws IOException {
		log.info("starting proxy at port: " + portNumber);
		connections = new Vector();
		mainSocket = new ServerSocket(portNumber);
		encrypt = encryption;
	}

	/**
	 * Adds a Connection object to the connections Vector.
	 * 
	 * @param c
	 *            The connection object to add.
	 */
	public void addConnection(Connection c) {
		synchronized (connections) {
			connections.addElement(c);
		}

		log.info("new Session from: " + c.getSrcHost() + "-->"
				+ c.getDestHost());
	}

	/**
	 * Empty method. Satisfies implementation of the Connection Monitor
	 * interface.
	 * 
	 * @param c
	 *            DOCUMENT ME!
	 */
	public void attemptingConnection(Connection c) {
		log.finest("attempting connection");
	}

	/**
	 * Reports connection error on the proxy log with <code>SEVERE</code>
	 * rating.
	 * 
	 * @param c
	 *            DOCUMENT ME!
	 * @param errMsg
	 *            DOCUMENT ME!
	 */
	public void connectionError(Connection c, String errMsg) {
		log.severe("error reported for connection " + c + ": " + errMsg);
	}

	/**
	 * Removes a Connection object from the connections Vector.
	 * 
	 * @param c
	 *            The connection object to remove.
	 */
	public void removeConnection(Connection c) {
		synchronized (connections) {
			connections.removeElement(c);
		}

		log.info("Session Closed for: " + c.getSrcHost() + "-->"
				+ c.getDestHost());
	}

	/**
	 * Starts the thread. This is designed to use up as little overhead as
	 * possible by spawning the Connection object (and it's corresponding
	 * threads) immediately.
	 */
	public void run() {
		log.info("running proxy thread");

		while (!Thread.currentThread().isInterrupted()) {
			try {
				log.fine("waiting for new connections...");

				Socket newSocket = mainSocket.accept();
				log.fine("got new connection");
				new Connection(newSocket, (ConnectionMonitor) this, encrypt);
				log.fine("created Connection successful...");
			} catch (IOException e) {
				e.printStackTrace();
				log.severe(e.getMessage());
			}
		}

		log.info("exiting from the proxy wait connections loop");
	}

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.err.println("The command line options are:");
			System.err.println("-port port - The port to listen to");
			System.err
					.println("-e - optional, if present then encrypt the connections using SSL");
			System.exit(1);
		}

		int port = parsePort(args);

		if (port == -1) {
			log.severe("Port is a required option");
		}
		boolean encrypt = parseEncription(args);
		Proxy prox = new Proxy(port, encrypt);
		prox.run();
		
		
	}

	private static boolean parseEncription(String[] args) {
		for (int i = 0; i < args.length; i++) 
			if (args[i].equals("-e"))
				return true;
		return false;
	}

	private static int parsePort(String[] args) {
		int port = -1;
		String portString = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-port")) {
				try {
					portString = args[i + 1];
					port = Integer.parseInt(portString);
					return port;
				} catch (ArrayIndexOutOfBoundsException e) {
					log
							.severe("We got the -port option, but no port was specified");
					throw e;
				} catch (NumberFormatException e) {
					log.severe("The port value specified (" + portString
							+ ") is not a valid integer.");
					throw e;
				}

			}
			

		}
		return port;
	}
}
