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

/*
 * 5/12/98 - Added the following methods:
 * 	public boolean contains(String search) - lets developers
 *			quickly test the current screen to see if the search
 *    	string is contained in it.
 *		public boolean waitFor(String search, int timeout) - lets developers
 *			block the calling thread until either the search string is found
 *			in the current screen or the specified timeout period elapses.
 *			This also required modifying the class to implement the Runnable
 *			interface, and adding the public void run() method to serve as
 *			a timer for the timeout.
 *		public void connect(String host, int port) - implemented to facilitate
 *			the deployment of the 3270 Servlet Developer's toolkit.  This enables
 *			implementations to connect directly to a TN3270 host, bypassing the
 *			SessionServer.  When packaging the 3270 Servlet Developer's toolkit, it
 *			may be wise to 'hide' the connect(String host, int port, String sessionserver,
 *			int sessionserverport, boolean encryption) method from Servlet developers
 *			and likewise hide the public void connect(String host, int port) from 
 *			users of the RightHost 3270 product (?????)
 */
package com.ino.freehost.client;

import java.util.Vector;
import java.util.Enumeration;
/**
 * This class represents a 3270 session to client implementations, and is the
 * main API for creating 3270 applications.
 */                        
public class RW3270
{
   /**
    * Default constructor.  Assumes Tn3270 model 2 (80 x 24)
    * @param client RWTnAction interface used for communicating with
    * the client implementation
    */
   public RW3270(RWTnAction client)
   {
      this(2, client);
   }
   /**
    * Constructor.  Sets the model type of the new 3270 object:
    * @param client RWTnAction interface used for communicating with
    * the client implementation
    * @param modelNumber the 3270 model number.    	
    * <UL>
    * <LI><code>2: 24x80</code>
    * <LI><code>3: 32x80</code>
    * <LI><code>4: 43x80</code>
    * <LI><code>5: 27x132</code>
    *</UL>
    */
   public RW3270(int modelNumber, RWTnAction client)
   {
      this.client = client;
      tnModel = (short)modelNumber;
      switch(tnModel)
      {
      case 2:
         rows = 24;
         cols = 80;
         break;
      case 3:
         rows = 32;
         cols = 80;
         break;
      case 4:
         rows = 43;
         cols = 80;
         break;
      case 5:
         rows = 27;
         cols = 132;
      }
      //create the character buffer
      chars = new RW3270Char[rows*cols];
      //create display buffer
      display = new char[rows*cols];
      //create RW3720Char instances for each position in the databuffer (chars array)
      for(int i = 0; i < chars.length; i++)
         chars[i] = new RW3270Char(i);
      //create the field vector
      fields = new Vector();
      //create the 3270 Stream Parser Object.  Pass it
      //this instance so it can call back changes to the
      //field and character buffer as necessary.
      tnParser = new RWTn3270StreamParser(this, client);
      //create the TELNET object
      tn = new RWTelnet(tnParser, tnModel);
      cursorPosition = 0;
      waitObject = new WaitObject();
   }
   protected RWTelnet getTelnet()
   {
      return tn;
   }
   /**
    * Gets the current screen's display characters.  This method is useful for
    * getting the characters necessary for display in the client implementation.
    * It automatically suppresses hidden (password, etc.) fields and null characters.
    * @return The current 3270 screen as an array of characters.
    */
   public char[] getDisplay()
   {
      return display;
   }
   /**
    * This method attempts to connect this 3270 object to the specified
    * SessionServer and 3270 Host.
    * @param host - the hostname or ip address of the SessionServer
    * @param port - the SessionServer's port
    * @param host3270 - the hostname or ip address of the 3270 host. (If using SessionServer)
    * @param port3270 - the port of the 3270 host
    * @param encryption - True if encryption should be used, false otherwise.  <B>IMPORTANT:</B>
    * this setting must match the SessionServer's encryption setting in order to communicate successfully.
    * the current encryption setting of the SessionServer can be obtained by calling <code><i>&gt;SessionServer&lt;</i>.
    * getEncryption()</code>
    */
   public void connect(String host, int port, String host3270, int port3270, boolean encryption)
   {
      tn.setEncryption(encryption);
      tn.connect(host, port, host3270, port3270);
   }
	/**
	 * This method implements connections directly to a host, bypassing the SessionServer
	 * NOTE: connections made in this fashion are not encrypted
	 * @param host The hostname of the TN3270 host to connect to
	 * @param port The port on which to connect to the TN3270 host
	 */
	public void connect(String host, int port)
	{
		tn.setEncryption(false);
		tn.connect(host, port);
	}
   /**
    * Disconnects this RW3270 object from the current Session
    */
   public void disconnect()
   {
      tn.disconnect();
   }
   /**
		* This method returns an array of RW3270Char objects representing the
		* data buffer.   
    * @return the RW3270Char array
    */
   public RW3270Char[] getDataBuffer()
   {
      return chars;
   }
   /**
    * Returns a vector of fields representing the current 3270 screen
    * @return the field Vector
    */
   public Vector getFields()
   {
      return fields;
   }
   /**
    * Blocks the currently executing thread until new data arrives from the host
    * This method is useful when using stateless client implementations (i.e. HTTP)
    * or when you're scripting through several 3270 screens.  For example:
    * <code><BR>
    * ...<BR>
    * //Set the value of a field and press enter<BR>
    * RW3270Field.setData("My Data");<BR>
		* RW3270.enter();<BR>
		* //Wait for the new screen<BR>
		* RW3270.waitForNewData();<BR>
		* myClient.paintScreen()....<BR>
		* </code><BR>
		* <BR>
		* <B>IMPORTANT:</B>  This method only blocks until the 3270 engine
		* has received a response from the host and processed it.  In the
		* case where the host has sent an EraseAllUnprotected or other Non-Data
		* type command, this method will return normally, even though the
		* DataBuffer (screen) may be empty.  Client implementations need 
		* to handle any host-specific anomalies such as screen clears, etc.
    */
   public void waitForNewData()
   {
   		parentThread = Thread.currentThread();
         try
         {
            parentThread.wait();
         }
         catch(InterruptedException e)
         {

         }
	}
   protected void resumeParentThread()
   {
   		if(parentThread != null)
   			parentThread.notify();
			if(waitObject != null && contains(waitForSearch))
			{
				waitForReturn = true;
            synchronized(waitObject)
            {
               waitObject.notify();
            }
			}

   }		
   protected void unlockKeyboard()
   {
      keyboardLocked = false;
   }
   protected void lockKeyboard()
   {
      keyboardLocked = true;
   }
   /**
    * This method is designed to let implementations check to see if the 'keyboard'
    * is currently locked.  
    * @return True if the screen is not currently accepting input, False if it is.
    */
   public boolean isKeyboardLocked()
   {
      return keyboardLocked;
   }
   /**
    * This method returns the first character position of the next field from the
    * current cursor position.
    * @return The next unprotected field's address, starting from the current cursor
    * position.
    */
   public short getNextUnprotectedField(int pos)
   {
      Enumeration e = fields.elements();
      while(e.hasMoreElements())
      {
         RW3270Field f = (RW3270Field)e.nextElement();
         if(!f.isProtected() && f.getBegin() >= pos)
            return (short)(f.getBegin() + 1);
      }
      //no next, get first unprotected (wrap)
      e = fields.elements();
      while(e.hasMoreElements())
      {
         RW3270Field f = (RW3270Field)e.nextElement();
         if(!f.isProtected())
            return (short)(f.getBegin() + 1);
      }
      return (short)pos;
   }
   /**
    * This method returns the first character position of the previous field from
    * the current cursor position
    * @return The previous unprotected field's address, starting from the current cursor
    * position
    */
   protected short getPreviousUnprotectedField(int pos)
   {
      RW3270Field currField = getChar(pos).getField();
      Enumeration e = fields.elements();
      RW3270Field thisField = (RW3270Field)e.nextElement();
      if(thisField == currField)
         return (short)thisField.getBegin();
      while(e.hasMoreElements())
      {
         RW3270Field nextField = (RW3270Field)e.nextElement();
         //if this is an unprotected field
         if(!thisField.isProtected() && getNextUnprotectedField(thisField.getEnd()) == (currField.getBegin() + 1))
            return (short)(thisField.getBegin() + 1);
         thisField = nextField;
      }
      //no previous, so stay at current
      return (short)pos;
   }
   /**
    * This method sets the cursor position
    * @param newCursorPos The new cursor position
    */
   public void setCursorPosition(short newCursorPos)
   {
      cursorPosition = newCursorPos;
   }
   /**
    * @return The current cursor position
    */
   public int getCursorPosition()
   {
      return cursorPosition;
   }
   /**
    * @return The number of rows in the current screen (Set by TN3270 model number)
    */
   public int getRows()
   {
      return rows;
   }
   /**
    * @return The number of cols in the current screen (Set by TN3270 model number)
    */
   public int getCols()
   {
      return cols;
   }
   /**
    * @return The current AID, or 0x00 for none.
    */
   public short getAID()
   {
      return aid;
   }
   /**
    * This method is useful for retrieving the RW3270 character object at a particular
    * position on the screen
    * @return RW3270Char object
    * @param i Screen position of the requested RW3270Char object
    */
   public RW3270Char getChar(int i)
   {
      return chars[i];
   }
   /**
    * This method returns the RW3270Char object at the current cursor position
    * @return RW3270Char Object.
    */
   public RW3270Char getChar()
   {
   		return chars[cursorPosition];
   }
   /**
    * This method returns the RW3270Field object that the current cursor position
    * is in.
    * @return RW3270Field object.
    */
   public RW3270Field getField()
   {
   		return chars[cursorPosition].getField();
   }		
   /**
    * This method returns the RW3270Field object at the specified buffer address
    * @return RW3270Field object
    * @param i Screen position of the requested RW3270Field Object.
    */
   public RW3270Field getField(int i)
   {
   		return chars[i].getField();
   }	
   	
