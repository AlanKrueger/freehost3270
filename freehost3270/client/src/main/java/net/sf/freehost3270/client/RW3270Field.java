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
package net.sf.freehost3270.client;

import java.io.IOException;


/**
 * Represents a Tn3270 'Field' instance.
 *
 * You can obtain an enumeration of RW3270Field objects by calling
 * RW3270.getFields().  You can also get the 'current' field.  (The
 * field that the current cursor position is located in) by calling
 * RW3270.getField().  Finally, you can get the RW3270Field object at
 * any given cursor position by calling <code>RW3270.getField(int
 * cursorPosition)</code>.
 *
 * <p>This class is useful for working with 3270 fields.  For example,
 * if you wanted to enter data from a GUI textField: <code></code>
 *
 * @since 0.1
 */
public class RW3270Field {
    public final static int DEFAULT_BGCOLOR = 0xF0;
    public final static int BLUE = 0xF1;
    public final static int RED = 0xF2;
    public final static int PINK = 0xF3;
    public final static int GREEN = 0xF4;
    public final static int TURQUOISE = 0xF5;
    public final static int YELLOW = 0xF6;
    public final static int DEFAULT_FGCOLOR = 0xF7;
    public final static int BLACK = 0xF8;
    public final static int DEEP_BLUE = 0xF9;
    public final static int ORANGE = 0xFA;
    public final static int PURPLE = 0xFB;
    public final static int PALE_GREEN = 0xFC;
    public final static int PALE_TURQUOISE = 0xFD;
    public final static int GREY = 0xFE;
    public final static int WHITE = 0xFF;
    private int begin;
    private int end;
    private RW3270Char fa;
    private RW3270 rw;

    /**
     * Not seen by end-programmers.
     *
     * The data stream handles the creation and destruction of field
     * objects.
     */
    protected RW3270Field(RW3270Char fa, RW3270 rw) {
        //pointer to the RW3270Char object that contains
        //the properties for this field (Field Attribute = fa).
        this.fa = fa;

        //pointer to the RWObject, so we can access the data
        //buffer when necessary
        this.rw = rw;
        begin = fa.getPosition();
    }

    /**
     * Checks the field to see if the current field is an 'input'
     * field (unprotected)
     *
     * @return <code>true</code> if the field is protected,
     * <code>false</code> otherwise.
     */
    public boolean isProtected() {
        return fa.isProtected();
    }

    protected void isProtected(boolean b) {
        fa.isProtected(b);
    }

    protected void setBegin(int i) {
        begin = i;
    }

    /**
     * Returns the data buffer address of the first character in this
     * field.
     *
     * @return the data buffer address of the FA (first character) in this
     * field.
     */
    public int getBegin() {
        return begin;
    }

    /**
     * Sets the data buffer address of the last character in this field.
     * 
     * (Not visible to end-programmers... handled by the data stream)
     */
    protected void setEnd(int i) {
        end = i;
    }

    /**
     * Returns the data buffer address of the last character in this field.
     *
     * @return the data buffer address of the last character in this field
     */
    public int getEnd() {
        return end;
    }

    /**
     * @return <code>true</code> if this field has been modified by
     * the operator
     */
    public boolean isModified() {
        return fa.isModified();
    }

    /**
     * @deprecated since 0.2 preferred method is setModified
     */
    public void isModified(boolean b) throws IsProtectedException {
      setModified(b);
    }

    /**
     * Sets the MDT for this field.
     * 
     * <p>If you change the data via the setData() method this method
     * is called for you automatically.  Otherwise, be sure to call
     * this method whenever you change the data in an unprotected
     * (input) field.
     * 
     * @param b <code>true</code> if the field has been modified.
     */
  public void setModified(boolean b) throws IsProtectedException {
         if (fa.isProtected()) {
            throw new IsProtectedException();
        }

        fa.isModified(b);
   
  }

    /**
     * Gets an array of RW3270Char objects representing the contents of this field.
     *
     * <p>
     * This method is only useful if you need to operate on the individual RW3270Char
     * objects.  For display of data buffer (screen), use <code>getDisplayChars()</code>
     * instead.
     * 
     * @return  array of {@link RW3270Char} objects.
     * @see RW3270Char
     */
    public RW3270Char[] getChars() {
        int adjEnd = end;

        //in case the field wraps around, adjust the end to be the data buffer
        //size + n
        if (end < begin) {
            adjEnd += rw.getDataBuffer().length;
        }

        RW3270Char[] ret = new RW3270Char[(adjEnd - begin) + 1];

        //
        int c = begin;

        for (int i = 0; i < ret.length; i++, c++) {
            if (c == rw.getDataBuffer().length) {
                c = 0;
            }

            ret[i] = rw.getChar(c);
        }

        return ret;
    }

