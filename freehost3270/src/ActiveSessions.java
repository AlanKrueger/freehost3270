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

import jclass.bwt.BWTEnum;
import jclass.bwt.JCMultiColumnList;
import jclass.bwt.JCTextArea;
import java.awt.*;
import java.applet.Applet;
import java.net.*;
import java.io.*;
import java.util.*;

public class ActiveSessions extends java.applet.Applet implements Runnable{


final static String[] column_labels = { 
	"Client", "Started", "Host"
};

final static int[] header_alignments = {
	BWTEnum.MIDDLELEFT, BWTEnum.MIDDLELEFT, BWTEnum.MIDDLELEFT  
};

final static int[] list_alignments = {
	BWTEnum.LEFT, BWTEnum.LEFT, BWTEnum.LEFT 
};


final int CMD_DELETEALL = 0;
final int CMD_DELETESELECTED = 1;
final int CMD_STOPSERVER = 2;
final int CMD_STARTSERVER = 3;
int command = -1;
Label status;
Button startStop;
public void init() 
    {
    	//System.out.println("Init called...");
    	list = new jclass.bwt.JCMultiColumnList();

    	// Force list to be same size as applet
    	setLayout(new BorderLayout());
    	setBackground(Color.lightGray);
    	list.useSystemColors(true);
    	list.getList().setBackground(Color.white);
    	//list.setVisibleRows(15);
    	//list.setSpacing(1);
    	//list.setRowHeight(15);
    	list.getList().setFont(new Font("Dialog", Font.PLAIN, 12));
    	list.setAllowMultipleSelections(true);
    	list.setSelectedBackground(new Color(0, 40, 86));
    	/*for (int i=0; i < data.length; i++) 
    		list.addItem(data[i], '|');*/
    	list.setColumnButtons(column_labels);
    	list.setColumnAlignments(list_alignments);
    	list.getHeader().setColumnAlignments(header_alignments);
    	add("Center", list);
    	Panel p = new Panel();
    	p.setLayout(new FlowLayout());
    	p.add(new Button("Kill Selected"));
    	p.add(new Button("Kill All"));
    	//p.add(new Button("Broadcast Selected"));
    	p.add(new Button("Broadcast All"));
    	p.add(startStop = new Button("STOP SessionServer"));
    	p.setBackground(Color.white);
    	add("South", p);
    	status = new Label();
    	status.setBackground(Color.white);
    	status.setForeground(Color.red);
    	add("North", status);
        
	}
    public boolean action(Event evt, Object arg)
    {
        if(evt.target instanceof Button)
        {
            if(arg.equals("Broadcast Selected"))
            {
                //BroadcastDialog bd = new BroadcastDialog();
            }
            if(arg.equals("Broadcast All"))
            {
            	try
            	{
                BroadcastDialog bd = new BroadcastDialog(new URL("http://" + getDocumentBase().getHost() + ":" + getDocumentBase().getPort()));
              }
              catch(Exception e){}
                
            }
            if(arg.equals("Kill Selected"))
            {
            			StringBuffer sb = new StringBuffer();
            			Object selected[] = list.getSelectedObjects();
            			for(int i = 0; i < selected.length; i++)
            			{
            				String str = selected[i].toString();
            				String str2 = str.substring(1, str.length() - 1);
            				sb.append(str2);
            				if(i != selected.length - 1)
            					sb.append("|");
            			}
            			String send = URLEncoder.encode(new String(sb));
            			System.out.println(send);
                	try
                	{
                		URL post = new URL("http://" + getDocumentBase().getHost() + ":" + getDocumentBase().getPort() + "/Admin?killselected=" + send);
                		InputStream tmp = post.openStream();
                		tmp.close();
                		tmp = null;
                		post = null;
                		System.gc();
                	}
                	catch(MalformedURLException e){}
                	catch(IOException e){}

            }
            if(arg.equals("Kill All"))
            {
                //Thread t = new Thread(this);
                //t.start();
                	System.out.println("Kill All");
                	try
                	{
                		URL post = new URL("http://" + getDocumentBase().getHost() + ":" + getDocumentBase().getPort() + "/Admin?killall=1");
                		InputStream tmp = post.openStream();
                		tmp.close();
                		tmp = null;
                		post = null;
                		System.gc();
                	}
                	catch(MalformedURLException e){System.out.println(e);}
                	catch(IOException e){
                		System.out.println(e);}                
            }
            if(arg.equals("STOP SessionServer"))
            {
                /*ruSureDialog rsd = new ruSureDialog();
                if(rsd.response == 0)
                {*/
                	try
                	{
                		URL post = new URL("http://" + getDocumentBase().getHost() + ":" + getDocumentBase().getPort() + "/Admin?stopserver=1");
                		InputStream tmp = post.openStream();
                		tmp.close();
                		tmp = null;
                		post = null;
                		System.gc();
                	}
                	catch(MalformedURLException e){}
                	catch(IOException e){}
               // }
            }
            if(arg.equals("START SessionServer"))
            {
                //status.setText("SessionServer is Running");
                	try
                	{
                		URL post = new URL("http://" + getDocumentBase().getHost() + ":" + getDocumentBase().getPort() + "/Admin?startserver=1");
                		InputStream tmp = post.openStream();
                		tmp.close();
                		tmp = null;
                		post = null;
                		System.gc();
                	}
                	catch(MalformedURLException e){}
                	catch(IOException e){}                
            }
        }
        return true;
    }
    public void start()
    {
    	/*System.out.println("Start called....");

    	//Get the hostname of the server*/
      host = getDocumentBase().getHost();
      port = getDocumentBase().getPort();
      /*
      try
      {
      	initHost = new URL("http://" + host + ":" + port + "/Admin?initialactivesessions=1");
      }
      catch(MalformedURLException e)
      {
      	System.out.println(e);
      }
			try
			{
      	URLConnection urlc = initHost.openConnection();
      	urlc.setUseCaches(false);				
				dis = new DataInputStream(urlc.getInputStream());
				String line = dis.readLine();
				while(line != null)
				{
					list.addItem(line, '|');
					System.out.println(line);					
					line = dis.readLine();
				}
				dis.close();
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
			finally
			{
				System.gc();
			} */
			if(t == null)
			{				
				t = new Thread(this);
    		t.start();   	
    	}
    }    	
    public void stop()
    {
				t.stop();
				t.destroy();
				dis = null;
		    host = null;
		    hostConn = null;
		    initHost = null;
		    list = null;
		    t = null;
 		    System.gc();
    }
    public void run()
    {
			//System.out.println("Thread Started...");
      while(true)
      {
	    	//Every few seconds, we're going to get an updated list
	    	//of commands from the server.  The command is pipe-delimited in
	    	//the following format:
	    	//   ADD/DELETE|client|started
	    	//We use client AND time started to insure that we only see the
	    	//actual session that has dropped off and not all sessions for
	    	//a particular client.
	      try
	      {
	      	initHost = new URL("http://" + host + ":" + port + "/Admin?initialactivesessions=1");
	      }
	      catch(MalformedURLException e)
	      {
	      	System.out.println(e);
	      }
		   	try
				{
		    	String updateline = null;
		    	URLConnection urlc = initHost.openConnection();
		    	urlc.setUseCaches(false);
		    	InputStream ins = urlc.getInputStream();
		    	dis = new DataInputStream(ins);
	      	boolean tmp = Boolean.valueOf(dis.readLine()).booleanValue(); 
	      	if(tmp != serverRunning)
	      	{
	      		serverRunning = tmp;
	      		if(serverRunning)
	      		{
	      			status.setText("SessionServer is Running.");
	      			startStop.setLabel("STOP SessionServer");
	      		}
	      		else
	      		{
	      			status.setText("SessionServer is Stopped.");
	      			startStop.setLabel("START SessionServer");
	      		}
	      	}
	      	Vector comp = new Vector();
	      	while((updateline = dis.readLine()) != null)
	      	{
	      		//Add the current line to the vector
	      		comp.addElement(updateline);
	      	}
	      	dis.close();
	      	ins.close();
	      	ins = null;
	      	//Now we have a vector representing the server's state
	      	//First we'll compare the existing list to the server's list
	      	//If the current list has a session that the server's list
	      	//doesn't, we need to delete it.
	      	
	      		//Get the existing list (from the applet)
    				String items[] = list.getItemsStrings();
    				//loop through each entry in the applet list
						for(int i = 0; i < items.length; i++)
						{
							//Get the entry
							String s = items[i];
							//System.out.println(s);
							//Get the server's list
							Enumeration e = comp.elements();
							//Flag variable that will flip to true if this session
							//matches a session on the server
							boolean match = false;
							//Iterate through each session from the server's list, checking
							//to see if it matches this one.
							//System.out.println("Enumerate server sessions...");
							while(comp.size() > 0 && e.hasMoreElements())
							{
								String t = (String)e.nextElement();
								//System.out.println(t);
								//System.out.println("got element...");
								//Parse the line into command/client/started
								StringTokenizer st = new StringTokenizer(t, "|");
								String client = st.nextToken();
								String started = st.nextToken();
								String host = st.nextToken();
								//System.out.println("Got tokens...");
								//System.out.println("Comparet to list sessions...");
								StringTokenizer st2 = new StringTokenizer(s, ",");
								if(client.equals(st2.nextToken()) && started.equals(st2.nextToken()))
								{	
									//If the session matches a new session, we don't need to keep
									//checking.
									match = true;
									break;
								}
							}
							//If there was no match, delete the item
							if(!match)
							{
								list.deleteItem(i--);
							}
						}
						//Now that we've checked for deletes, Iterate through all of the
						//Server's session's to see if there needs to be an add
						Enumeration e = comp.elements();
						//System.out.println("Enumerate server sessions..." + comp.size());
						while(e.hasMoreElements())
						{
								String t = (String)e.nextElement();
								//System.out.println(t);
								//Parse the line into command/client/started
								StringTokenizer st = new StringTokenizer(t, "|");
								String client = st.nextToken();
								String started = st.nextToken();
								String host = st.nextToken();
								boolean match = false;
								items = list.getItemsStrings();
								for(int i = 0; i < items.length; i++)
								{
									String s = items[i];
									//System.out.println("Compare to list sessions...");
									StringTokenizer st2 = new StringTokenizer(s, ",");
									if(client.equals(st2.nextToken()) && started.equals(st2.nextToken()))
									{
										match = true;
										break;
									}
								}
								if(!match)
									list.addItem(t, '|');
							}

	      	}
	      	catch(IOException e)
	      	{
	      		//System.out.println(e);
	      	}
	
	      	try
	      	{
	      		Thread.sleep(4000);
	      	}
	      	catch(InterruptedException e){//System.out.println(e);
	      	}
	     }
    }
    String host;
    URL hostConn, initHost;
    JCMultiColumnList list;
    Thread t;
    int port;
    DataInputStream dis;
    boolean serverRunning;
}

