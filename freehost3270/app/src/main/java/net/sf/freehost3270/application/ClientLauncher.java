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


package net.sf.freehost3270.application;

import net.sf.freehost3270.application.ApplicationFrame;
import net.sf.freehost3270.client.Host;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Logger;


/**
 * Freehost standalone terminal emulator GUI client launcher. This class
 * defines a <code>main</code> method, thus, making it possible to use
 * Freehost3270 as a standalone application. Configuration parameters are
 * fetched from the <code>System</code> properties. So, connection settings
 * should be set up on the command line.
 *
 * @see #KEY_AVAILABLE
 * @see #KEY_SERVERNAME
 * @see #KEY_SERVERPORT
 * @since 0.2
 */
public class ClientLauncher {
    private static final Logger log = Logger.getLogger(ClientLauncher.class.getName());
    public static final String KEY_AVAILABLE = "net.sf.freehost3270.available";
    public static final String KEY_SERVERNAME = "net.sf.freehost3270.servername";
    public static final String KEY_SERVERPORT = "net.sf.freehost3270.serverport";

    /**
     * Launches the stand-alone client application.
     *
     * @param args DOCUMENT ME!
     */
    public static void main(String[] args) {
        ApplicationFrame appFrame;
        log.info("launching FreeHost standalone GUI client");
        log.info("with parameters:");
        log.info(KEY_AVAILABLE + " = " + System.getProperty(KEY_AVAILABLE));
        log.info(KEY_SERVERNAME + " = " + System.getProperty(KEY_SERVERNAME));
        log.info(KEY_SERVERPORT + " = " + System.getProperty(KEY_SERVERPORT));

        String available = System.getProperty(KEY_AVAILABLE);
        String fhServerName = System.getProperty(KEY_SERVERNAME);
        String fhServerPortStr = System.getProperty(KEY_SERVERPORT);

        if ((available != null) && (fhServerName != null) &&
                (fhServerPortStr != null)) {
            int fhServerPort = 23;

            try {
                fhServerPort = Integer.parseInt(fhServerPortStr);
            } catch (NumberFormatException e) {
                log.severe(e.getMessage());
                e.printStackTrace();
            }

            Hashtable hosts = new Hashtable();
            StringTokenizer st = new StringTokenizer(available, "|");

            while (st.hasMoreElements()) {
                String hostName = st.nextToken();
                int hostPort = Integer.parseInt(st.nextToken());
                String friendlyName = st.nextToken();
                hosts.put(friendlyName,
                    new Host(hostName, hostPort, friendlyName));
            }

            appFrame = new ApplicationFrame(fhServerName, fhServerPort, hosts);
        } else {
            appFrame = new ApplicationFrame();
        }

        appFrame.setVisible(true);
    }
}
