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


package net.sf.freehost3270.client;

import junit.framework.TestCase;


/**
 * A primitive test case for that tests <code>Host</code> struct.
 */
public class HostTest extends TestCase {
    public void testHostConstruct() {
        String hostName = "host.name";
        int port = 10;
        String friendlyName = "friendly.name";

        Host h = new Host(hostName, port, friendlyName);

        assertTrue(hostName.equals(h.getHostName()));
        assertTrue(friendlyName.equals(h.getFriendlyName()));
        assertTrue(port == h.getPort());
    }
}