class BroadcastDialog extends Frame
{
    BroadcastDialog(URL host)
    {
        super("Broadcast Message");
    		this.host = host;
        setLayout(new BorderLayout());
        setBackground(Color.lightGray);
        message = new JCTextArea();
        message.setScrollbarDisplay(message.DISPLAY_VERTICAL_ONLY);
        message.setMultiline(false);
        message.setColumns(20);
        message.setBackground(Color.white);
        add("Center", message);
        Panel p = new Panel();
        p.setLayout(new FlowLayout());
        p.add(new Button("Broadcast"));
        add("South", p);
        //center us on the screen
        Dimension screen, dlg;
        screen = Toolkit.getDefaultToolkit().getScreenSize();
        resize(400, 200);
        dlg = this.size();
        move((screen.width - dlg.width)/2, (screen.height - dlg.height)/2);
        show();
    }
    
    public boolean action(Event evt, Object arg)
    {
        if(evt.target instanceof Button)
        {
        		String msg = URLEncoder.encode(message.getText());
            try
          	{
          		URL post = new URL(host, "/Admin?broadcast=" + msg);
          		InputStream tmp = post.openStream();
          		tmp.close();
          		tmp = null;
          		post = null;
          		System.gc();
          	}
          	catch(MalformedURLException e){System.out.println(e);}
          	catch(IOException e){
          		System.out.println(e);}   
            this.dispose();
        }
        if(evt.id == Event.WINDOW_DESTROY)
        	this.dispose();
        	
        return true;
    }
    public boolean keyDown(Event evt, int key)
    {
    	if(key == Event.ESCAPE)
    		this.dispose();
    	return true;
    }
    JCTextArea message;
    URL host;
}
        
