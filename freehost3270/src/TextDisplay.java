/*
 * Freehost3270 - A web deployment system for TN3270 clients
 *
 * Copyright (C) 1998,2001 Art Gillespie
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
 *
 * The Author can be contacted at agillesp@i-no.com or
 * 185 Captain Whitney Road (Becket)
 * Chester, MA  01011
 */

import java.awt.*;
import java.util.*;

public class TextDisplay extends Canvas
{

    public TextDisplay(String s)
    {
        this(s, new Font("Dialog", 1, 12), 0, 0);
    }

    public TextDisplay(String s, Font font)
    {
        this(s, font, 0, 0);
    }

    public TextDisplay(String s, Font font, int i)
    {
        this(s, font, 0, 0);
    }

    public TextDisplay(String s, Font font, int i, int j)
    {
        linear = false;
        inited = false;
        text = s;
        if(font != null)
            setFont(font);
        offset = i;
        voffset = j;
    }

    public synchronized void setText(String s, int i)
    {
        text = s;
        offset = i;
        inited = false;
        repaint();
    }

    public synchronized void setText(String s)
    {
        text = s;
        inited = false;
        repaint();
    }

    private final void init()
    {
        FontMetrics fontmetrics = getFontMetrics(getFont());
        startht = fontmetrics.getAscent() + voffset;
        ht = fontmetrics.getHeight();
        desc = fontmetrics.getDescent();
        Vector vector = new Vector();
        for(StringTokenizer stringtokenizer = new StringTokenizer(text, " \r\t"); stringtokenizer.hasMoreElements(); vector.addElement(stringtokenizer.nextElement()));
        words = new String[vector.size()];
        wordLengths = new int[vector.size()];
        int i = 0;
        for(Enumeration enumeration = vector.elements(); enumeration.hasMoreElements();)
        {
            String s = (String)enumeration.nextElement();
            if(s.endsWith("\n"))
            {
                words[i] = s.replace('\n', ' ');
                wordLengths[i] = -fontmetrics.stringWidth(s);
            }
            else
            {
                s += " ";
                words[i] = s;
                wordLengths[i] = fontmetrics.stringWidth(s);
            }
            i++;
        }

        inited = true;
    }

    public void paint(Graphics g)
    {
        Dimension dimension = size();
        if(!inited)
            init();
        int i = offset;
        int j = startht;
        for(int k = 0; k < words.length; k++)
        {
            int l = wordLengths[k] >= 0 ? wordLengths[k] : -wordLengths[k];
            if(dimension.width < i + l)
            {
                i = offset;
                j += ht;
                if(j > dimension.height)
                    return;
            }
            g.drawString(words[k], i, j);
            if(wordLengths[k] <= 0)
            {
                i = offset;
                j += ht;
            }
            else
            {
                i += wordLengths[k];
            }
        }

    }

    public Dimension minumumSize()
    {
        return preferredSize();
    }

    public void makeLinear()
    {
        linear = true;
    }

    public Dimension preferredSize()
    {
        Dimension dimension = size();
        int i = dimension.width;
        if(!linear && i == 0)
            i = 300;
        if(!inited)
            init();
        int j = offset;
        int k = startht;
        for(int l = 0; l < words.length; l++)
        {
            int i1 = wordLengths[l] >= 0 ? wordLengths[l] : -wordLengths[l];
            if(!linear && i < j + i1)
            {
                j = offset;
                k += ht;
                if(i1 > i)
                    i = i1;
            }
            if(wordLengths[l] <= 0)
            {
                j = offset;
                k += ht;
            }
            else
            {
                j += wordLengths[l];
            }
        }

        if(linear)
            i = j;
        dimension = new Dimension(i, k + desc);
        return dimension;
    }

    String words[];
    int wordLengths[];
    private int offset;
    private boolean linear;
    int ht;
    int startht;
    int voffset;
    int desc;
    boolean inited;
    String text;
}
