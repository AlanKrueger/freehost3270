package com.ino.freehost.client;
/**
 * This class represents a single 'character' in the client's
 * data buffer/screen.  Field attributes (Highlighting, Color, etc.)
 * should be manipulated using the RW3270Field object, which can 
 * obtained from any RW3270Char object by calling the <code>getField()</code>
 * method.
 *
 * An array of RW3270Char objects representing the current 3270 screen can be
 * obtained by calling the <code>RW3270.getDataBuffer()</code> method.
 * @see com.ino.freehost.client.RW3270Field
 * @see com.ino.freehost.client.RW3270
 */
public class RW3270Char
{
   /**
    * Constructor called by RW3270.  End-programmers
    * have no reason to create RW3270Objects, as they
    * are managed completely by the data stream and session
    * instantiation.
    * @param position The position of this character in the buffer
    */
   protected RW3270Char(int position)
   {
      this.position = position;
      character = 0;
      background = (short)RW3270Field.DEFAULT_BGCOLOR;
      foreground = (short)RW3270Field.DEFAULT_FGCOLOR;
   }
   /**
    * Creates a pointer to the RW3270Field object that 'contains'
    * this character.
    * @param the field in which this character is contained
    */
   protected void setField(RW3270Field field)
   {
      this.field = field;
   }
   /**
    * Sets the actual ASCII char stored in this object.
    *
    * @param the character stored in this buffer location
    */
   protected void setChar(char c)
   {
      character = c;
   }
   /**
    * @return the field in which this character is contained
    */
   public RW3270Field getField()
   {
      return field;
   }
   /** 
    * @return the position of this character relative to the data buffer (screen)
    */
   public int getPosition()
   {
      return position;
   }
   /**
    * @return the representation of this character
    */
   public String toString()
   {
      char c = (character == 0) ? ' ': character;
      return new Character(c).toString();
   }
   /**
    * @return the character represented by this object.  <B>NOTE:</B>This method
    * returns the contents of hidden fields. Do not use for display of characters,
    * only for testing contents.  For displaying the character, use <code>getDisplayChar()</code>
    *
    */
   public char getChar()
   {
      return character;
   }
   /**
    * This method returns the actual Screen representation of an object
    * If the character is hidden or null, this method returns ' ';
    * Use this method if you plan on displaying this character to the
    * user.
    * @return the 'display' character represented by this object
    */
   public char getDisplayChar()
   {
      
      if(getField() != null && getField().isHidden())
      {
         return ' ';
      }
      if(character == 0)
         return ' ';
      return character;
   }
   /**
    * Clears the this location in the character buffer
    */
   protected void clear()
   {
      character = 0;
      //field = null;
      isStartField = false;
      highlighting = 0xF0;
      background = (short)RW3270Field.DEFAULT_BGCOLOR;
      foreground = (short)RW3270Field.DEFAULT_FGCOLOR;
      attribute = 0x00;
      outlining = 0x00;
   }
   /**
    * Sets this position in the data buffer as a start field
    */
   protected void setStartField()
   {
      isStartField = true;
   }
   /**
    * @return True if this character marks the beginning of a 3270 Field.
    */
   public boolean isStartField()
   {
      return isStartField;
   }
   /**
    * Stores the short that represents the FA
    */
   protected void setFieldAttribute(short in)
   {
      attribute = in;
      isProtected = false;
      isBold = false;
      isHidden = false;
      isModified = false;
      isNumeric = false;
      if((attribute & 0x08) != 0 && (attribute & 0x04) != 0)//bit 4 & 5 are on
         isHidden = true;
      else if((attribute & 0x08) != 0)//bit 4 is on
         isBold = true;
      if((attribute & 0x20) != 0)//bit 2 is on
         isProtected = true;
      if((attribute & 0x10) != 0)//bit 3 is on = numeric
      	isNumeric = true;
      if((attribute & 0x01) != 0)//bit 7 is on = modified
         isModified = true;
   }
   /**
    * @return the byte representation of this Field Attribute (assuming it's
    * a StartField.
    */
   public short getFieldAttribute()
   {
      return attribute;
   }
   /** 
    * Sets the highlighting attributes for this character/startField (assuming it's
    * a StartField.)
    */
   protected void setHighlighting(short in)
   {
      highlighting = in;
   }
   /**
    * Returns the highlighting attributes for this character/startField (assuming it's
    * a StartField.)
    */
   protected short getHighlighting()
   {
      return highlighting;
   }
   /**
    * Sets the outlining attributes for this character/startField (assuming it's
    * a StartField.)
    */
   protected void setOutlining(short in)
   {
      if(in == 0)
         outlining = OL_NONE;
      else if((in & 0x01) != 0)//00000001
         outlining = OL_UNDER;
      else if((in & 0x02) != 0)//00000010
         outlining = OL_RIGHT;
      else if((in & 0x04) != 0)//00000100
         outlining = OL_OVER;
      else if((in & 0x08) != 0)//00001000
         outlining = OL_LEFT;
      else if((in & 0x03) != 0)//00000011
         outlining = OL_UNDER_RIGHT;
      else if((in & 0x05) != 0)//00000101
         outlining = OL_UNDER_OVER;
      else if((in & 0x09) != 0)//00001001
         outlining = OL_UNDER_LEFT;
      else if((in & 0x06) != 0)//00000110
         outlining = OL_RIGHT_OVER;
      else if((in & 0x0A) != 0)//00001010
         outlining = OL_RIGHT_LEFT;
      else if((in & 0x0C) != 0)//00001100
         outlining = OL_OVER_LEFT;
      else if((in & 0x07) != 0)//00000111
         outlining = OL_OVER_RIGHT_UNDER;
      else if((in & 0x0B) != 0)//00001011
         outlining = OL_UNDER_RIGHT_LEFT;
      else if((in & 0x0D) != 0)//00001101
         outlining = OL_OVER_LEFT_UNDER;
      else if((in & 0x0E) != 0)//00001110
         outlining = OL_OVER_RIGHT_LEFT;
      else if((in & 0x0F) != 0)//00001111
         outlining = OL_RECTANGLE;
   }
   /**
    * @return int corresponding to Outlining constants outlined below.
    */
   public int getOutlining()
   {
      return outlining;
   }
   /**
    * Sets the foreground (font) color for this character/field
    */
   protected void setForeground(short in)
   {
      foreground = in;
   }
   /**
    * Returns the foreground (font) color of this character/field
    */
   public int getForeground()
   {
      return foreground;
   }
   /**
    * Sets the background color of this character field.
    */
   protected void setBackground(short in)
   {
      background = in;
   }
   /**
    * Returns the background color of this character/field.
    */
   public int getBackground()
   {
      return background;
   }
   /**
    * Sets the validation attributes for this character/field.
    */
   protected void setValidation(short in)
   {
      //TODO:  Probably won't implement this... it's a bitch.
   }
   /** 
    * @return True if this character/field is protected. (Accessed through
    * RW3270Field)
    */
   public boolean isProtected()
   {
      return isProtected;
   }
   public void isProtected(boolean b)
   {
   		isProtected = b;
   }
   /**
    * @return True if this field should be rendered in Bold type
    */
   public boolean isBold()
   {
      return isBold;
   }
   /**
    * @return True if this field should be hidden
    */
   public boolean isHidden()
   {
      return isHidden;
   }
   /**
    * Sets the MDT for this field (assuming it's a SF character)
    */
   protected void isModified(boolean b)
   {
      if(b)
         attribute |= 0x01;//flip the modified bit on.
      else
         attribute ^= 0x01;//flip the modified bit to off.
      isModified = b;
   }
   protected boolean isModified()
   {
      return isModified;
   }
   public boolean isNumeric()
   {
   	 return isNumeric;
   }
   private RW3270Field              field;
   private int                      position;
   private char                     character;
   private boolean                  isStartField;
   private boolean                  isProtected;
   private boolean                  isBold;
   private boolean                  isHidden;
   private boolean                  isModified;
   private boolean									isNumeric;
   private short                    attribute;
   private short                    foreground, background, highlighting, outlining;
   
