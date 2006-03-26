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
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;


/**
 * Main application frame. TODO: Implement a paintField(field) and
 * paintChar(RW3270Char) method
 */
public class ApplicationFrame extends JFrame implements ActionListener,
    FocusListener {
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
    private static final boolean encryption = false;
    private JCheckBoxMenuItem showButtons;
    private JMenuBar menubar;

    //    private RH3270Buttons rhbuttons;
    private JTerminalScreen rhp;
    private Map available;
    private String host;
    private int port;

    /**
     * No-ops constructor. Asks users to enter the connection settings in the
     * corresponding dialog box then proceeds as normal.
     */
    public ApplicationFrame() {
        super("Freehost3270");

        EditHostFrame edhFrame = new EditHostFrame();
        edhFrame.setVisible(true);

        if (edhFrame.getResult() == 1) {
            init(edhFrame.getHost(), edhFrame.getPort(), null, null);
        } else {
            exit();
        }
    }

    public ApplicationFrame(String host, int port, Map available) {
        this(host, port, available, null);
    }

    public ApplicationFrame(String host, int port, Map available, JFrame parent) {
        super("FreeHost3270");
        init(host, port, available, parent);
    }

    public void actionPerformed(ActionEvent evt) {
        log.fine("dispatching action event");
    }

    public void disconnect() {
        rhp.disconnect();
    }

    /**
     * Shuts the application down.
     */
    public void exit() {
        if (rhp != null) {
            rhp.disconnect();
        }

        dispose();
        System.exit(0);
    }

    public void focusGained(FocusEvent evt) {
        rhp.requestFocus();
    }

    public void focusLost(FocusEvent evt) {
    }

    public void processEvent(AWTEvent evt) {
        if (evt.getID() == Event.WINDOW_DESTROY) {
            exit();
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

    /**
     * Builds main menu. Constructs several menu items.
     */
    private void buildMainMenu() {
        menubar = new JMenuBar();
        setJMenuBar(menubar);

        JMenu file = new JMenu("Terminal");

        //file.add(new JMenuItem("Print"));
        //file.add(new JMenuItem("New Window"));
        //file.addSeparator();
        file.add(new AbstractAction("Exit") {
                public void actionPerformed(ActionEvent evt) {
                    ApplicationFrame.this.exit();
                }
            });
        menubar.add(file);

        JMenu connect = new JMenu("Connect");

        Iterator hostKeys = available.keySet().iterator();

        while (hostKeys.hasNext()) {
            String hostKey = (String) hostKeys.next();
            connect.add(new AbstractAction(hostKey) {
                    public void actionPerformed(ActionEvent evt) {
                        ApplicationFrame.this.disconnect();

                        try {
                            rhp.connect(ApplicationFrame.this.host,
                                ApplicationFrame.this.port);
                        } catch (Exception e) {
                            showConnectionErrorDialog(e.getMessage());
                        }
                    }
                });
        }

        connect.add(new AbstractAction("Other...") {
                public void actionPerformed(ActionEvent evt) {
                    EditHostFrame edhFrame = new EditHostFrame();
                    edhFrame.setVisible(true);

                    if (edhFrame.getResult() == 1) {
                        ApplicationFrame.this.disconnect();

                        try {
                            rhp.connect(edhFrame.getHost(), edhFrame.getPort());
                        } catch (Exception e) {
                            showConnectionErrorDialog(e.getMessage());
                        }

                        rhp.requestFocusInWindow();
                    }
                }
            });
        connect.addSeparator();

        connect.add(new AbstractAction("Disconnect") {
                public void actionPerformed(ActionEvent evt) {
                    ApplicationFrame.this.disconnect();
                }
            });

        menubar.add(connect);

        JMenu options = new JMenu("Options");

        JMenu fonts = new JMenu("Font Size");
        ButtonGroup fontsGroup = new ButtonGroup();

        for (int i = 0; i < FONT_SIZES.length; i++) {
            int size = FONT_SIZES[i];

            JRadioButtonMenuItem sizeItem = new JRadioButtonMenuItem(new AbstractAction(
                        Integer.toString(size, 10)) {
                        public void actionPerformed(ActionEvent evt) {
                            int size = Integer.parseInt(evt.getActionCommand(),
                                    10);
                            fontSize((float) size);
                        }
                    });

            if (size == JTerminalScreen.DEFAULT_FONT_SIZE) {
                sizeItem.setSelected(true);
            }

            fonts.add(sizeItem);
            fontsGroup.add(sizeItem);
        }

        options.add(fonts);

        JMenu fontcolor = new JMenu("Font Color");
        JMenu dfFontColor = new JMenu("Default Font");
        ButtonGroup fontColorGroup = new ButtonGroup();

        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String name = COLOR_NAMES[i];
            JRadioButtonMenuItem colorItem = new JRadioButtonMenuItem(new AbstractAction(
                        name) {
                        public void actionPerformed(ActionEvent evt) {
                            String name = evt.getActionCommand();

                            for (int idx = 0; idx < COLOR_NAMES.length;
                                    idx++) {
                                if (name.equals(COLOR_NAMES[idx])) {
                                    ApplicationFrame.this.rhp.setForegroundColor(COLOR_VALUES[idx]);
                                }
                            }
                        }
                    });

            if (COLOR_VALUES[i] == JTerminalScreen.DEFAULT_FG_COLOR) {
                colorItem.setSelected(true);
            }

            dfFontColor.add(colorItem);
            fontColorGroup.add(colorItem);
        }

        fontcolor.add(dfFontColor);

        JMenu bldFontColor = new JMenu("Bold Font");
        ButtonGroup bldFontGroup = new ButtonGroup();

        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String name = COLOR_NAMES[i];
            JRadioButtonMenuItem colorItem = new JRadioButtonMenuItem(new AbstractAction(
                        name) {
                        public void actionPerformed(ActionEvent evt) {
                            String name = evt.getActionCommand();

                            for (int idx = 0; idx < COLOR_NAMES.length;
                                    idx++) {
                                if (name.equals(COLOR_NAMES[idx])) {
                                    ApplicationFrame.this.rhp.setBoldColor(COLOR_VALUES[idx]);
                                }
                            }
                        }
                    });

            if (COLOR_VALUES[i] == JTerminalScreen.DEFAULT_BOLD_COLOR) {
                colorItem.setSelected(true);
            }

            bldFontColor.add(colorItem);
            bldFontGroup.add(colorItem);
        }

        fontcolor.add(bldFontColor);
        options.add(fontcolor);
        options.addSeparator();

        JMenu bgcolor = new JMenu("Background Color");
        ButtonGroup bgcolorGroup = new ButtonGroup();

        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String name = COLOR_NAMES[i];
            JRadioButtonMenuItem colorItem = new JRadioButtonMenuItem(new AbstractAction(
                        name) {
                        public void actionPerformed(ActionEvent evt) {
                            String name = evt.getActionCommand();

                            for (int idx = 0; idx < COLOR_NAMES.length;
                                    idx++) {
                                if (name.equals(COLOR_NAMES[idx])) {
                                    ApplicationFrame.this.rhp.setBackgroundColor(COLOR_VALUES[idx]);
                                }
                            }
                        }
                    });

            if (COLOR_VALUES[i] == JTerminalScreen.DEFAULT_BG_COLOR) {
                colorItem.setSelected(true);
            }

            bgcolor.add(colorItem);
            bgcolorGroup.add(colorItem);
        }

        options.add(bgcolor);

        //options.addSeparator();
        //options.add(showButtons = new JCheckBoxMenuItem("Buttons"));
        menubar.add(options);

        JMenu about = new JMenu("Help");
        menubar.add(about);
        about.add(new AbstractAction("About") {
                public void actionPerformed(ActionEvent evt) {
                    AboutFrame about = new AboutFrame(ApplicationFrame.this);
                    about.setVisible(true);
                }
            });
        rhp = new JTerminalScreen();
        add("Center", rhp);
    }

    private void fontSize(float size) {
        rhp.setFont(rhp.getFont().deriveFont(size));
        setSize(rhp.getSize().width + getInsets().top + getInsets().bottom,
            rhp.getSize().height + getInsets().right + getInsets().left);
        repaint();
    }

    /**
     * Performs operations neccessary to construct main application frame.
     *
     * @param host DOCUMENT ME!
     * @param port DOCUMENT ME!
     * @param available DOCUMENT ME!
     * @param parent DOCUMENT ME!
     */
    private void init(String host, int port, Map available, JFrame parent) {
        if (available == null) {
            available = new Hashtable();
            available.put(host, new Host(host, port, host));
        }

        this.available = available;

        this.port = port;
        this.host = host;
        setResizable(false);

        setLayout(new BorderLayout());

        buildMainMenu();

        // Center on screen
        Dimension screen_size;
        Dimension frame_size;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(rhp.getSize().width + getInsets().top + getInsets().bottom,
            rhp.getSize().height + getInsets().right + getInsets().left);
        frame_size = this.getSize();

        int offX = frame_size.width;

        int offY = frame_size.height;

        // If we have parent component, offset the new window from
        // it (cascade windows)
        if (parent != null) {
            setLocation(parent.getLocation().x + 20,
                parent.getLocation().y + 20);
        } else {
            setLocation((screen_size.width - offX) / 2,
                (screen_size.height - offY) / 2);
        }

        // Connect to the first host in the set of available hosts in
        // case if there is only one host available
        if (available.size() == 1) {
            Iterator el = available.values().iterator();
            Host currentHost = (Host) el.next();

            setTitle("Free Host 3270 - Connecting to " +
                currentHost.getFriendlyName());

            try {
                rhp.connect(host, port);
                requestFocus();
                setTitle("Free Host 3270 - Connected to " +
                    currentHost.getFriendlyName());
            } catch (Exception e) {
                showConnectionErrorDialog(e.getMessage());
            }
        } else {
            setTitle("Free Host 3270 - Not Connected");
        }

        addFocusListener(this);
    }

    private void showConnectionErrorDialog(String message) {
        JOptionPane.showMessageDialog(rhp,
            "Failed to connect to the server:\n" + message,
            "Connection failure", JOptionPane.WARNING_MESSAGE);
    }
}