   /**
    * Sets the encryption setting for the 3270 session... 
    * @param true will turn encryption on, false will turn encryption off
    * @deprec
    */
   public void setEncryption(boolean encryption)
   {
      tn.setEncryption(encryption);
   }
   /**
    * Presses the specified PF Key, as specified in the constants for this class.  For example
    * to press PF1, call rw.PF(RW3270.PF1);
    * @param int - the key to be pressed, as specified in the constants for this class.
    */
   public void PF(int key)
   {
      if(keyboardLocked)
         return;
      aid = (short)key;
      tnParser.readModified();
   }
   /**
    * Presses the specified PA Key, as specified in the constants for this class.  For example
    * to press PA1, call rw.PA(RW3270.PA1);
    * @param int - the key to be pressed, as specified in the constants for this class.
    */   
   public void PA(int key)
   {
      if(keyboardLocked)
         return;
      aid = (short)key;
      tnParser.readModified();
   }
   /**
    * Inserts the specified ASCII character at the current cursor position if the current
    * field is unprotected, and advances the cursor position by one.  This is useful
    * for implementations that accept keyboard input directly.  For implementations
    * that don't require character-by-character input, use RW3270Field.setData(String data)
    * instead.
    * @see RW3270Field
    * @param - the keyboard/ASCII number corresponding to the key pressed.
    * @throws IsProtectedException if the current field is protected.
    */
   public void type(int key)
   throws IsProtectedException
   {
      if(keyboardLocked)
         return;
      int oldPos = cursorPosition;
      if(getChar(oldPos).getField().isProtected()||getChar(oldPos).isStartField())
         throw new IsProtectedException();
      //If the field is numeric, check the input
//			if(getChar(oldPos).getField().isNumeric())         
//			{
//				//if the input is not a number, return
//				try
//				{	
//					Integer.parseInt((char)key);
//				}	
//				catch(NumberFormatException e)
//				{	
//					return;
//				}
//			}
      getChar(oldPos).setChar((char)key);
      getChar(oldPos).getField().isModified(true);
      cursorPosition++;
      if(cursorPosition >= rows*cols)
         cursorPosition = 0;
      client.cursorMove(oldPos, cursorPosition);
   }
   /**
    * Deletes the current character (by setting it to ' '). 
    * @throws IsProtectedException if the current field is protected.
    */
   public void delete()
   throws IsProtectedException
   {
      if(keyboardLocked)
         return;
      if(getChar(cursorPosition).getField().isProtected())
         throw new IsProtectedException();
      getChar(cursorPosition).setChar(' ');
      getChar(cursorPosition).getField().isModified(true);
      client.cursorMove(cursorPosition, cursorPosition);
   }
   /**
    * This method sets the cursor position to the first character
    * position of the last unprotected field.
    */
   public void backTab()
   {
      if(keyboardLocked)
         return;
      int oldPos = cursorPosition;
      cursorPosition = getPreviousUnprotectedField(cursorPosition);
      client.cursorMove(oldPos, cursorPosition);
   }
   /**
    * This method advances the cursor position to the first character
    * position of the next unprotected field.
    */
   public void tab()
   {
      if(keyboardLocked)
         return;
      int oldPos = cursorPosition;
      cursorPosition = getNextUnprotectedField(cursorPosition);
      client.cursorMove(oldPos, cursorPosition);
   }
   /**
    * Deletes the current character (by setting it to ' ') and decrements the cursor position
    * by one.
    * @throws IsProtectedException if the current field is protected.
    */   
   public void backspace()
   throws IsProtectedException
   {
      if(keyboardLocked)
         return;
      if(getChar(cursorPosition - 1).getField().isProtected()||getChar(cursorPosition - 1).isStartField())
         throw new IsProtectedException();
      int oldPos = cursorPosition;
      cursorPosition--;
      if(cursorPosition < 0)
         cursorPosition = (short)((rows*cols) - 1);
      getChar(cursorPosition).getField().isModified(true);
      getChar(cursorPosition).setChar(' ');
      client.cursorMove(oldPos, cursorPosition);
   }
   /**
    * Moves the cursor position one character to the left, wrapping when necessary.
    * Returns without moving the cursor if the terminal is currently locked.
    */
   public void left()
   {
      if(keyboardLocked)
         return;
      int oldPos = cursorPosition;
      cursorPosition--;
      if(cursorPosition < 0)
         cursorPosition = (short)((rows*cols) - 1);
      client.cursorMove(oldPos, cursorPosition);
   }
   /**
    * Moves the cursor position one character to the right, wrapping when necessary.
    * Returns without moving the cursor if the terminal is currently locked.
    */   
   public void right()
   {
      if(keyboardLocked)
         return;
      int oldPos = cursorPosition;
      cursorPosition++;
      if(cursorPosition == rows*cols)
         cursorPosition = 0;
      client.cursorMove(oldPos, cursorPosition);
   }
   /**
   	* Moves the cursor position 'up' one row from it's current position. 
   	* For example, in an 80-column screen, calling the <code>up()</code> mehtod
   	* will decrease the cursor position by 80.  This method will 'wrap' from the first
   	* row to the last row 
    */      
   public void up()
   {
      if(keyboardLocked)
         return;
      int oldPos = cursorPosition;
      cursorPosition -= cols;
      if(cursorPosition < 0)
         cursorPosition = (short)((rows*cols) + cursorPosition);
      client.cursorMove(oldPos, cursorPosition);
   }
   /**
   	* Moves the cursor position 'down' one row from it's current position. 
   	* For example, in an 80-column screen, calling the <code>down()</code> mehtod
   	* will increase the cursor position by 80.  This method will 'wrap' from the last
   	* row to the first row 
    */         
   public void down()
   {
      if(keyboardLocked)
         return;
      int oldPos = cursorPosition;
      cursorPosition += cols;
      if(cursorPosition >= rows*cols)
         cursorPosition = (short)(cursorPosition - (rows*cols));
      client.cursorMove(oldPos, cursorPosition);
   }

