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

package com.ino.freehost.html3270;

import com.ino.freehost.client.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

public class RWHtml3270 implements RWTnAction
{
	public RWHtml3270(String sessionserver, int sessionserverport, String host, int port, boolean encryption, HttpServletResponse res)
	{ 
		rw = new RW3270(this);
		rw.connect(sessionserver, sessionserverport, host, port, encryption);
		rw.waitForNewData();
		returnData(res);
	}
	public void status(int i)
	{
	}
	public void beep()
	{
	}
	public void disconnect()
	{
		rw.disconnect();
	}
	public void incomingData()
	{
	}
	public void cursorMove(int x, int y)
	{
	}
	public void broadcastMessage(String s)
	{
	}
	public void process(HttpServletRequest req, HttpServletResponse res)
	throws IOException
	{
		//input code goes here
		 if(req.getParameter("Disconnect") != null)
		 {
		 		rw.disconnect();
		 		//res.sendRedirect("index.htm");
		 		return;
		 }		
		 if(req.getParameter("AID").equals("Refresh"))
		 {
		 		returnData(res);
		 		return;
		 }

		 sendData(req, res);
		 //block for new data
		 rw.waitForNewData();
		/*
		 * Output: create an instance of the DataListener class.  When the
		 * RW3270 engine calls the incomingData callback, DataListener will
		 * print the 3270 screen back to the browser
		 */
		 returnData(res);

	}
public void sendData(HttpServletRequest req, HttpServletResponse res)
{
   Enumeration e = req.getParameterNames();
    while(e.hasMoreElements())
    {
        String inName = (String)e.nextElement();
        String inValue = req.getParameter(inName.trim());
        int pos;
        try
        {
            pos = Integer.parseInt(inName);
        }
        //this catches the one exception to the
        //<...name=int> rule, where <name = buttonname>,
        //when this form element is encountered,
        //it needs to be skipped, so any remaining
        //field data can be typed to the 3270 screen.
        catch(NumberFormatException ee){continue;}
        if(!inValue.equals(""))//if an input field is blank, why bother?
        {
            try{
            rw.getField(pos).setData(inValue.trim());	
            }
            catch(NullPointerException ee)
            {                    
                try{
                res.sendRedirect("index.htm");//if there's a problem with
                                              //the session, we don't want them
                                              //sitting there until their browser
                                              //times out... send them to the index page
                }
                catch(IOException eee){}
            }
            catch(IOException eeee)
            {
            	System.out.println("Data too long for field...");
            }
            catch(IsProtectedException eeeee)
            {}
        }
    }
    if(req.getParameter("Disconnect") != null)
    {
        rw.disconnect();
        try{
            res.sendRedirect("index.htm");
        }
        catch(IOException ee){}
    }
    else
    {
    	String aid = req.getParameter("AID");
    	if(aid != null)
      {
      	if(aid.equals("Enter"))
      		rw.enter();
      	if(aid.equals("Clear"))
      		rw.clear();
      	if(aid.equals("PF1"))
      		rw.PF(RW3270.PF1);
      	if(aid.equals("PF2"))
      		rw.PF(RW3270.PF2);
      	if(aid.equals("PF3"))
      		rw.PF(RW3270.PF3);
      	if(aid.equals("PF4"))
      		rw.PF(RW3270.PF4);      		
      	if(aid.equals("PF5"))
      		rw.PF(RW3270.PF5);      		
      	if(aid.equals("PF6"))
      		rw.PF(RW3270.PF6);      		
      	if(aid.equals("PF7"))
      		rw.PF(RW3270.PF7);      		
      	if(aid.equals("PF8"))
      		rw.PF(RW3270.PF8);      		
      	if(aid.equals("PF9"))
      		rw.PF(RW3270.PF9);      		
      	if(aid.equals("PF10"))
      		rw.PF(RW3270.PF10);      		
      	if(aid.equals("PF11"))
      		rw.PF(RW3270.PF11);      		
      	if(aid.equals("PF12"))
      		rw.PF(RW3270.PF12);      		
      	if(aid.equals("PF13"))
      		rw.PF(RW3270.PF13);      		
      	if(aid.equals("PF14"))
      		rw.PF(RW3270.PF14);      		
      	if(aid.equals("PF15"))
      		rw.PF(RW3270.PF15);      		
      	if(aid.equals("PF16"))
      		rw.PF(RW3270.PF16);      		
      	if(aid.equals("PF17"))
      		rw.PF(RW3270.PF17);      		
      	if(aid.equals("PF18"))
      		rw.PF(RW3270.PF18);      		
      	if(aid.equals("PF19"))
      		rw.PF(RW3270.PF19);      		
      	if(aid.equals("PF20"))
      		rw.PF(RW3270.PF20);      		
      	if(aid.equals("PF21"))
      		rw.PF(RW3270.PF21);      		
      	if(aid.equals("PF22"))
      		rw.PF(RW3270.PF22);      		
      	if(aid.equals("PF23"))
      		rw.PF(RW3270.PF23);      		
      	if(aid.equals("PF24"))
      		rw.PF(RW3270.PF24);      		
      	if(aid.equals("PA1"))
      		rw.PA(RW3270.PA1);
      	if(aid.equals("PA2"))
      		rw.PA(RW3270.PA2);      		
      	if(aid.equals("PA3"))
      		rw.PA(RW3270.PA3);      		
      		
      }
		}
	}
public void returnData(HttpServletResponse res)
	{
			StringBuffer sb = new StringBuffer();
			RW3270Char chars[] = rw.getDataBuffer();
			for(int i = 0; i < chars.length; i++)
			{
				//reached the end of a row
				if(i % rw.getCols() == 0)
					sb.append("\n");
				RW3270Char currChar = chars[i];
				if(currChar.isStartField())
				{
					 sb.append(" ");
					 RW3270Field f = currChar.getField();	
					 String color = (f.isBold() && f.getForegroundColor() == RW3270Field.DEFAULT_FGCOLOR)?"white":translateColor(f.getForegroundColor());
					 sb.append("<font color=\"" + color + "\">");
					 /*
					  * If the field is unprotected, create an HTML input
					  * box.  We need to increment the counter the length
					  * of the field, as well.
					  * TO DO:  Trap for fields that would wrap longer than
					  * the current line.
					  */
					 if(!f.isProtected())
					 {
					 		String type = (f.isHidden())?"password":"text";	
					 		sb.append("<INPUT type=\"" + type + "\" size=\"" + (f.size() - 1) +
					 							"\" name=\"" + f.getBegin() + "\" value=\"" + new String(f.getDisplayChars()).substring(1).trim() +
					 							"\" maxlength=\"" + (f.size()) +"\">");
					 	  i += f.size();
					 	  //System.out.println("Input contents: " + new String(f.getDisplayChars()));
					 }
					 //System.out.println(f.getEnd());
					 continue;
				}
				/*
				 * If the character is not a start field, simply add it's
				 * display representation to the output string
				 */
				sb.append(chars[i].getDisplayChar());
			}
			try
			{
				res.getOutputStream().print(sb.toString());
			}
			catch(IOException e)
			{
				System.out.println(e);
			}
		}
   /**
    * This translates the int colors stored in a Field Attribute into an HTML
    * Color 
    */
   private String translateColor(int c)
   {
      switch(c)
      {
      case RW3270Field.DEFAULT_BGCOLOR:
         return "#000000";
      case RW3270Field.BLUE:
         return "Blue";
      case RW3270Field.RED:
         return "Red";
      case RW3270Field.PINK:
         return "Pink";
      case RW3270Field.GREEN:
         return "#00FF00";
      case RW3270Field.TURQUOISE:
         return "Cyan";
      case RW3270Field.YELLOW:
         return "Yellow";
      case RW3270Field.DEFAULT_FGCOLOR:
         return "Cyan";
      case RW3270Field.BLACK:
         return "Black";
      case RW3270Field.DEEP_BLUE:
         return "Blue";
      case RW3270Field.ORANGE:
         return "Orange";
      case RW3270Field.PURPLE:
         return "Purple";
      case RW3270Field.PALE_GREEN:
         return "LightGreen";
      case RW3270Field.PALE_TURQUOISE:
         return "Cyan";
      case RW3270Field.GREY:
         return "Grey";
      case RW3270Field.WHITE:
         return "White";
      }
      return "Black";
   }			
	
	private RW3270 rw;
	private boolean dataIn;
	private HttpServletRequest req;
}