class ruSureDialog extends Frame implements Runnable
{
    static int response = -1;
    ruSureDialog()
    {
        super();
        setBackground(Color.lightGray);
        setLayout(new BorderLayout());
        Label ru = new Label("Are you sure?", Label.CENTER);
        add("Center", ru);
        Panel p = new Panel();
        p.setLayout(new FlowLayout());
        p.add(new Button("OK"));
        p.add(new Button("Cancel"));
        add("South", p);
        Dimension screen, dlg;
        screen = Toolkit.getDefaultToolkit().getScreenSize();
        resize(200, 100);
        dlg = this.size();
        move((screen.width - dlg.width)/2, (screen.height - dlg.height)/2);        
        show();
        t = new Thread(this);
        t.start();
        try
        {
        	t.join();
        }
        catch(InterruptedException e){}
    }
    public void run()
    {
    	while(response < 0)
    	{
    		try
    		{
    			Thread.sleep(250);
    		}
    		catch(InterruptedException e){}
    	}
    	this.dispose();	
    }
    	
    public boolean action(Event evt, Object arg)
    {
        if(evt.target instanceof Button)
        {
            if(arg.equals("OK"))
            {
                System.out.println("OK clicked...");
                response = 0;
            }
            if(arg.equals("Cancel"))
            {
                response = 1;
            }
        }
        return true;
    }
    Thread t;
}
        
        
        
