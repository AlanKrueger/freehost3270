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

/**
 * Represents a single 'character' in the client's data buffer/screen.
 * 
 * <p>
 * Field attributes (Highlighting, Color, etc.) should be manipulated using
 * the RW3270Field object, which can obtained from any RW3270Char object by
 * calling the <code>getField()</code> method. An array of RW3270Char objects
 * representing the current 3270 screen can be obtained by calling the
 * <code>RW3270.getDataBuffer()</code> method.
 * </p>
 *
 * @see net.sf.freehost3270.client.RW3270Field
 * @see net.sf.freehost3270.client.RW3270
 */
public class RW3270Char {
    //Highlighting constants p 4.4.6.3
    public static final short HL_DEFAULT = 0x00;
    public static final short HL_NORMAL = 0xF0;
    public static final short HL_BLINK = 0xF1;
    public static final short HL_REVERSE = 0xF2;
    public static final short HL_UNDERSCORE = 0xF4;

    //color constants p 4.4.6.4
    // TODO: the same constants are defined in the RW3270Field
    public static final short BGCOLOR_DEFAULT = 0xF0;
    public static final short FGCOLOR_DEFAULT = 0xF7;
    public static final short BLUE = 0xF1;
    public static final short RED = 0xF2;
    public static final short PINK = 0xF3;
    public static final short GREEN = 0xF4;
    public static final short TURQUOISE = 0xF5;
    public static final short YELLOW = 0xF6;
    public static final short BLACK = 0xF8;
    public static final short DEEP_BLUE = 0xF9;
    public static final short ORANGE = 0xFA;
    public static final short PURPLE = 0xFB;
    public static final short PALE_GREEN = 0xFC;
    public static final short PALE_TURQUOISE = 0xFD;
    public static final short GREY = 0xFE;
    public static final short WHITE = 0xFF;

    //4.4.6.6 Field Outlining (Internal use)
    public static final short OL_NONE = 0;
    public static final short OL_UNDER = 1;
    public static final short OL_RIGHT = 2;
    public static final short OL_OVER = 3;
    public static final short OL_LEFT = 4;
    public static final short OL_UNDER_RIGHT = 5;
    public static final short OL_UNDER_OVER = 6;
    public static final short OL_UNDER_LEFT = 7;
    public static final short OL_RIGHT_OVER = 8;
    public static final short OL_RIGHT_LEFT = 9;
    public static final short OL_OVER_LEFT = 10;
    public static final short OL_OVER_RIGHT_UNDER = 11;
    public static final short OL_UNDER_RIGHT_LEFT = 12;
    public static final short OL_OVER_LEFT_UNDER = 13;
    public static final short OL_OVER_RIGHT_LEFT = 14;
    public static final short OL_RECTANGLE = 15;
    private RW3270Field field;
    private boolean isBold;
    private boolean isHidden;
    private boolean isModified;
    private boolean isNumeric;
    private boolean isProtected;
    private boolean isStartField;
    private char character;
    private int position;
    private short attribute;
    private short background;
    private short foreground;
    private short highlighting;
    private short outlining;

    /**
     * Instanitates a new RW3270 character.
     * 
     * <p>
     * Normally is called by RW3270.  End-programmers have no reason to create
     * RW3270Objects, as they are managed completely by the data stream and
     * session instantiation.
     * </p>
     *
     * @param position The position of this character in the buffer
     */
    protected RW3270Char(int position) {
        this.position = position;
        character = 0;
        background = (short) RW3270Field.DEFAULT_BGCOLOR;
        foreground = (short) RW3270Field.DEFAULT_FGCOLOR;
    }

    /**
     * Returns the background color of this character/field.
     *
     * @return DOCUMENT ME!
     */
    public int getBackground() {
        return background;
    }