   public void keyFieldMark()
   {
   }
   public void keyNewLine()
   {
      if(keyboardLocked)
         return;
   }
   /**
    * This method moves the cursor to the first character of the first
    * unprotected field in the data buffer.
    */
   public void home()
   {
      if(keyboardLocked)
         return;
      int oldPos = cursorPosition;
      Enumeration e = fields.elements();
      while(e.hasMoreElements())
      {
         RW3270Field nextField = (RW3270Field)e.nextElement();
         //if this is an unprotected field
         if(!nextField.isProtected())
         {	
            setCursorPosition((short)(nextField.getBegin() + 1));
            client.cursorMove(oldPos, cursorPosition);
            break;
         }
      }
   }
   public void reset()
   {
      if(keyboardLocked)
         return;

   }
   /**
    * Executes the tn3270 clear command
    */
   public void clear()
   {
      if(keyboardLocked)
         return;
      aid = CLEAR;
      tnParser.readModified();
      for(int i = 0; i < chars.length; i++)
      {
         ((RW3270Char)chars[i]).clear();
         display[i] = ' ';
      }
      resumeParentThread();         
   }
   /**
    * Executes the sysreq command
    */
   public void sysreq()
   {
   		if(keyboardLocked)
   			return;
   	  aid = SYSREQ;
   	  tnParser.readModified();
   }
   /**
    * Presses the 'enter' key on the terminal.  This sets the AID to ENTER, and 
	 * fires off a Read Modified command on the current buffer.
	 */
   public void enter()
   {
      if(keyboardLocked)
         return;
      aid = ENTER;
      tnParser.readModified();
   }
	/**
	 * Searches the current data buffer (screen) for the specified string.
	 * @return true if the string was found, false otherwise.
	 * @param search The string to search the data buffer for.
	 */
	public boolean contains(String search)
	{
      if(search == null)
         return false;
      if((new String(getDisplay())).indexOf(search) == -1)
			return false;
		else
			return true;
	}
	/**
	 * Blocks until the specified string is found in the data buffer (screen) or
	 * until the specified timeout is reached.  This method is extrememly useful
	 * when the host may be sending several screens in response to your request, and
	 * you want to block until you reach the response you're waiting for.
	 * For example:<BR><BR>
	 * <UL>
	 * <code>
	 * ...<BR>
	 * //Create a RW3270 object<BR><BR>
	 *	RW3270 rw = new RW3270(this);<BR><BR>
	 * //Connect to the SessionServer and specify a host<BR><BR>
	 * rw.connect("3.3.88.3", 6870, "hollis.harvard.edu", 23, true);<BR><BR>
	 * //This will block for 30 seconds or until the string<BR>
	 * //H A R V A R D is found.  it also returns a boolean to<BR>
	 * //let us check the results<BR><BR>
	 * <B>boolean t = rw.waitFor("H A R V A R D", 30);</B><BR><BR>
	 * //Now we'll execute two different blocks of code depending<BR>
	 * //on whether our string was found:<BR><BR>
	 * if(t)<BR>
	 * {<BR>
	 * <UL>
	 * 	out.print("Got it:");<BR>
	 *		out.print(new String(rw.getDisplay()));<BR>
	 * </UL>
	 * }<BR>
	 * else<BR>
	 *	{<BR>
	 * <UL>
	 *	 out.print("Didn't get it:>");<BR>
	 *	 out.print(new String(rw.getDisplay()));<BR>
	 * </UL>
	 *	}<BR>
 	 * </code>
	 * </UL>
	 * @param search The string to wait for
	 * @param timeout Number of seconds to wait for the search string.
	 * @return true if the string was found, false if the timeout was reached
	 * 	without finding it.
	 */
	public boolean waitFor(String search, int timeout)
	{
		//set the class variable waitForTimeout so the run method can
		//get at it.
		waitForTimeout = timeout;
		//set the class variable waitForSearch so the rest of the class
		//can get at it.
		waitForSearch = search;
		//Suspend the calling thread
      try
      {
         synchronized(waitObject)
         {
 			waitForReturn = false;
            waitObject.wait(timeout * 1000);
         }
      }
      catch(InterruptedException e)
      {

      }
      return waitForReturn;
	}
	//This is only here to act as the timeout timer for the 
	//waitFor method
	public void run()
	{
		try
		{
			Thread.sleep(waitForTimeout * 1000);
		}
		catch(InterruptedException e)
		{
      }
		waitForReturn = false;
		waitForThread.notify();
	}
   /**
    * Sets session data on the session server
    */
   public void setSessionData(String key, String value)
   {
      tn.setSessionData(key, value);
   }
   private short                 cursorPosition;//integer representation of the current cursor position
   private short                 aid;//current aid
   private Vector                fields;//Vector of current fields
   private RW3270Char            chars[];//array of current characters
   private char                  display[];
   private RWTelnet              tn;//Telnet object
   private RWTn3270StreamParser  tnParser;//Tn3270StreamParser object
   private RWTnAction            client;
   private short                 rows, cols, tnModel;
   private boolean               keyboardLocked;
   private short                 AID;
   private Thread                sessionThread;
   private Thread					   parentThread;
	private Thread						waitForThread;
   private Thread                timerThread;
	private int							waitForTimeout;
	private boolean					waitForReturn;
	private String						waitForSearch;
   private WaitObject            waitObject;
   