    /**
     * Gets an array of characters containing the display representation of this
     * field.
     * <p>
     * Specifically, hidden and null characters are represented by an ' '
     * 
     * @return array of characters.
     */
    public char[] getDisplayChars() {
        int adjEnd = end;
        RW3270Char[] display = rw.getDataBuffer();

        //in case the field wraps around, adjust the end to be the data buffer
        //size + n
        if (end < begin) {
            adjEnd += display.length;
        }

        char[] ret = new char[(adjEnd - begin) + 1];
        int c = begin;

        for (int i = 0; i < ret.length; i++, c++) {
            if (c == display.length) {
                c = 0;
            }

            ret[i] = display[c].getDisplayChar();
        }

        return ret;
    }

    /**
     * If the field is an input field (unprotected), this method will set the
     * field to the specified String.
     *
     * If the string is longer than the field, this
     * method will throw an IOException.
     * 
     * @param s The string to insert into this field
     * @exception IOException thrown if the string is longer than the field
     * @exception IsProtectedException thrown if the current field is protected
     */
    public void setData(String s) throws IOException, IsProtectedException {
        if (isProtected()) {
            throw new IsProtectedException();
        }

        char[] b = s.toCharArray();
        int size = (end > begin) ? (end - begin)
                                 : ((rw.getRows() * rw.getCols()) - begin +
            end);

        if (b.length > size) {
            throw new IOException();
        }

        try {
            setModified(true);
        } catch (Exception e) {
        }

        int offset = begin + 1;
        RW3270Char[] chars = rw.getDataBuffer();

        for (int i = 0; i < b.length; i++, offset++) {
            rw.getChar(offset).setChar(b[i]);
        }
    }

    /**
     * This method returns the size, in characters of the current field.
     *
     * <p> Be aware that this includes the Field Attribute for the
     * field, as well as the characters
     * 
     * @return The size (in characters) of this Field object
     */
    public int size() {
        if (end > begin) {
            return end - begin;
        }

        return (rw.getDataBuffer().length - begin) + end;
    }

    /**
     * @return the foreground (text) color of this field. (Corresponding to the
     * color constants defined in the RW3270Char class)
     */
    public int getForegroundColor() {
        return fa.getForeground();
    }

    /**
     * @return the background color of this field.  (Corresponding to the
     * color constants defined in the RW3270Char class)
     */
    public int getBackgroundColor() {
        return fa.getBackground();
    }

    /**
     * @return the highlighting scheme for this field.  (Corresponding to the
     * highlighting constants defined in the RW3270Char class)
     */
    public int getHighlighting() {
        return fa.getHighlighting();
    }

    /**
     * @return <code>true</code> if the field is hidden (i.e. password
     * field), <code>dalse</code> otherwise.
     */
    public boolean isHidden() {
        return fa.isHidden();
    }

    /**
     * @return <code>true</code> if the field is bold,
     * <code>false</code> if it is not.
     */
    public boolean isBold() {
        return fa.isBold();
    }

    /**
     * @return True if the field only accepts numeric data, False if it will
     * accept any alphanumeric input.  Client implementations should test
     * input if this method returns true and reject it if it is not numeric/
     */
    public boolean isNumeric() {
        return fa.isNumeric();
    }
    public boolean isHighIntensity() {
    	return fa.isHighIntensity();
    }
    public int getFieldAttribute() {
        return fa.getFieldAttribute();
    }

  /**
   * Returns a string representation of the field.
   *
   * @since 0.2
   */
  public String toString() {
    StringBuffer rep = new StringBuffer("(field");
    rep.append(":begin ").append(getBegin()).append("\n");
    rep.append(":highlight ").append(getHighlighting()).append("\n");
    rep.append(":hidden-p ").append(isHidden()).append("\n");
    rep.append(":bold-p ").append(isBold()).append("\n");
    rep.append(":protected-p ").append(isProtected()).append("\n");
    rep.append(":numeric-p ").append(isNumeric());
    // autoskip, nondisplay, intensified display are missing
    // detectable as well
    rep.append(")");
    return rep.toString();
  }
}
