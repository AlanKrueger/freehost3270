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
 * This class represents a thread that is the connection to the client.
 *
 * @since 0.1
 */
public class AgentIn implements Runnable {
    private static final Logger log = Logger.getLogger(AgentIn.class.getName());
    private static int BUFFER_SIZE = 5000;

    /** An agent monitor to notify the AgentOut when this connection dies. */
    private AgentMonitor am = null;

    /** The InputStream from the client. */
    private InputStream in = null;

    /** The OutputStream to the host. */
    private OutputStream out = null;
    private byte[] buffer = null;

    /**
     * Public constructor for creating an AgentIn object.  Called from the
     * Connection object.
     *
     * @param eis EncryptedInputStream object passed from the Connection
     *        object. Connection to client.
     * @param out OutputStream object passed from the Connection object.
     *        Connection to host.
     * @param am AgentMonitor object passed from the Connection object.
     *
     * @see Connection
     */
    public AgentIn(InputStream eis, OutputStream out, AgentMonitor am) {
        this.in = eis;
        this.out = out;
        this.am = am;

        buffer = new byte[BUFFER_SIZE];
        log.info("AgentIn starting...");

        Thread t = new Thread(this);
        t.start();
    }

    /**
     * Start the thread
     */
    public void run() {
        try {
            int bytesRead = 0;

            while (true) {
                if ((bytesRead = in.read(buffer, 0, BUFFER_SIZE)) == -1) {
                    // Connection closed or lost.
                    break;
                } else {
                    out.write(buffer, 0, bytesRead);
                }

                /*
                   FileWriter fw = new FileWriter("debug.this", true);
                   fw.write("FROM CLIENT:\n");
                   for(int i = 0; i < bytesRead; i++)
                   {
                   if(i % 16 == 0)
                   fw.write('\n');
                   int x = 0;
                   if(( x = buffer[i] ) < 0)
                   x += 256;
                
                   fw.write(Integer.toHexString(x) + " ");
                   }
                   fw.write("\n\n\n\n");
                   fw.close();
                 */

                //System.gc();
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.severe(e.getMessage());
        }

        // Once the loop is broken by a -1 return from in.read, the
        // am.agentHasDied method is fired, notifying the
        // corresponding AgentOut that it's time to drop the
        // connection.
        am.agentHasDied(this);
    }
}
