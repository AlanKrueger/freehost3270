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

import net.sf.freehost3270.client.*;
import net.sf.freehost3270.gui.JTerminalScreen;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import java.io.InputStream;

import java.util.*;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;


/**
 * Main application (or applet) frame. TODO:  Implement a paintField(field)
 * and paintChar(RW3270Char) method
 */
public class ApplicationFrame extends JFrame implements ActionListener,
    Runnable, FocusListener {
    private static final Logger log = Logger.getLogger(ApplicationFrame.class.getName());

    /** These font sizes will be presented to the user. */
    public static final int[] FONT_SIZES = { 10, 12, 14, 16, 18, 20, 22 };
    public static final String[] COLOR_NAMES = {
            "Black", "White", "Green", "Red", "Blue", "Orange", "Turquoise",
            "Dark Blue", "Light Green"
        };
    public static final Color[] COLOR_VALUES = {
            Color.BLACK, Color.WHITE, Color.GREEN, Color.RED, Color.BLUE,
            Color.ORANGE, Color.CYAN, new Color(0, 51, 102),
            new Color(204, 255, 204)
        };
    private Font basefont;
    private Font f;
    private Hashtable available;
    private JCheckBoxMenuItem showButtons;
    private JMenuBar menubar;
    private JMenuItem disconnect;

    //    private RH3270Buttons rhbuttons;
    //private Label msg;
    private JTerminalScreen rhp;
    private String defaulthost;
    private String helpabout;
    private String host;
    private boolean encryption;
    private int defaulthostport;
    private int port;

    public ApplicationFrame(String host, int port, String defaulthost,
        Hashtable h, Component c) {
        super("FreeHost3270");

        //helpabout = a.getParameter("helpabout");

        //this.defaulthost = defaulthost;
        encryption = false;

        //System.out.println(helpabout);
        available = h;
        this.port = port;
        this.host = host;
        setResizable(false);

        basefont = new Font("Monospaced", Font.PLAIN, 12);
        f = basefont.deriveFont(Font.PLAIN, 12);

        log.fine("FONT: " + f.getName());

        setLayout(new BorderLayout());
        menubar = new JMenuBar();
        setJMenuBar(menubar);

        JMenu file = new JMenu("File");
        file.add(new JMenuItem("Print"));
        file.addSeparator();
        file.add(new JMenuItem("New Window"));
        menubar.add(file);

        JMenu connect = new JMenu("Connect");
        Enumeration hostKeys = available.keys();

        while (hostKeys.hasMoreElements()) {
            String hostKey = (String) hostKeys.nextElement();
            connect.add(new AbstractAction(hostKey) {
                    public void actionPerformed(ActionEvent evt) {
                        rhp.disconnect();
                        rhp.connect(ApplicationFrame.this.host,
                            ApplicationFrame.this.port,
                            ((Host) ApplicationFrame.this.available.get(
                                evt.getActionCommand())).getHostName(),
                            ((Host) ApplicationFrame.this.available.get(
                                evt.getActionCommand())).getPort(), encryption);
                    }
                });
        }

        //        if(a.getParameter("manualEntry").equals("true"))
        //  connect.add(new MenuItem("Other..."));
        connect.addSeparator();
        connect.add(disconnect = new JMenuItem("Disconnect"));
        disconnect.addActionListener(this);
        menubar.add(connect);

        JMenu options = new JMenu("Options");
        JMenu fonts = new JMenu("Font Size");

        for (int i = 0; i < FONT_SIZES.length; i++) {
            int size = FONT_SIZES[i];
            fonts.add(new AbstractAction(Integer.toString(size, 10)) {
                    public void actionPerformed(ActionEvent evt) {
                        int size = Integer.parseInt(evt.getActionCommand(), 10);
                        fontSize((float) size);
                    }
                });
        }

        options.add(fonts);

        JMenu fontcolor = new JMenu("Font Color");
        JMenu dfFontColor = new JMenu("Default Font");

        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String name = COLOR_NAMES[i];
            dfFontColor.add(new AbstractAction(name) {
                    public void actionPerformed(ActionEvent evt) {
                        String name = evt.getActionCommand();

                        for (int idx = 0; idx < COLOR_NAMES.length; idx++) {
                            if (name.equals(COLOR_NAMES[idx])) {
                                ApplicationFrame.this.rhp.setForegroundColor(COLOR_VALUES[idx]);
                            }
                        }
                    }
                });
        }

        fontcolor.add(dfFontColor);

        JMenu bldFontColor = new JMenu("Bold Font");

        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String name = COLOR_NAMES[i];
            bldFontColor.add(new AbstractAction(name) {
                    public void actionPerformed(ActionEvent evt) {
                        String name = evt.getActionCommand();

                        for (int idx = 0; idx < COLOR_NAMES.length; idx++) {
                            if (name.equals(COLOR_NAMES[idx])) {
                                ApplicationFrame.this.rhp.setBoldColor(COLOR_VALUES[idx]);
                            }
                        }
                    }
                });
        }

        fontcolor.add(bldFontColor);
        options.add(fontcolor);
        options.addSeparator();

        JMenu bgcolor = new JMenu("Background Color");

        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String name = COLOR_NAMES[i];
            bgcolor.add(new AbstractAction(name) {
                    public void actionPerformed(ActionEvent evt) {
                        String name = evt.getActionCommand();

                        for (int idx = 0; idx < COLOR_NAMES.length; idx++) {
                            if (name.equals(COLOR_NAMES[idx])) {
                                ApplicationFrame.this.rhp.setBackgroundColor(COLOR_VALUES[idx]);
                            }
                        }
                    }
                });
        }

        options.add(bgcolor);
        options.addSeparator();
        options.add(showButtons = new JCheckBoxMenuItem("Buttons"));
        menubar.add(options);

        JMenu about = new JMenu("Help");
        menubar.add(about);
        about.add(new JMenuItem("About"));
        rhp = new JTerminalScreen();
        add("Center", rhp);

        //System.out.println(rhp.size().width + " " + rhp.size().height);

        //Center on screen
        Dimension screen_size;

        //System.out.println(rhp.size().width + " " + rhp.size().height);

        //Center on screen
        Dimension frame_size;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(rhp.getSize().width + getInsets().top + getInsets().bottom,
            rhp.getSize().height + getInsets().right + getInsets().left);
        frame_size = this.getSize();

        int offX = frame_size.width;
        int offY = frame_size.height;

        // If we have parent component, offset the new window from
        // it (cascade windows)
        if (c != null) {
            setLocation(c.getLocation().x + 20, c.getLocation().y + 20);
        } else {
            setLocation((screen_size.width - offX) / 2,
                (screen_size.height - offY) / 2);
        }

        if (available.size() == 1) {
            Enumeration el = available.elements();
            Host currentHost = (Host) el.nextElement();
            setTitle("RightHost 3270 - Connecting to " +
                currentHost.getFriendlyName());
            rhp.connect(host, port, currentHost.getHostName(),
                currentHost.getPort(), encryption);
            requestFocus();
            setTitle("RightHost 3270 - Connected to " +
                currentHost.getFriendlyName());
        } else {
            setTitle("RightHost 3270 - Not Connected");
        }

        // setFont(f);
        addFocusListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        log.fine("dispatching action event");

        if (evt.getSource() instanceof JCheckBoxMenuItem) {
            JCheckBoxMenuItem cmi = (JCheckBoxMenuItem) evt.getSource();

            if (cmi.getState()) {
                //System.out.println("Buttons checked...");
                setSize(this.getSize().width, this.getSize().height + 90);

                //RH3270Buttons rhbuttons = new RH3270Buttons(rhp.getRW3270());
                //add("South", rhbuttons);
            } else {
                setSize(this.getSize().width, this.getSize().height - 90);
                remove(getComponent(1));
            }

            return;
        }

        if (evt.getSource() instanceof JMenuItem) {
            if (evt.getActionCommand().equals("Disconnect")) {
                setTitle("RightHost 3270 - Disconnected");
                rhp.disconnect();

                return;
            }

            if (evt.getActionCommand().equals("Print")) {
                rhp.print();

                return;
            }

            if (evt.getActionCommand().equals("New Window")) {
                ApplicationFrame rht = new ApplicationFrame(host, port,
                        defaulthost, available, this);

                return;
            }

            if (evt.getSource() instanceof JMenuItem) {
                if (((JMenuItem) ((JMenuItem) evt.getSource()).getParent()).getText()
                         .equals("Connect")) {
                    rhp.disconnect();

                    if (evt.getActionCommand().equals("Other...")) {
                        Thread t = new Thread(this);

                        //t.start();
                        return;
                    }

                    super.setTitle("RightHost 3270 - Connecting to " +
                        evt.getActionCommand());
                    rhp.connect(host, port,
                        ((Host) available.get(evt.getActionCommand())).getHostName(),
                        ((Host) available.get(evt.getActionCommand())).getPort(),
                        encryption);
                    super.setTitle("RightHost 3270 - Connected to " +
                        evt.getActionCommand());
                }
            }

            if (evt.getActionCommand().equals("About")) {
                // TODO FIXME
            }

            return;
        }

        return;
    }

    public void disconnect() {
        rhp.disconnect();
    }

    public void focusGained(FocusEvent evt) {
        rhp.requestFocus();
    }

    public void focusLost(FocusEvent evt) {
    }

    public void processEvent(AWTEvent evt) {
        if (evt.getID() == Event.WINDOW_DESTROY) {
            rhp.disconnect();
            dispose();

            //System.exit(-1);
        }

        /*if(evt.id == Event.WINDOW_MOVED)
           {
              fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
              int i = 2;
              int window_width = size().width;
              System.out.println(window_width/fm.charWidth(' '));
              do
              {
                 fm = Toolkit.getDefaultToolkit().getFontMetrics(new Font("Courier", Font.PLAIN, i));
                 i++;
              }while(window_width/fm.charWidth(' ') > 80);
        
              fontSize(i);
              return true;
           }*/
        super.processEvent(evt);
    }

    public void run() {
        //  otherHost oh = new otherHost();
        //          while(oh.response < 0){}
        //          if(oh.response == 0)
        //          {
        //              rhp.connect(host, port, oh.host.getText(), Integer.parseInt(oh.port.getText()), encryption);
        //              setTitle("RightHost 3270 - Connected to " + oh.host.getText());
        //              repaint();
        //          }
        //          oh.response = -1;
    }

    private void fontSize(float size) {
        rhp.setFont(rhp.getFont().deriveFont(size));
        setSize(rhp.getSize().width + getInsets().top + getInsets().bottom,
            rhp.getSize().height + getInsets().right + getInsets().left);
        repaint();
    }
}
