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

import java.util.*;
import java.applet.*;
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import com.ino.freehost.client.*;
import com.ino.freehost.client.Host;
import java.io.InputStream;
/**
 * TODO:  Implement a paintField(field) and paintChar(RW3270Char) method
 */
class RHTest extends Frame implements Runnable
{

    String host, defaulthost;
    int port, defaulthostport;
    boolean encryption;
    Hashtable available;
    Font f;
    Font basefont;
    FontMetrics fm;
    MenuItem disconnect;
    CheckboxMenuItem showButtons;
    MenuBar menubar;
    RH3270Buttons rhbuttons;
    Label msg;
    RHPanel rhp;
    Applet parent;
    String helpabout;

    public static void main(String args[])
    {
       Hashtable h = new Hashtable();
       h.put("LOCIS", "locis.loc.gov");
       h.put("HOLLIS", "hollis.harvard.edu");
       h.put("INGRAM MICRO", "tn3270.ingrammicro.com");
       RHTest rh = new RHTest("hollis.harvard.edu", 23, "locis.loc.gov", h, null, null);
    }
      
    public RHTest(String host, int port, String defaulthost, Hashtable h, Component c, Applet a)
    {
        super("RightHost3270");
        this.parent = a;
        helpabout = a.getParameter("helpabout");
        //this.defaulthost = defaulthost;
        encryption = Boolean.valueOf(a.getParameter("encryption")).booleanValue();
        //System.out.println(helpabout);
        available = h;
        this.port = port;
        this.host = host;
        setResizable(false);
        //setBackground(Color.black);
	InputStream is = getClass().getResourceAsStream("cour.ttf");
	GraphicsEnvironment gre = GraphicsEnvironment.getLocalGraphicsEnvironment();
	Font[] allfonts = gre.getAllFonts();
	for ( int i = 0; i < allfonts.length; i++ ) {
	    System.out.println("FONT: " + allfonts[i].getName());
	    if ( allfonts[i].getName().equals("OCR A Extended")) {
		basefont = allfonts[i];
	    } // end of if ()
	    
	} // end of for ()
	f = basefont.deriveFont(Font.PLAIN, 12);
	System.out.println("FONT: " + f.getName());

        setLayout(new BorderLayout());
        menubar = new MenuBar();
        setMenuBar(menubar);
        Menu file = new Menu("File");
        file.add(new MenuItem("Print"));
        file.addSeparator();
        file.add(new MenuItem("New Window"));
        menubar.add(file);
        Menu connect = new Menu("Connect");
        Enumeration e = available.keys();
        while(e.hasMoreElements())
            connect.add(new MenuItem((String)e.nextElement()));
	//        if(a.getParameter("manualEntry").equals("true"))
	//  connect.add(new MenuItem("Other..."));
        connect.addSeparator();
        connect.add(disconnect = new MenuItem("Disconnect"));
        menubar.add(connect);
        Menu options = new Menu("Options");
        Menu fonts = new Menu("Font Size");
        fonts.add(new MenuItem("10"));
        fonts.add(new MenuItem("12"));
        fonts.add(new MenuItem("14"));
        fonts.add(new MenuItem("16"));
        fonts.add(new MenuItem("18"));
        fonts.add(new MenuItem("20"));
        fonts.add(new MenuItem("22"));
        fonts.add(new MenuItem("24"));
        options.add(fonts);
        Menu fontcolor = new Menu("Font Color");
        Menu dfFontColor = new Menu("Default Font");
        dfFontColor.add(new MenuItem("Black"));
        dfFontColor.add(new MenuItem("White"));
        dfFontColor.add(new MenuItem("Green"));
        dfFontColor.add(new MenuItem("Red"));
        dfFontColor.add(new MenuItem("Blue"));
        dfFontColor.add(new MenuItem("Orange"));
        dfFontColor.add(new MenuItem("Turquoise"));
        dfFontColor.add(new MenuItem("Dark Blue"));
        dfFontColor.add(new MenuItem("Light Green"));
        fontcolor.add(dfFontColor);
        Menu bldFontColor = new Menu("Bold Font");
        bldFontColor.add(new MenuItem("Black"));
        bldFontColor.add(new MenuItem("White"));
        bldFontColor.add(new MenuItem("Green"));
        bldFontColor.add(new MenuItem("Red"));
        bldFontColor.add(new MenuItem("Blue"));
        bldFontColor.add(new MenuItem("Orange"));
        bldFontColor.add(new MenuItem("Turquoise"));
        bldFontColor.add(new MenuItem("Dark Blue"));
        bldFontColor.add(new MenuItem("Light Green"));
        fontcolor.add(bldFontColor);
        options.add(fontcolor);
        options.addSeparator();
        Menu bgcolor = new Menu("Background Color");
        bgcolor.add(new MenuItem("Black"));
        bgcolor.add(new MenuItem("White"));
        bgcolor.add(new MenuItem("Green"));
        bgcolor.add(new MenuItem("Red"));
        bgcolor.add(new MenuItem("Blue"));
        bgcolor.add(new MenuItem("Orange"));
        bgcolor.add(new MenuItem("Turquoise"));
        bgcolor.add(new MenuItem("Dark Blue"));
        bgcolor.add(new MenuItem("Light Green"));
        options.add(bgcolor);
        options.addSeparator();
        options.add(showButtons = new CheckboxMenuItem("Buttons"));        
        menubar.add(options);
        Menu about = new Menu("Help");
        menubar.add(about);
        about.add(new MenuItem("About"));
        rhp = new RHPanel(parent);
        add("Center", rhp);
        //System.out.println(rhp.size().width + " " + rhp.size().height);
        //Center on screen
        Dimension screen_size, frame_size;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        resize(rhp.size().width + insets().top + insets().bottom, rhp.size().height + insets().right + insets().left);
        frame_size = this.size();
        int offX = frame_size.width;
        int offY = frame_size.height;
        //If we have parent component, offset the new window from
        //it (cascade windows)
        if(c != null)
        {
           move(c.location().x + 20, c.location().y + 20);
        }
        else
           move((screen_size.width - offX)/2, (screen_size.height - offY)/2);
        repaint();
        show();
        if(available.size() == 1)
        {
        	  Enumeration el = available.elements();
        	  Host hos = (Host)el.nextElement();
		        setTitle("RightHost 3270 - Connecting to " + hos.friendlyName);
		        rhp.connect(host, port, hos.hostName, hos.port, encryption);
		        requestFocus();
		        setTitle("RightHost 3270 - Connected to " + hos.friendlyName);
		    }
		    else
		    	setTitle("RightHost 3270 - Not Connected");
	setFont(f);
        rhp.requestFocus();
    }
    public void disconnect()
    {
    	rhp.disconnect();
    }
    public boolean gotFocus(Event evt, Object arg)
    {
       super.gotFocus(evt, arg);
       rhp.requestFocus();
       return true;
    }
    public boolean action(Event evt, Object arg)
    {
        if(evt.target instanceof CheckboxMenuItem)
        {
          CheckboxMenuItem cmi = (CheckboxMenuItem)evt.target;
          if(cmi.getState())
          {
            //System.out.println("Buttons checked...");
            resize(this.size().width, this.size().height + 90);
            RH3270Buttons rhbuttons = new RH3270Buttons(rhp.getRW3270());
            add("South", rhbuttons);
          }
          else
          {
             resize(this.size().width, this.size().height - 90);
             remove(getComponent(1));
          }
          return true;
        }
           
        if(evt.target instanceof MenuItem)
        {

            if(arg.equals("10"))
            {
               fontSize(10);
            }
            if(arg.equals("12"))
            {
               fontSize(12);
            }
            if(arg.equals("14"))
            {
               fontSize(14);
            }
            if(arg.equals("16"))
            {
               fontSize(16);
            }
            if(arg.equals("18"))
            {
               fontSize(18);
            }
            if(arg.equals("20"))
            {   
               fontSize(20);
            }
            if(arg.equals("22"))
            {
               fontSize(22);
            }
            if(arg.equals("24"))
            {
               fontSize(24);
            }
            if(arg.equals("Black"))
            {
               setFontColor(evt.target, Color.black);
            }
            if(arg.equals("White"))
            {
               setFontColor(evt.target, Color.white);
            }
            if(arg.equals("Red"))
            {
               setFontColor(evt.target, Color.red);
            }
            if(arg.equals("Pink"))
            {
               setFontColor(evt.target, Color.pink);
            }
            if(arg.equals("Blue"))
            {
               setFontColor(evt.target, Color.blue);
            }
            if(arg.equals("Green"))
            {
               setFontColor(evt.target, Color.green);
            }
            if(arg.equals("Turquoise"))
            {
               setFontColor(evt.target, Color.cyan);
            }
            if(arg.equals("Dark Blue"))
            {
               setFontColor(evt.target, new Color(0, 51, 102));
            }
            if(arg.equals("Light Green"))
            {
               setFontColor(evt.target, new Color(204, 255, 204));
            }
            if(arg.equals("Orange"))
            {
               setFontColor(evt.target, Color.orange);
            }
            if(arg.equals("Disconnect"))
            {
               setTitle("RightHost 3270 - Disconnected");
               rhp.disconnect();
               return true;
            }
            if(arg.equals("Print"))
            {
               rhp.print();
               return true;
            }
            if(arg.equals("New Window"))
            {
               RHTest rht = new RHTest(host, port, defaulthost, available, this, parent);
               return true;
            }
            if(((MenuItem)((MenuItem)evt.target).getParent()).getLabel().equals("Connect"))
            {
               rhp.disconnect();
               if(arg.equals("Other..."))
               {
                  Thread t =new Thread(this);
                  //t.start();
                  return true;
               }
               super.setTitle("RightHost 3270 - Connecting to " + arg);
               rhp.connect(host, port, ((Host)available.get(arg)).hostName, ((Host)available.get(arg)).port, encryption);
               super.setTitle("RightHost 3270 - Connected to " + arg);
            }
            if(arg.equals("About"))
            {

            }
            return true;
        }
        return false;
    }
    private void fontSize(int size)
    {
       f = new Font("Monospaced", Font.PLAIN, size);
       rhp.setFont(f);
       resize(rhp.size().width + insets().top + insets().bottom, rhp.size().height + insets().right + insets().left);
       repaint();
    }
    public void setFontColor(Object menu, Color c)
    {
       MenuItem mi = (MenuItem)menu;
       MenuItem p = (MenuItem)mi.getParent();
       if(p.getLabel().equals("Default Font"))
          rhp.setForegroundColor(c);
       else if(p.getLabel().equals("Bold Font"))
          rhp.setBoldColor(c);
       else
          rhp.setBackgroundColor(c);
    }
    public boolean handleEvent(Event evt)
    {
        if(evt.id == Event.WINDOW_DESTROY)
        {
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
         return super.handleEvent(evt);
    }
    public void run()
    {
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
    

}

class RHPanel extends Panel implements Runnable, RWTnAction
{
   int callTracker;
    Font basefont;
   public RHPanel(Applet a)
   {
      this.parent = a;
      callTracker = 0;
      fontsize = 12;
      rw = new RW3270(2, this);
      //chars = rw.getDataBuffer();
      //rw.setEncryption(false);
      //rw.connect(host, port, null, false);
      defaultBGColor = Color.black;
      messageOnScreen = false;
      defaultFGColor = Color.cyan;
      cursorColor    = Color.red;
      boldColor = Color.white;
      setBackground(defaultBGColor);
      basefont = new Font("Lucida Sans Typewriter Regular", Font.PLAIN, 12);
      font = basefont.deriveFont(12);
      fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
      x = fm.charWidth(' ');
      y = fm.getHeight();
      ascent = fm.getAscent();
      resize(x * 85, y * 30 + 7);
      show();
      requestFocus();
      //setLayout(null);
   }
   public void broadcastMessage(String msg)
   {
   		//System.out.println("Broadcast message..");
	   	StrMessage = msg;
	   	if(msg.indexOf("<too many connections>") != -1)
	   	{
	   		StrMessage = msg.substring(23);
	   		tooManyConnections = true;
	   	}
			setWindowMessage(MSG_BROADCAST);
			paintWindowMessage();
   }
   public void setWindowMessage(int msg)
   {
   		noMessage = msg;
   		paintWindowMessage();
   }
   public void setWindowMessage(String msg)
   {
   		StrMessage = msg;
   		paintWindowMessage();
   }
   public RW3270 getRW3270()
   {
      return rw;
   }
   public void disconnect()
   {
      rw.disconnect();
      paintStatus("Disconnected");
   }
   public void connect(String host, int port, String host3270, int port3270, boolean encryption)
   {
      if(host == null)
         return;
      messageOnScreen = false;
      paintStatus("Connecting");
      rw.connect(host, port, host3270, port3270, encryption);
   }
   public void setFont(Font f)
   {
      font = f;
      fontsize = f.getSize();
      fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
      x = fm.charWidth(' ');
      y = fm.getHeight();
      System.out.println("FONT METRICS: " + x + " ; " + y);
      ascent = fm.getAscent();
      resize(x * 85, y * 30 + 7);
      updateScreen(getGraphics());
   }
   public void setForegroundColor(Color c)
   {
      defaultFGColor = c;
      repaint();
   }
   public void setBackgroundColor(Color c)
   {
      defaultBGColor = c;
      setBackground(c);
      repaint();
   }
   public void setBoldColor(Color c)
   {
      boldColor = c;
      repaint();
   }
  /**
   * This repaints the screen with a message
   */ 
	public void paintWindowMessage() 
	{
		String      message = null;
		Graphics g;
		messageOnScreen = true;
		if ((g = getGraphics()) == null)
			return;
		g.setFont(font);
		g.setColor(defaultBGColor);
		g.fillRect(3, 3, size().width - 6, size().height - 6);
		switch (noMessage)
		{
		case MSG_CLOSED_REMOTE:
			message = "Your connection to the RightHost 3270 Session Server was lost or could not be established. " +
								"Please try your session again, and contact your System Administrator if the problem persists.";
			break;
		case MSG_STRING:
		case MSG_BROADCAST:
			message = StrMessage;
			break;
		}
		g.setColor(Color.red);
		g.draw3DRect(5 + x * 20, y * 2, x * 40, x * 40, true);
		g.setColor(Color.white);
		g.setFont(new Font("Helvetica", Font.PLAIN, fontsize));
		/*
			 the next few lines of code handle broadcast messages of varying length
			 and therefore had to be able to auto-wrap on whitespace
		*/
		if (message.length() <= 40)
			g.drawString(message, x * 22, y * 3);
		else
		{
			int lineNo = 0;
			for (int i = 0; i < message.length(); i++)
			{
				if ((message.length() - i) <= 45)
				{
					g.drawString(message.substring(i, message.length()), x * 22, y * (3 + lineNo));
					break;
				}
				else
				{
					String line = message.substring(i, i + 45);
					int lastSpace = line.lastIndexOf(' ');                    
					g.drawString(message.substring(i, i + lastSpace), x * 22, y * (3 + lineNo));
					i = i + lastSpace;
					lineNo++;                    
				}
			}
		}
		if (noMessage == MSG_BROADCAST && tooManyConnections == false)
		{
			g.setFont(new Font("Helvetica", Font.BOLD, fontsize));
			g.drawString("Message From Your System Administrator:", x * 22, y * 2 - 5);
			g.setFont(new Font("Helvetica", Font.PLAIN, fontsize - 2));
			g.drawString("Press <ESC> to return to your session.", x * 22, y * 19);
		}
	}
   
   public void paint(Graphics g)
   {
      if(g == null)
         return;
      //g.setColor(defaultBGColor);
      //g.fillRect(0, 0, size().width, size().height);
      if(messageOnScreen)
      {
      	paintWindowMessage();
      	return;
      }
      updateScreen(g);
      status(RWTnAction.READY);
   }
   public void updateScreen(Graphics g)
   {
      //start the blink thread
      if(t != null)
      {
         t.stop();
         t = null;
         System.gc();
      }
      t = new Thread(this);
      t.start();
      g.setColor(defaultBGColor);
      g.fillRect(0, 0, x * 85, y * 25 - 4);
      try
      {
         g.setFont(font);
         Color bgcolor = translateColor(RW3270Field.DEFAULT_BGCOLOR);
         Color fgcolor = translateColor(RW3270Field.DEFAULT_FGCOLOR);
         //if the screen is unformatted, paint the entire
         //data buffer:
         //System.out.println("Field count: " + rw.getFields().size());
         if(rw.getFields().size() < 1)
         {
            char c[] = rw.getDisplay();
            byte ca[] = new byte[c.length];
            for(int i = 0; i < c.length; i++)
            	ca[i] = (byte)c[i];
            paintData(ca, c.length, 0, g, bgcolor, fgcolor, false, false);
         }
         else//formatted... check for fields:
         {
            Enumeration e = rw.getFields().elements();
            while(e.hasMoreElements())
            {
               RW3270Field f = (RW3270Field)e.nextElement();
               paintField(g, f, false);
            }
         }      
      }
      catch(NullPointerException e)
      {
         return;
      }
      paintCursor(rw.getCursorPosition());
   }
   public void run()
   {
      //blinked is a toggle.  When true,
      //the affected text is 'off'...
      boolean blinked = false;
      while(true)
      {
         try
         {
            Thread.sleep(1000);
         }
         catch(InterruptedException e)
         {
            System.err.println(e);
         }
         Enumeration e = rw.getFields().elements();
         while(e.hasMoreElements())
         {
            RW3270Field f = (RW3270Field)e.nextElement();
            if(f.getHighlighting() == RW3270Char.HL_BLINK)
            {
               Graphics g = getGraphics();
               paintField(g, f, blinked);
            }
         }
         //paintCursor(rw.getCursorPosition(), blinked);
         blinked = !blinked;
         //System.out.println("Blink");
      }
   }
   /**
    * This method makes it easy to paint a field
    * @param   The current graphics context
    * @param   The field to paint
    * @param   Is this a blink off iteration?
    */
   private synchronized void paintField(Graphics g, RW3270Field f, boolean blink)
   {
      
      //long beg = System.currentTimeMillis();
      //System.out.println("pf");
      /*for(int i = 0; i < ca.length; i++)
      {
         ca[i] = c[i].getDisplayChar();
      }*/
      //int bufLen = f.getEnd() - f.getBegin();
      //bufLen = (bufLen < 0)?(chars.length - f.getBegin()) + f.getEnd():bufLen;
      //bufLen++;
      char ca[] = f.getDisplayChars();
      //int c = f.getBegin();
      //System.out.println(c + " " + bufLen);
      /*for(int i = 0; i < bufLen; i++, c++)
      {
         if(c == chars.length)
            c = 0;
         ca[i] = chars[c].getDisplayChar();
         //System.out.print(ca[i]);
      }*/
      Color fgcolor = translateColor(f.getForegroundColor());
      Color bgcolor = translateColor(f.getBackgroundColor());
      boolean underscore = false;
      boolean hidden     = false;
      if(!blink)
      {
         switch(f.getHighlighting())
         {
         case RW3270Char.HL_REVERSE:
            fgcolor = bgcolor;
            bgcolor = translateColor(f.getForegroundColor());
            break;
         case RW3270Char.HL_UNDERSCORE:
            underscore = true;
         }
      }
      else
         fgcolor = bgcolor;
      /*if(f.isHidden())
         hidden = true;*/
      if(f.isBold())
      {
         if(fgcolor == defaultFGColor)
            fgcolor = boldColor;
      }
      //new Font("Courier", Font.PLAIN, fontsize);
      else
         g.setFont(font);
      byte b[] = new byte[ca.length];
      for(int i = 0; i < b.length; i++)
         b[i] = (byte)ca[i];
      //System.out.println(callTracker+= (System.currentTimeMillis() - beg));
      //we have to overwrite the FieldAttribute ourselves
      g.setColor(defaultBGColor);
      g.fillRect((f.getBegin()%rw.getCols()) * x + (x + 5), (f.getBegin()/rw.getCols()) * y + 6, x, y - 1);
      paintData(b, b.length, f.getBegin(), g, bgcolor, fgcolor, underscore, hidden);
   }
   /**
    * This utility/reuse method takes an array of chars, the starting position, and
    * a graphics object to paint a field, screen, character, etc.
    * @param   c - array of characters
    * @param   bufLen - length of c
    * @param   startPos - the starting position, relative to the databuffer
    * @param   g - This component's paint object
    */
   private synchronized void paintData(byte[] c, int bufLen, int startPos, Graphics g, Color bgcolor, Color fgcolor, boolean under, boolean hidden)
   {
      long beg = System.currentTimeMillis();
      //number of columns in this screen
      //startPos = ((startPos + 1)%cols == 0)?startPos + 1:startPos;
      //a counter for keeping our place relative to the
      //character buffer passed to this method
      int counter = 0;
      //the first row this field will appear on
      int firstRow = startPos/rw.getCols();
      //the starting position, relative to the first row this field
      //appears on
      int firstRowStart = startPos%rw.getCols();
      //the number of characters that will appear on the first row
      int firstRowLen   = rw.getCols() - firstRowStart;
      firstRowLen = (firstRowLen > bufLen) ? bufLen : firstRowLen;
      //the number of rows (not including first
      //and last) that this field will take
      int loops         = (bufLen - firstRowLen)/rw.getCols();
      //System.out.println("rw.getCols(): " + cols + "Start pos: " + startPos + " First row: " + firstRow + " " + "First Row Start: " + firstRowStart + " FirstRowLen " + firstRowLen + " loops " + loops);
      //the number of characters that will appear
      //in the last row of this field.
      int lastRowLen    = (bufLen - firstRowLen)%rw.getCols();
      firstRow = (firstRow == rw.getRows())? 0: firstRow;
      //now let's draw the first row:
      g.setColor(bgcolor);
      g.fillRect((firstRowStart + 1) * x + (x + 5), firstRow * y + 7, (firstRowLen - 1) * x, y - 2);
      g.setColor(fgcolor);
      //draw the underline, if appropriate
      if(under)
         g.drawLine((firstRowStart + 1) * x + (x + 5), firstRow * y + 5 + y, (firstRowLen + firstRowStart) * x + (x + 5), firstRow * y + 5 + y);
      /*if(hidden)
         g.setColor(bgcolor);*/
      g.drawBytes(c, counter, firstRowLen, (firstRowStart) * x + (x + 5), firstRow * y + ascent + 5);
      counter += firstRowLen;
      //System.out.println(counter);
      //iterate through the 'full' rows
      for(int i = 0; i < loops; i++)
      {
         firstRow++;
         firstRow = (firstRow == rw.getRows())?0: firstRow;
         g.setColor(bgcolor);
         g.fillRect(x + 5, firstRow * y + 7, rw.getCols() * x, y + 2); 
         g.setColor(fgcolor);
         if(under)
            g.drawLine(x + 5, firstRow * y + 5 + y, rw.getCols() * x + 5, firstRow * y + 5 + y);
         /*if(hidden)
            g.setColor(bgcolor);*/
         g.drawBytes(c, counter, rw.getCols(), x + 5, firstRow * y + ascent + 5);
         //System.out.println(x + " " + y);
         counter += rw.getCols();
         //System.out.println(counter);
      }
      //if we're done, return
      if(counter >= bufLen - 1)
      {
         return;
      }
      //increment the row
      firstRow++;
      firstRow = (firstRow == rw.getRows())? 0: firstRow;
      //now draw the last, partial row
      g.setColor(bgcolor);
      g.fillRect(x + 5, firstRow * y + 7, (lastRowLen) * x, y + 2);
      g.setColor(fgcolor);
      if(under)
            g.drawLine(x + 5, firstRow * y + 5 + y, (lastRowLen) * x + 5, firstRow * y + 5 + y);
      /*if(hidden)
         g.setColor(bgcolor);*/
      g.drawBytes(c, counter, lastRowLen, x + 5, firstRow * y + ascent + 5);
   }
   /**
    * paintChar
    */
   protected void paintChar(RW3270Char c)
   {
      Graphics g = getGraphics();
      RW3270Field f = c.getField();
      int pos = c.getPosition();
      if(c.isStartField())
      {
         g.setColor(defaultBGColor);
         g.fillRect((pos%rw.getCols()) * x + (x + 5), (pos/rw.getCols()) * y + 6, x, y - 1);
         return;
      }
      byte ca[] = new byte[1];
      ca[0] = (byte)c.getDisplayChar();
      Color bgcolor = translateColor(f.getBackgroundColor());
      Color fgcolor = translateColor(f.getForegroundColor());
      boolean underscore = false;
      boolean hidden = false;
      switch(f.getHighlighting())
      {
      case RW3270Char.HL_REVERSE:
         fgcolor = bgcolor;
         bgcolor = translateColor(f.getForegroundColor());
         break;
      case RW3270Char.HL_UNDERSCORE:
         underscore = true;
         break;
      default:
         break;
      }
      /*if(f.isHidden())
         hidden = true;*/
      if(f.isBold())
      {
         if(fgcolor == defaultFGColor)
            fgcolor = boldColor;
      }
      g.setFont(font);
      //System.out.println("Print data..." + ca[0] + " pos: " + pos + " bgcolor: " + bgcolor);
      //we have to draw the background
      g.setColor(bgcolor);
     g.fillRect((pos%rw.getCols()) * x + (x + 5), (pos/rw.getCols()) * y + 6, x, y - 1);
      paintData(ca, 1, pos, g, bgcolor, fgcolor, underscore, hidden);
   }  
   /**
    * This translates the int colors stored in a Field Attribute into a java
    * Color object
    */
   private Color translateColor(int c)
   {
      switch(c)
      {
      case RW3270Field.DEFAULT_BGCOLOR:
         return defaultBGColor;
      case RW3270Field.BLUE:
         return Color.blue;
      case RW3270Field.RED:
         return Color.red;
      case RW3270Field.PINK:
         return Color.pink;
      case RW3270Field.GREEN:
         return Color.green;
      case RW3270Field.TURQUOISE:
         return Color.cyan;
      case RW3270Field.YELLOW:
         return Color.yellow;
      case RW3270Field.DEFAULT_FGCOLOR:
         return defaultFGColor;
      case RW3270Field.BLACK:
         return Color.black;
      case RW3270Field.DEEP_BLUE:
         return Color.blue;
      case RW3270Field.ORANGE:
         return Color.orange;
      case RW3270Field.PURPLE:
         return Color.blue;
      case RW3270Field.PALE_GREEN:
         return Color.green;
      case RW3270Field.PALE_TURQUOISE:
         return Color.cyan;
      case RW3270Field.GREY:
         return new Color(180, 180, 180);
      case RW3270Field.WHITE:
         return Color.white;
      }
      return Color.black;
   }
	public boolean keyDown(Event evt, int key) 
	{
		Graphics g = null;
		//System.out.println("Key: " + key);
		if ((g = getGraphics()) == null)
			return (true);
		g.setFont(font);
		if (messageOnScreen)
		{
			if (noMessage != MSG_BROADCAST)
				return (true);
			if(key == 27 && !tooManyConnections)//escape key
			{
				messageOnScreen = false;
				noMessage = -1;
				repaint();
				return (true);
			}
			return true;
		}
		synchronized(rw) 
		{
         switch (key)
         {
         case 3:					// <Ctrl-C>
         case Event.ESCAPE:
            rw.clear();
            return (true);
         case 10:				// <Return>
            rw.enter();
            return (true);
            /* 
               Function keys are called as x - 1 Where x is FX
               for example F1 = KeyPF(0), F2 = KeyPF(1), etc.
            */
         case Event.F1:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF13);
            else
               if (evt.modifiers == Event.META_MASK)
               rw.PA(RW3270.PA1);
            else
               rw.PF(RW3270.PF1);
            return (true);
         case Event.F2:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF14);
            else
               if (evt.modifiers == Event.META_MASK)
               rw.PA(RW3270.PA2);
            else
               rw.PF(RW3270.PF2);
            return (true);
         case Event.F3:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF15);
            else
               if (evt.modifiers == Event.META_MASK)
               rw.PA(RW3270.PA3);
            else
               rw.PF(RW3270.PF3);
            return (true);
         case Event.F4:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF16);
            else
               rw.PF(RW3270.PF4);
            return (true);
         case Event.F5:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF17);
            else
               rw.PF(RW3270.PF5);
            return (true);
         case Event.F6:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF18);
            else
               rw.PF(RW3270.PF6);
            return (true);
         case Event.F7:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF19);
            else
               rw.PF(RW3270.PF7);
            return (true);
         case Event.F8:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF20);
            else
               rw.PF(RW3270.PF8);
            return (true);
         case Event.F9:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF21);
            else
               rw.PF(RW3270.PF9);
            return (true);
         case Event.F10:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF22);
            else
               rw.PF(RW3270.PF10);
            return (true);
         case Event.F11:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF23);
            else
               rw.PF(RW3270.PF11);
            return (true);
         case Event.F12:
            if (evt.modifiers == Event.SHIFT_MASK)
               rw.PF(RW3270.PF24);
            else
               rw.PF(RW3270.PF12);
            return (true);
         }
         switch (key)
			{
         case 127:					// <Del>
         case 5:						// <Ctrl-E>
            try
            {
               rw.delete();
            }
            catch(IsProtectedException e){}
				return (true);
			case 9:						// <Tab>
				if (evt.modifiers == Event.SHIFT_MASK)	//Shift-Tab is a standard Win32 shortcut for backtab
				{
					rw.backTab();
					return (true);
				}
			case 6:						// <Ctrl-F>
				rw.tab();
				return (true);
         case 8:						// <BackSpace>
            try
            {
               rw.backspace();
            }
				catch(IsProtectedException e){}
				return (true);
			case Event.LEFT:
            rw.left();
				return (true);
			case Event.RIGHT:
				rw.right();
				return (true);
			case 13:					// <Ctrl-M>
				rw.keyFieldMark();
				return (true);
			case 14:					// <Ctrl-N>
				rw.keyNewLine();
				return (true);
			case Event.END:
				return (true);
      case 16:					// <Ctrl-P> (PRINT)
      case Event.PRINT_SCREEN:
      	print();
      	return (true);
			case Event.PGUP:
				rw.PA(rw.PA1);
				return (true);
			case Event.PGDN:
				rw.PA(rw.PA2);
				return (true);
/*            g = getGraphics();
				g.setFont(new Font("Helvetica", Font.PLAIN, 12));
				g.setColor(Color.red);
				g.drawString("Loading RightHost Keypad...", 5, status_y + 15);
				try
				{            
					 Class.forName("RightHostButtons");
				}
				catch(ClassNotFoundException e){splashScreen.getAppletContext().showStatus("Can't load RightHostButtons");}
				rhb = new RightHostButtons(this);
				showButtons = true;
				rhb.paint3270Buttons();
*/
			case Event.HOME:
				rw.home();
				return (true);
			case 18:					// <Ctrl-R>
				rw.reset();
				return (true);
			case 19:					//<Ctrl - S> SysReq
				rw.sysreq();
				return (true);
			case Event.UP:
				rw.up();
				return (true);
			case Event.DOWN:
				rw.down();
				return (true);
      default:
            try
            {
               //First, we'll see if the field is numeric, and if so, if the input
               //is numeric:
               if(rw.getField().isNumeric() && (key < 48 || key > 57))
               	return true;
               rw.type(key);
               if(rw.getChar(rw.getCursorPosition()).getField().isProtected()||rw.getChar(rw.getCursorPosition()).isStartField())
               {
                  int oldpos = rw.getCursorPosition();
                  rw.setCursorPosition(rw.getNextUnprotectedField(rw.getCursorPosition()));
                  cursorMove(oldpos, rw.getCursorPosition());
               }
            }
            catch(IsProtectedException e)
            {

            }
         }
		}
		return (true);
	}
   public void paintStatus(String msg)
   {
      Graphics g = getGraphics();
      if(g == null)
         return;
      g.setColor(Color.red);
      g.drawLine(x + 5, (rw.getRows()) * y + ascent, rw.getCols() * x + x + 5, (rw.getRows()) * y + ascent);
      g.setColor(defaultBGColor);
      g.fillRect(x + 5, (rw.getRows() + 1) * y, (rw.getCols()) * x, y);
      g.setColor(defaultFGColor);
      g.drawString(msg, x + 5, (rw.getRows()) * y + y + ascent);

   }
   public void print()
   {
      URL print = null;
      char c[] = rw.getDisplay();
      //char c[]= new char[chars.length];
      //for(int i = 0; i < chars.length; i++)
      //      c[i] = chars[i].getDisplayChar();
      StringBuffer scr = new StringBuffer(new String(c));
      for (int i = 0; i < rw.getRows(); i++)
         scr.insert((i*rw.getCols())+i, '\n');
      String urlEnc = URLEncoder.encode(scr.toString());
      try
      {
         print = new URL(parent.getCodeBase(), "Print?screen=" + urlEnc);
      }
      catch (MalformedURLException e)
      {
         
      }
      parent.getAppletContext().showDocument(print, "_new");
   }
   public void beep()
   {
   		Toolkit.getDefaultToolkit().beep();
   }
   public void status(int status)
   {
      switch(status)
      {
      case RWTnAction.X_WAIT:
         paintStatus("X-WAIT");
         break;
      case RWTnAction.READY:
         paintStatus("Ready");
         break;
      case RWTnAction.DISCONNECTED_BY_REMOTE_HOST:
      	 //System.out.println("status called...");
      	 noMessage = MSG_CLOSED_REMOTE;
      	 paintWindowMessage();
      	 	
      }
   }
   public void incomingData()
   {
      Graphics g = getGraphics();
      updateScreen(g);
      //repaint();
   }
   public void cursorMove(int oldPos, int newPos)
   {
      paintCursor(oldPos);
      paintChar(rw.getChar(oldPos));
      paintChar(rw.getChar(newPos));
      paintCursor(newPos);
   }
   public void paintCursor(int pos)
   {
      //System.out.println(rw.getCols() + " = " + rw.getCols());
      Graphics g = getGraphics();
      if(g == null)
         return;
      g.setFont(font);
      g.setColor(cursorColor);
      g.fillRect((pos%rw.getCols()) * x  + (x + 5), (pos/rw.getCols()) * y + 6, x, y - 1);
      g.setColor(Color.black);
      byte c[] = {(byte)rw.getChar(pos).getDisplayChar()};
      g.drawBytes(c, 0, 1, (pos%rw.getCols()) * x + (x + 5), (pos/rw.getCols()) * y + ascent + 5); 
   }
   private Font                     font;
   private int                      x, y, ascent;
   private RW3270                   rw;
   private Thread                   t;
   private int                      fontsize, cols, rows;
   private Color                    defaultFGColor, defaultBGColor, cursorColor, boldColor;
   private FontMetrics              fm;
   //private RW3270Char               chars[];
   private Applet                   parent;
   private int											noMessage;
   final static int MSG_CLOSED_REMOTE	= 0;
   final static int MSG_STRING				= 1;
   final static int MSG_BROADCAST			= 2;
   private String 	StrMessage;
   private boolean	messageOnScreen;
   private boolean 	tooManyConnections;
   
   
   
}

