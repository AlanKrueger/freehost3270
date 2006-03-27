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


package net.sf.freehost3270.applet;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JApplet;
import javax.swing.JToolBar;

import net.sf.freehost3270.client.Host;
import net.sf.freehost3270.gui.JTerminalScreen;


/**
 * An applet that launches the Freehost terminal emulator GUI client.
 */
public class FreeHostApplet extends JApplet implements ComponentListener {
    private static final Logger log = Logger.getLogger(FreeHostApplet.class.getName());
    private JTerminalScreen scr;
    private JToolBar toolBar;
    private Map hosts = new Hashtable();
    private String proxyHost = null;
    private int proxyPort;

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        if (e.getSource() == scr) {
            validate();
        }
    }

    public void componentShown(ComponentEvent e) {
    }

    public void connect() {
        log.info("connecting");

        if (scr != null) {
            Host dest = (Host) (hosts.values().toArray())[0];

            // TODO: make destination host selectable
            scr.connect(proxyHost, proxyPort, dest.getHostName(),
                dest.getPort(), false);
        }

        log.info("connected");
    }

    public void destroy() {
        disconnect();
    }

    public void disconnect() {
        if (scr != null) {
            scr.disconnect();
        }
    }

    public void init() {
        String proxyPortStr = getParameter("proxy-port");

        proxyHost = getDocumentBase().getHost();

        if (proxyPortStr == null) {
            proxyPort = 6728;
            log.warning("proxy port is not specified, using default " +
                proxyPort);
        } else {
            proxyPort = Integer.parseInt(proxyPortStr, 10);
        }

        String available = getParameter("avail-hosts");

        StringTokenizer st = new StringTokenizer(available, "|");

        while (st.hasMoreElements()) {
            String hostName = st.nextToken();
            int hostPort = Integer.parseInt(st.nextToken());
            String friendlyName = st.nextToken();
            hosts.put(friendlyName, new Host(hostName, hostPort, friendlyName));
        }

        //         rht = new RHTest(getParameter("RightHostServer"),
        //                 Integer.parseInt(getParameter("RightHostPort")),
        //                 getParameter("DefaultHost"), hosts, null);
        buildGui();
    }

    public void redrawScreen() {
        if (scr != null) {
            resize(scr.getSize());
            scr.renderScreen();
            scr.repaint();
        }
    }

    private void buildGui() {
        Container contentPane = getContentPane();

        toolBar = new JToolBar();
        toolBar.add(new AbstractAction("Connect") {
                public void actionPerformed(ActionEvent e) {
                    FreeHostApplet.this.connect();
                }
            });
        toolBar.add(new AbstractAction("Disconnect") {
                public void actionPerformed(ActionEvent e) {
                    FreeHostApplet.this.disconnect();
                }
            });
        toolBar.add(new AbstractAction("Redraw") {
                public void actionPerformed(ActionEvent e) {
                    FreeHostApplet.this.redrawScreen();
                }
            });
        toolBar.add(new AbstractAction("Print") {
        	public void actionPerformed(ActionEvent e) {
        		PrinterJob printJob = PrinterJob.getPrinterJob();
        		printJob.setPrintable(scr);
        		if (printJob.printDialog()) {
        			try { 
        				printJob.print();
        			} catch(PrinterException pe) {
        				System.out.println("Error printing: " + pe);
        			}
        		}
        	}
        });
        contentPane.add(toolBar, BorderLayout.NORTH);

        scr = new JTerminalScreen();
        contentPane.add(scr);
        this.addKeyListener(scr);
        connect();
        scr.requestFocusInWindow();
    }
}