   //Highlighting constants p 4.4.6.3
   public final static short               HL_DEFAULT           = 0x00;
   public final static short               HL_NORMAL            = 0xF0;
   public final static short               HL_BLINK             = 0xF1;
   public final static short               HL_REVERSE           = 0xF2;
   public final static short               HL_UNDERSCORE        = 0xF4;
   //color constants p 4.4.6.4
   public final static short               BGCOLOR_DEFAULT      = 0xF0;
   public final static short               FGCOLOR_DEFAULT      = 0xF7;
   public final static short               BLUE                 = 0xF1;
   public final static short               RED                  = 0xF2;
   public final static short               PINK                 = 0xF3;
   public final static short               GREEN                = 0xF4;
   public final static short               TURQUOISE            = 0xF5;
   public final static short               YELLOW               = 0xF6;
   public final static short               BLACK                = 0xF8;
   public final static short               DEEP_BLUE            = 0xF9;
   public final static short               ORANGE               = 0xFA;
   public final static short               PURPLE               = 0xFB;
   public final static short               PALE_GREEN           = 0xFC;
   public final static short               PALE_TURQUOISE       = 0xFD;
   public final static short               GREY                 = 0xFE;
   public final static short               WHITE                = 0xFF;
   //4.4.6.6 Field Outlining (Internal use)
   public final static short               OL_NONE              = 0;
   public final static short               OL_UNDER             = 1;
   public final static short               OL_RIGHT             = 2;
   public final static short               OL_OVER              = 3;
   public final static short               OL_LEFT              = 4;
   public final static short               OL_UNDER_RIGHT       = 5;
   public final static short               OL_UNDER_OVER        = 6;
   public final static short               OL_UNDER_LEFT        = 7;
   public final static short               OL_RIGHT_OVER        = 8;
   public final static short               OL_RIGHT_LEFT        = 9;
   public final static short               OL_OVER_LEFT         = 10;
   public final static short               OL_OVER_RIGHT_UNDER  = 11;
   public final static short               OL_UNDER_RIGHT_LEFT  = 12;
   public final static short               OL_OVER_LEFT_UNDER   = 13;
   public final static short               OL_OVER_RIGHT_LEFT   = 14;
   public final static short               OL_RECTANGLE         = 15;
}
