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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class EditHostFrame extends JFrame implements ActionListener {
    JButton cancel;
    JButton ok;
    JTextField host;
    int response;

    public EditHostFrame() {
        super("Host Connect");
        response = -1;
        setLayout(new BorderLayout());
        add("Center", host = new JTextField());

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout());
        p.add(ok = new JButton("OK"));
        p.add(cancel = new JButton("Cancel"));
        add("South", p);
        add("North", new JLabel("Enter a host to connect to:"));
        pack();

        Dimension screen_size;
        Dimension dlg;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        dlg = this.getSize();
        setLocation((screen_size.width - dlg.width) / 2,
            (screen_size.height - dlg.height) / 2);
        setVisible(true);
        requestFocus();
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() instanceof JButton) {
            if (evt.getActionCommand().equals("OK")) {
                response = 0;
                dispose();
            } else if (evt.getActionCommand().equals("Cancel")) {
                response = 1;
                dispose();
            }
        }
    }
}