    /**
     * Returns the contents of hidden fields.
     * 
     * <p>
     * <b>NOTE:</b> Do not use for display of characters, only for testing
     * contents.  For displaying the character, use
     * <code>getDisplayChar()</code>
     * </p>
     *
     * @return the character represented by this object.
     */
    public char getChar() {
        return character;
    }

    /**
     * This method returns the actual Screen representation of an object If
     * the character is hidden or null, this method returns ' '; Use this
     * method if you plan on displaying this character to the user.
     *
     * @return the 'display' character represented by this object
     */
    public char getDisplayChar() {
        if (((getField() != null) && getField().isHidden()) ||
                (character == 0)) {
            return ' ';
        } else {
            return character;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return the field in which this character is contained
     */
    public RW3270Field getField() {
        return field;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the byte representation of this Field Attribute (assuming it's
     *         a StartField.
     */
    public short getFieldAttribute() {
        return attribute;
    }

    /**
     * Returns the foreground (font) color of this character/field
     *
     * @return DOCUMENT ME!
     */
    public int getForeground() {
        return foreground;
    }

    /**
     * DOCUMENT ME!
     *
     * @return int corresponding to Outlining constants outlined below.
     */
    public int getOutlining() {
        return outlining;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the position of this character relative to the data buffer
     *         (screen)
     */
    public int getPosition() {
        return position;
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this field should be rendered in Bold type
     */
    public boolean isBold() {
        return isBold;
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this field should be hidden
     */
    public boolean isHidden() {
        return isHidden;
    }

    public boolean isNumeric() {
        return isNumeric;
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this character/field is protected. (Accessed through
     *         RW3270Field)
     */
    public boolean isProtected() {
        return isProtected;
    }

    public void isProtected(boolean b) {
        isProtected = b;
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this character marks the beginning of a 3270 Field.
     */
    public boolean isStartField() {
        return isStartField;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the representation of this character
     */
    public String toString() {
        StringBuffer rep = new StringBuffer("(char ");
        rep.append(":char ").append((short) getChar()).append("\n");
        rep.append(":position ").append(getPosition()).append("\n");
        rep.append(":background ").append(getBackground()).append("\n");
        rep.append(":foreground ").append(getForeground()).append("\n");
        rep.append(":highlighting ").append(getHighlighting()).append("\n");
        rep.append(":outlining ").append(getOutlining()).append("\n");
        rep.append(":bold-p ").append((isBold() ? "#t" : "#f")).append("\n");
        rep.append(":hidden-p ").append((isHidden() ? "#t" : "#f")).append("\n");
        rep.append(":numeric-p ").append((isNumeric() ? "#t" : "#f"))
           .append("\n");
        rep.append(":protected-p ").append((isNumeric() ? "#t" : "#f"))
           .append("\n");
        rep.append(":start-field-p ").append(isStartField());
        rep.append(")");

        return rep.toString();

        //         char c = (character == 0) ? ' ' : character;
        //         return new Character(c).toString();
    }

    /**
     * Clears the this location in the character buffer
     */
    protected void clear() {
        character = 0;

        //field = null;
        isStartField = false;
        highlighting = 0xF0;
        background = (short) RW3270Field.DEFAULT_BGCOLOR;
        foreground = (short) RW3270Field.DEFAULT_FGCOLOR;
        attribute = 0x00;
        outlining = 0x00;
    }

    /**
     * Returns the highlighting attributes for this character/startField
     * (assuming it's a StartField.)
     *
     * @return DOCUMENT ME!
     */
    protected short getHighlighting() {
        return highlighting;
    }

    /**
     * Sets the MDT for this field (assuming it's a SF character)
     *
     * @param b DOCUMENT ME!
     */
    protected void isModified(boolean b) {
        if (b) {
            attribute |= 0x01; //flip the modified bit on.
        } else {
            attribute ^= 0x01; //flip the modified bit to off.
        }

        isModified = b;
    }

    protected boolean isModified() {
        return isModified;
    }

    /**
     * Sets the background color of this character field.
     *
     * @param in DOCUMENT ME!
     */
    protected void setBackground(short in) {
        background = in;
    }

    /**
     * Sets the actual ASCII char stored in this object.
     *
     * @param c character stored in this buffer location
     */
    protected void setChar(char c) {
        character = c;
    }

    /**
     * Creates a pointer to the RW3270Field object that 'contains' this
     * character.
     *
     * @param field the field in which this character is contained
     */
    protected void setField(RW3270Field field) {
        this.field = field;
    }

    /**
     * Stores the short that represents the FA
     *
     * @param in DOCUMENT ME!
     */
    protected void setFieldAttribute(short in) {
        attribute = in;
        isProtected = false;
        isBold = false;
        isHidden = false;
        isModified = false;
        isNumeric = false;

        if (((attribute & 0x08) != 0) && ((attribute & 0x04) != 0)) { //bit 4 & 5 are on
            isHidden = true;
        } else if ((attribute & 0x08) != 0) { //bit 4 is on
            isBold = true;
        }

        if ((attribute & 0x20) != 0) { //bit 2 is on
            isProtected = true;
        }

        if ((attribute & 0x10) != 0) { //bit 3 is on = numeric
            isNumeric = true;
        }

        if ((attribute & 0x01) != 0) { //bit 7 is on = modified
            isModified = true;
        }
    }

    /**
     * Sets the foreground (font) color for this character/field
     *
     * @param in DOCUMENT ME!
     */
    protected void setForeground(short in) {
        foreground = in;
    }

    /**
     * Sets the highlighting attributes for this character/startField
     * (assuming it's a StartField.)
     *
     * @param in DOCUMENT ME!
     */
    protected void setHighlighting(short in) {
        highlighting = in;
    }

    /**
     * Sets the outlining attributes for this character/startField (assuming
     * it's a StartField.)
     *
     * @param in DOCUMENT ME!
     */
    protected void setOutlining(short in) {
        if (in == 0) {
            outlining = OL_NONE;
        } else if ((in & 0x01) != 0) { //00000001
            outlining = OL_UNDER;
        } else if ((in & 0x02) != 0) { //00000010
            outlining = OL_RIGHT;
        } else if ((in & 0x04) != 0) { //00000100
            outlining = OL_OVER;
        } else if ((in & 0x08) != 0) { //00001000
            outlining = OL_LEFT;
        } else if ((in & 0x03) != 0) { //00000011
            outlining = OL_UNDER_RIGHT;
        } else if ((in & 0x05) != 0) { //00000101
            outlining = OL_UNDER_OVER;
        } else if ((in & 0x09) != 0) { //00001001
            outlining = OL_UNDER_LEFT;
        } else if ((in & 0x06) != 0) { //00000110
            outlining = OL_RIGHT_OVER;
        } else if ((in & 0x0A) != 0) { //00001010
            outlining = OL_RIGHT_LEFT;
        } else if ((in & 0x0C) != 0) { //00001100
            outlining = OL_OVER_LEFT;
        } else if ((in & 0x07) != 0) { //00000111
            outlining = OL_OVER_RIGHT_UNDER;
        } else if ((in & 0x0B) != 0) { //00001011
            outlining = OL_UNDER_RIGHT_LEFT;
        } else if ((in & 0x0D) != 0) { //00001101
            outlining = OL_OVER_LEFT_UNDER;
        } else if ((in & 0x0E) != 0) { //00001110
            outlining = OL_OVER_RIGHT_LEFT;
        } else if ((in & 0x0F) != 0) { //00001111
            outlining = OL_RECTANGLE;
        }
    }

    /**
     * Sets this position in the data buffer as a start field
     */
    protected void setStartField() {
        isStartField = true;
    }

    /**
     * Sets the validation attributes for this character/field.
     *
     * @param in DOCUMENT ME!
     */
    protected void setValidation(short in) {
        //TODO:  Probably won't implement this... it's a bitch.
    }
}
