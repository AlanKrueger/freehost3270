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

import javax.servlet.http.*;
import javax.servlet.*;
import com.ino.freehost.servlet.*;
import com.ino.freehost.client.Host;
import java.io.*;
import java.util.*;

public class html3270Servlet extends HttpServlet implements Runnable
{
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
	{
		doPost(req, res);
	}
	public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
	{
		SessionServer a = (SessionServer)getServletContext().getServlet("Admin");
		/*
		 * Manage the users session
		 */
     if(a.getFilterMode() > 0)
      {	
	      boolean flag = a.checkFilter(req);
	      if(!flag)
	      {
	         res.sendError(res.SC_FORBIDDEN);
	         return;
	      }
	    }
      if(!a.isRunning())
      {
      	res.sendError(res.SC_INTERNAL_SERVER_ERROR, "The SessionServer is not currently running.");
      	return;
      }


		if(req.getParameter("host") == null && req.getParameter("sessionID") == null)
		{
     
    	if(a.getLoadBalancing() && req.getParameter("loaded") == null)
    	{
    		String s = null;
    		try
    		{
    			s = a.getLeastLoaded();
    			//System.out.println(s);
    		}
    		catch(SessionServerNotRunningException e)
    		{
    			res.sendError(res.SC_INTERNAL_SERVER_ERROR, "The SessionServer is not Running.");
    			return;
    		}
    		if(s != null)
    		{
    				res.sendRedirect("http://" + s + "/HTML3270?loaded=1");
    				return;
    		}
			} 			
			Enumeration e = a.getHosts();
			int i = 0;
			while(e.hasMoreElements())
			{
				e.nextElement();
				i++;
			}
			ServletOutputStream out = res.getOutputStream();
			out.print(getNoHostHTML());
			return;
		}
		RWHtml3270 rw = null; 
		RWHTTPSession session = (RWHTTPSession)sessions.getSession(req.getParameter("sessionID"));
		if(session.isNew())
		{
		 //System.out.println("New Sessions");

			ServletOutputStream out = res.getOutputStream();
			res.setContentType("text/html");
			out.print("<HTML>\n");
			out.print("<BODY bgcolor=000000>\n");
			out.print("<FORM method=post>\n");
			out.print("<INPUT type=\"hidden\" name=\"sessionID\" value=\"" + session.getId() + "\">\n");
			out.print("<PRE>\n");			
			rw = new RWHtml3270(a.getSessionServerHost(), a.getSessionServerPort(),
													req.getParameter("host"), Integer.parseInt(req.getParameter("port")),
													a.getEncryption(), res);
			out.print("</PRE>\n");
			getButtons(out);
			out.print("</FORM>\n");
			out.print("</BODY>\n");
			out.print("</HTML>\n");
			session.putValue("3270Session", rw);
			session.isNew(false);
			return;
		}
		if(req.getParameter("Disconnect") != null)
		{
			res.sendRedirect("HTML3270");
			try
			{
				rw.process(req, res);
			}
			catch(NullPointerException e)
			{}
			return;
		}
		rw = (RWHtml3270)session.getValue("3270Session");
		session.setLastAccessedTime();	
		ServletOutputStream out = res.getOutputStream();
		res.setContentType("text/html");
		out.print("<HTML>\n");
		out.print("<BODY bgcolor=000000>\n");
		out.print("<FORM method=post>\n");
		out.print("<INPUT type=\"hidden\" name=\"sessionID\" value=\"" + session.getId() + "\">\n");
		out.print("<PRE>\n");
		rw.process(req, res);
		out.print("</PRE>\n");
		getButtons(out);
		out.print("</FORM>\n");
		out.print("</BODY>\n");
		out.print("</HTML>\n");
	}
	public void getButtons(ServletOutputStream out)
	throws IOException
	{
		out.print("<BR>");
    out.print("<INPUT type = submit name=AID value=Enter>");
    out.print("<INPUT type=submit name=AID value=Clear>");
    out.print("<INPUT type=submit name=AID value=Refresh>");
    out.println("<BR>");
    out.print("<INPUT type=submit name=AID value=PF1>");
    out.print("<INPUT type=submit name=AID value=PF2>");
    out.print("<INPUT type=submit name=AID value=PF3>");
    out.print("<INPUT type=submit name=AID value=PF4>");
    out.print("<INPUT type=submit name=AID value=PF5>");
    out.print("<INPUT type=submit name=AID value=PF6>");
    out.print("<INPUT type=submit name=AID value=PF7>");
    out.print("<INPUT type=submit name=AID value=PF8>");
    out.print("<INPUT type=submit name=AID value=PF9>");
    out.print("<INPUT type=submit name=AID value=PF10>");
    out.print("<INPUT type=submit name=AID value=PF11>");
    out.println("<INPUT type=submit name=AID value=PF12>");
    out.println("<BR>");
    out.print("<INPUT type=submit name=AID value=PF13>");
    out.print("<INPUT type=submit name=AID value=PF14>");
    out.print("<INPUT type=submit name=AID value=PF15>");
    out.print("<INPUT type=submit name=AID value=PF16>");
    out.print("<INPUT type=submit name=AID value=PF17>");
    out.print("<INPUT type=submit name=AID value=PF18>");
    out.print("<INPUT type=submit name=AID value=PF19>");
    out.print("<INPUT type=submit name=AID value=PF20>");
    out.print("<INPUT type=submit name=AID value=PF21>");
    out.print("<INPUT type=submit name=AID value=PF22>");
    out.print("<INPUT type=submit name=AID value=PF23>");
    out.println("<INPUT type=submit name=AID value=PF24>");
    out.println("<BR>");
    out.print("<INPUT type=submit name=AID value=PA1>");
    out.print("<INPUT type=submit name=AID value=PA2>");    
    out.print("<INPUT type=submit name=AID value=PA3>");    
    out.print("<INPUT type=submit name=Disconnect value=Disconnect>");				
  }
  protected String getNoHostHTML()
  throws ServletException
  {
  	StringBuffer sb = new StringBuffer();
  	SessionServer a = (SessionServer)getServletContext().getServlet("Admin");
  	try
    {
    	
    	FileInputStream fis = new FileInputStream(a.getPropsDirectory() + File.separator + "html3270.htm");
    	DataInputStream dis = new DataInputStream(fis);
    	String line = dis.readLine();
    	while(line != null)
    	{
    		if(line.toLowerCase().indexOf("</body>") != -1)
    		{	
    			//out.print(req.getHeader("User-Agent"));
			  	sb.append("<CENTER>");
			  	a = (SessionServer)getServletContext().getServlet("Admin");
					Enumeration e = a.getHosts();
					while(e.hasMoreElements())
					{
						Host h = (Host)e.nextElement();
						sb.append("<a href=HTML3270?host=" + h.hostName + "&port=" + h.port + "><font face=\"arial, helvetica\">" + h.friendlyName + "</font></a><BR>\n");
					}
					sb.append("</BODY>\n");    		
				}
    		else
    			sb.append(line + "\n");
    		line = dis.readLine();
    	}
    	dis.close();
    	fis.close();
    }
    catch(Exception ee)
    {
    	a.error(ee.getMessage());
    }		
		return sb.toString();
	}
	protected String getOneHostHTML()
	throws ServletException
	{
  	StringBuffer sb = new StringBuffer();
  	sb.append("<HTML>\n");
  	sb.append("<HEAD>\n");
  	sb.append("<TITLE>RightHost 3270 HTML</TITLE>\n");
  	sb.append("</HEAD>\n");
  	sb.append("<BODY>\n");
		SessionServer a = (SessionServer)getServletContext().getServlet("Admin");		
		Enumeration e = a.getHosts();
		Host h = (Host)e.nextElement();
		sb.append("<a href=HTML3270?host=" + h.hostName + "&port=" + h.port + "><font face=\"arial, helvetica\">" + h.friendlyName + "</font></a><BR>\n");
		sb.append("</BODY>\n");
		sb.append("</HTML>\n");
		return sb.toString();
	}		
	public void init(ServletConfig c)
	throws ServletException
	{
		super.init(c);
		Thread t = new Thread(this);
		t.start();
		sessions = new RWHTTPSessionContext();
	}
	/*
	 * We need some way to kill sessions after a certain amount of inactivity
	 * every 30 seconds, all the sessions are checked, and those that have 10 
	 * minutes of inactivity are killed.
	 */
	public void run()
	{
		while(true)
		{
			try
			{
				Thread.sleep(30000);
			}
			catch(InterruptedException e){}
			Enumeration e = sessions.getIds();
			while(e.hasMoreElements())
			{
				String key = (String)e.nextElement();
				RWHTTPSession sess = (RWHTTPSession)sessions.getSession(key);
				if(System.currentTimeMillis() - sess.getLastAccessedTime() > 600000)
					sess.invalidate();
		  }
		}
	}
	private RWHTTPSessionContext sessions;
}
		
