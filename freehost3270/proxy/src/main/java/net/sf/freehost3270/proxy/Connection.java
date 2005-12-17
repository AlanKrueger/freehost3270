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

import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;

import java.util.Date;
import java.util.logging.Logger;


/**
 * This class is the 'container' that represents a discrete client/host
 * session.
 *
 * @since 0.1
 */
public class Connection implements Runnable, AgentMonitor {
    private static final Logger log = Logger.getLogger(Connection.class.getName());

    /** The default destination terminal server port number. */
    public static final int DEFAULT_DESTINATION_PORT = 23;

    /**
     * An AgentIn object for the connection to the client.
     *
     * @see AgentIn
     */
    protected AgentIn fromSrcToDest = null;

    /**
     * An AgentOut object for the connection to the host.
     *
     * @see AgentOut
     */
    protected AgentOut fromDestToSrc = null;

    /** When the connection object was created */
    protected Date sessionStarted = null;

    /** A socket for the host's connection. */
    protected Socket destSocket = null;

    /** A socket for the client's connection. */
    protected Socket srcSocket = null;

    /** A ConnecitonMonitor for the connection. */
    private ConnectionMonitor cm = null;

    /** Input stream <B>FROM</B> the host. */
    private InputStream destIn = null;

    /**
     * For secure communications <B>FROM</B> the client.
     *
     * @see EncryptedInputStream
     */
    private InputStream eis = null;

    /** Input stream <B>FROM</B> the client. */
    private InputStream srcIn = null;

    /** Ouput stream <B>TO</B> the host. */
    private OutputStream destOut = null;

    /**
     * For secure communications <B>TO</B> the client.
     *
     * @see EncryptedOutputStream
     */
    private OutputStream eos = null;

    /** Output stream <B>TO</B> the client. */
    private OutputStream srcOut = null;

    /** The default host. */
    private String destHost = null;

    /** Is the connection closed? */
    private boolean connectionClosed = false;

    /** Encryption on/off (t/f) */
    private boolean encrypt = true;

    /** The current destination terminal server port number. */
    private int destPort = DEFAULT_DESTINATION_PORT;

    /**
     * Called as soon as the server socket receives a request on this port.
     *
     * @param srcSocket Socket representing the clients request.
     * @param cm ConnectionMonitor object to provide feedback on the
     *        connections state.
     * @param encryption on/off(t/f)
     */
    public Connection(Socket srcSocket, ConnectionMonitor cm,
        boolean encryption) {
        try {
            log.fine("creating new Connection");
            this.srcSocket = srcSocket;
            this.cm = cm;

            cm.attemptingConnection(this);
            sessionStarted = new Date();
            encrypt = encryption;

            // Establish read/write for the socket
            srcIn = srcSocket.getInputStream();
            srcOut = srcSocket.getOutputStream();

            eos = srcOut;
            eis = srcIn;

            //check for host info;
            int hostNameLen = 0;
            byte[] hostNameIn = new byte[512];
            byte thisByte = 0x00;

            // the client will send 0xCC when it
            // is done sending the hostname, if any.
            log.fine("reading host request...");

            while (thisByte != (byte) 0xCC) {
                thisByte = (byte) eis.read();
                hostNameIn[hostNameLen] = thisByte;
                hostNameLen++;
            }

            log.fine("host request was " + hostNameLen + " bytes long");

            // the byte immediately following 0xCC will
            // be the port number.
            byte port = (byte) eis.read();
            byte[] hostNameDone = new byte[hostNameLen - 1];
            System.arraycopy(hostNameIn, 0, hostNameDone, 0, hostNameLen - 1);

            if (hostNameLen > 1) {
                //if the new string != "", then we have a client-assigned host.
                destHost = new String(hostNameDone, "ASCII").trim();
                destPort = (int) port;
                log.fine("connecting to: " + destHost + ":" + destPort);
            }

            Thread t = new Thread(this);
            t.start();
        } catch (Exception anException) {
            anException.printStackTrace();
            log.severe(anException.getMessage());
        }
    }

    /**
     * Called when the connection to the client is lost.
     *
     * @param a The connection to the client.
     *
     * @see AgentIn
     */
    public synchronized void agentHasDied(AgentIn a) {
        if (connectionClosed) {
            return;
        }

        kill();

        //         closeSrc();
        //         closeDest();
    }

    /**
     * Called when the connection to the host is lost.
     *
     * @param a the connection to the host.
     *
     * @see AgentOut
     */
    public synchronized void agentHasDied(AgentOut a) {
        if (connectionClosed) {
            return;
        }

        kill();

        //         closeSrc();
        //         closeDest();
    }

    /**
     * Returns the destination terminal server host name.
     *
     * @return current destination host's hostname.
     */
    public String getDestHost() {
        return (destSocket.getInetAddress().toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @return current destination host's port
     */
    public int getDestPort() {
        return destPort;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the clients hostname.
     */
    public String getSrcHost() {
        return (srcSocket.getInetAddress().toString());
    }

    /**
     * Kills current connection, halts the <code>Connection</code> thread.
     * Removes current connection from the connection monitor.
     */
    public void kill() {
        closeDest();
        closeSrc();
        cm.removeConnection(this);
        connectionClosed = true;
    }

    /**
     * Starts the connection thread.
     */
    public void run() {
        log.fine("starting the proxy connection thread");

        if (!connectToDest()) {
            // if the connection to the host fails,
            // dump the client.
            closeSrc();
        } else {
            //System.out.println("Successfully connected to destination...");
            // Ok, we're all ready ... since we've gotten this far,
            // add ourselves into the connection list
            //System.out.println("Adding connection...");
            cm.addConnection(this);

            //System.out.println("Checking Encryption...");
            // Create our two agents
            fromSrcToDest = new AgentIn(eis, destOut, this);
            fromDestToSrc = new AgentOut(destIn, eos, this);

            //System.out.println("Done...");
            // No need for our thread to continue, we'll be notified if
            // either of our agents dies
        }

        while (!connectionClosed && !Thread.currentThread().isInterrupted()) {
            // make this connection thread live till it is interrupted
            // or connection is closed.
        }

        log.fine("exiting the proxy connection thread run method");
    }

    /**
     * Closes the connection to the destination terminal server host.
     */
    private void closeDest() {
        try {
            destIn.close();
            destOut.close();
            destSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.severe(e.getMessage());
        }
    }

    /**
     * Closes the connection to the client.
     */
    private void closeSrc() {
        try {
            srcIn.close();
            srcOut.close();
            srcSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.severe(e.getMessage());
        }
    }

    /**
     * Establishes a connection to the destination terminal server host for
     * this Connection instance.
     *
     * @return DOCUMENT ME!
     */
    private boolean connectToDest() {
        try {
            // Ok, we've got the host name and port to which we wish to
            // connect, try to establish a connection
            log.finest("creating new dest socket to " + destHost + " " +
                destPort);
            destSocket = new Socket(destHost, destPort);
            destIn = destSocket.getInputStream();
            destOut = destSocket.getOutputStream();
            log.finest("successfully created the dest socket");
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("error creating dest socket, " + e.getMessage());
            cm.connectionError(this,
                "error connecting to " + destHost + "|" + destPort + ", " +
                e.getMessage());

            return false;
        }

        return true;
    }
}