   /********************************************************************/
   /*                   AID Constants Table 3-4                        */
   /********************************************************************/

   public final static short   AID_NO               = 0x60;
   public final static short   AID_SF               = 0x88;
   public final static short   AID_READ_PARTITION   = 0x61;
   public final static short   AID_TRIGGER          = 0x7F;
   public final static short   SYSREQ           = 0xF0;
   public final static short   PF1              = 0xF1;
   public final static short   PF2              = 0xF2;
   public final static short   PF3              = 0xF3;
   public final static short   PF4              = 0xF4;
   public final static short   PF5              = 0xF5;
   public final static short   PF6              = 0xF6;
   public final static short   PF7              = 0xF7;
   public final static short   PF8              = 0xF8;
   public final static short   PF9              = 0xF9;
   public final static short   PF10             = 0x7A;
   public final static short   PF11             = 0x7B;
   public final static short   PF12             = 0x7C;
   public final static short   PF13             = 0xC1;
   public final static short   PF14             = 0xC2;
   public final static short   PF15             = 0xC3;
   public final static short   PF16             = 0xC4;
   public final static short   PF17             = 0xC5;
   public final static short   PF18             = 0xC6;
   public final static short   PF19             = 0xC7;
   public final static short   PF20             = 0xC8;
   public final static short   PF21             = 0xC9;
   public final static short   PF22             = 0x4A;
   public final static short   PF23             = 0x4B;
   public final static short   PF24             = 0x4C;
   public final static short   PA1              = 0x6C;
   public final static short   PA2              = 0x6E;
   public final static short   PA3              = 0x6B;
   public final static short   CLEAR            = 0x6D;
   public final static short   CLEAR_PARTITION  = 0x6A;
   public final static short   ENTER            = 0x7D;
}

class WaitObject
{
   public synchronized void getLock()
   {

   }
}
