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
import java.io.InputStream;
import java.io.OutputStream;

import java.util.logging.Logger;


/**
 * This class represents a thread that is the connection to the terminal
 * server host.
 *
 * @since 0.1
 */
public class AgentOut implements Runnable {
    private static final Logger log = Logger.getLogger(AgentOut.class.getName());
    private static int BUFFER_SIZE = 4096;

    /** The EncryptedOutputStream to the client. */
    protected OutputStream out = null;

    /** The AgentMonitor for the connection. */
    private AgentMonitor am = null;

    /** The InputStream from the host. */
    private InputStream in = null;
    private byte[] buffer = null;

    /**
     * Public constructor for creating an AgentOut object. Called from the
     * Connection object.
     *
     * @param in InputStream object passed from the Connection object.
     *        Connection to host.
     * @param eos EncryptedOutputStream object passed from the Connection
     *        object. Connection to client.
     * @param am AgentMonitor object passed from the Connection object.
     *
     * @see Connection
     */
    public AgentOut(InputStream in, OutputStream eos, AgentMonitor am) {
        this.in = in;
        this.out = eos;
        this.am = am;
        buffer = new byte[BUFFER_SIZE];

        Thread t = new Thread(this);
        t.start();
        log.info("AgentOut started.");
    }

    /**
     * Start the thread
     */
    public void run() {
        //see AgentIn for additional comments.
        try {
            int bytesRead = 0;

            while (true) {
                if ((bytesRead = in.read(buffer, 0, BUFFER_SIZE)) == -1) {
                    break;
                }

                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.severe(e.getMessage());
        }

        am.agentHasDied(this);
    }
}
