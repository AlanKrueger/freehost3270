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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * Dialog that serves as a widget to specify connection settings to the host
 * server.
 *
 * @since 0.2
 */
public class EditHostFrame extends JDialog implements ActionListener,
    PropertyChangeListener {
    private static final String okString = "Ok";
    private static final String cancelString = "Cancel";
    private JCheckBox useEncryptionField;
    private JFormattedTextField portField;
    private JOptionPane optionPane;
    private JTextField hostField;
    private String hostName = "";
    private boolean useEncryption = false;
    private int portNumber;
    private int response;
    private int result = 0;

    public EditHostFrame() {
        this(null, null, null);
    }

    public EditHostFrame(Frame owner, String hostName, Integer portNumber) {
        this(null, null, null, false);
    }

    /**
     * Constructs a new edit host connection settings dialog window.
     *
     * @param owner DOCUMENT ME!
     * @param hostName default value for host name. If <code>null</code> none
     *        is used.
     * @param portNumber default value for port number. If <code>null</code>
     *        none is used.
     * @param useEncryption default state for 'use encryption' check box.
     */
    public EditHostFrame(Frame owner, String hostName, Integer portNumber,
        boolean useEncryption) {
        super(owner, true);

        setTitle("Edit connection settings");

        hostField = new JTextField(10);
        portField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        portField.setColumns(4);
        useEncryptionField = new JCheckBox();
        useEncryptionField.setSelected(useEncryption);

        Object[] controls = {
                "Specify target host server:", "Host name", hostField,
                "Port number", portField, "Use encryption", useEncryptionField,
            };

        Object[] options = { okString, cancelString };

        optionPane = new JOptionPane(controls, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, options, options[0]);

        setContentPane(optionPane);

        // Handle window closing event
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
                }
            });

        // ensure the Host Name field gets the focus
        addComponentListener(new ComponentAdapter() {
                public void componentShown(ComponentEvent ce) {
                    hostField.requestFocusInWindow();
                }
            });

        optionPane.addPropertyChangeListener(this);

        pack();

        // center dialog frame on the desktop (root window)
        Dimension frame_size;

        // center dialog frame on the desktop (root window)
        Dimension screen_size;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        frame_size = this.getSize();

        int offX = (int) frame_size.getWidth();
        int offY = (int) frame_size.getHeight();
        setLocation((screen_size.width - offX) / 2,
            (screen_size.height - offY) / 2);
    }

    public void actionPerformed(ActionEvent evt) {
    }

    /**
     * This method clears the dialog and hides it.
     */
    public void clearAndHide() {
        hostField.setText(null);
        setVisible(false);
    }

    /**
     * Returns host name entered by user.
     *
     * @return DOCUMENT ME!
     */
    public String getHost() {
        return hostName;
    }

    /**
     * Returns port number entered by user.
     *
     * @return DOCUMENT ME!
     */
    public int getPort() {
        return portNumber;
    }

    /**
     * Returns the close operation code. Codes are:
     * 
     * <ul>
     * <li>
     * 0 - closed or cancelled.
     * </li>
     * <li>
     * 1 - successful.
     * </li>
     * </ul>
     * 
     *
     * @return the operation code.
     */
    public int getResult() {
        return result;
    }

    public boolean isEncryptionUsed() {
        return useEncryption;
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (isVisible() && (e.getSource() == optionPane) &&
                (JOptionPane.VALUE_PROPERTY.equals(prop) ||
                JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
            Object value = optionPane.getValue();

            if (value == JOptionPane.UNINITIALIZED_VALUE) {
                // ignore reset
                return;
            }

            // Reset the JOptionPane's value.  If you don't do this,
            // then if the user presses the same button next time, no
            // property change event will be fired.
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

            if (okString.equals(value)) {
                System.err.println("Done with host: " + hostField.getText());

                // we're done; clear and dismiss the dialog
                hostName = hostField.getText();

                Number port = (Number) portField.getValue();
                portNumber = port.intValue();
                useEncryption = useEncryptionField.isSelected();
                result = 1;
                clearAndHide();
            } else {
                System.err.println("Closed or canceled");

                // user closed dialog or clicked cancel
                result = 0;
                clearAndHide();
            }
        }
    }
}