class RH3270Buttons extends Panel
{
   RH3270Buttons(RW3270 rw)
   {
      super();
      this.rw = rw;
      setLayout(new GridLayout(4, 8));
      add(new Button("PF1"));
      add(new Button("PF2"));
      add(new Button("PF3"));
      add(new Button("PF4"));
      add(new Button("PF5"));
      add(new Button("PF6"));
      add(new Button("PF7"));
      add(new Button("PF8"));
      add(new Button("PF9"));
      add(new Button("PF10"));
      add(new Button("PF11"));
      add(new Button("PF12"));
      add(new Button("PF13"));
      add(new Button("PF14"));
      add(new Button("PF15"));
      add(new Button("PF16"));
      add(new Button("PF17"));
      add(new Button("PF18"));
      add(new Button("PF19"));
      add(new Button("PF20"));
      add(new Button("PF21"));
      add(new Button("PF22"));
      add(new Button("PF23"));
      add(new Button("PF24"));
      add(new Button("PA1"));
      add(new Button("PA2"));
      add(new Button("PA3"));
      add(new Button("Enter"));
      add(new Button("Clear"));
      add(new Button("Reset"));
      add(new Button("Home"));
   }
   public boolean action(Event evt, Object arg)
   {
      if(evt.target instanceof Button)
      {
         if(arg.equals("Enter"))
            rw.enter();
         if(arg.equals("Clear"))
            rw.clear();
         if(arg.equals("Reset"))
            rw.reset();
         if(arg.equals("PF1"))
            rw.PF(RW3270.PF1);
         if(arg.equals("PF2"))
            rw.PF(RW3270.PF2);
         if(arg.equals("PF3"))
            rw.PF(RW3270.PF3);
         if(arg.equals("PF4"))
            rw.PF(RW3270.PF4);
         if(arg.equals("PF5"))
            rw.PF(RW3270.PF5);
         if(arg.equals("PF6"))
            rw.PF(RW3270.PF6);
         if(arg.equals("PF7"))
            rw.PF(RW3270.PF7);
         if(arg.equals("PF8"))
            rw.PF(RW3270.PF8);
         if(arg.equals("PF9"))
            rw.PF(RW3270.PF9);
         if(arg.equals("PF10"))
            rw.PF(RW3270.PF10);
         if(arg.equals("PF11"))
            rw.PF(RW3270.PF11);
         if(arg.equals("PF12"))
            rw.PF(RW3270.PF12);
         if(arg.equals("PF13"))
            rw.PF(RW3270.PF13);
         if(arg.equals("PF14"))
            rw.PF(RW3270.PF14);
         if(arg.equals("PF15"))
            rw.PF(RW3270.PF15);
         if(arg.equals("PF16"))
            rw.PF(RW3270.PF16);
         if(arg.equals("PF17"))
            rw.PF(RW3270.PF17);
         if(arg.equals("PF18"))
            rw.PF(RW3270.PF18);
         if(arg.equals("PF19"))
            rw.PF(RW3270.PF19);
         if(arg.equals("PF20"))
            rw.PF(RW3270.PF20);
         if(arg.equals("PF21"))
            rw.PF(RW3270.PF21);
         if(arg.equals("PF22"))
            rw.PF(RW3270.PF22);
         if(arg.equals("PF23"))
            rw.PF(RW3270.PF23);
         if(arg.equals("PF24"))
            rw.PF(RW3270.PF24);
         if(arg.equals("PA1"))
            rw.PA(RW3270.PA1);
         if(arg.equals("PA2"))
            rw.PA(RW3270.PA2);
         if(arg.equals("PA3"))
            rw.PA(RW3270.PA3);
      }

      return false;
   }
   RW3270 rw;
}
