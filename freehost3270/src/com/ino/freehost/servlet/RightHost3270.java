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


package com.ino.freehost.servlet;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.log4j.Category;
/**
 * The RightHost3270 class is the primary Servlet that fires off
 * the 'proxy' server that handles host to client connections.  It
 * also serves up the HTML for the java applet.
 */
public class RightHost3270 extends HttpServlet {

    private Category cat;
    private String rootpath;

    private String sessionServerHost;
    private int sessionServerPort;
    private String available;
    private boolean encryption;
    private boolean manualEntry;
    private Vector filterList;
    private int filterMode;
    private boolean loadBalancing;
    private String helpAbout;

    /**
     * Standard Servlet method that handles HTTP posts.
     * @param   req The request object.
     * @param   res The response object.
     *
     * @see     javax.servlet.http.HttpServletRequest
     * @see     javax.servlet.http.HttpServletResponse
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
    {

        try
	    {
		doGet(req, res);//to doGet
	    }
        catch(Exception e){}
    }
    /**
     * Standard servlet method for handling HTTP gets.
     * @param   req The request object
     * @param   res The response object
     *
     * @see     javax.servlet.http.HttpServletRequest
     * @see     javax.servlet.http.HttpServletResponse
     */
    public void doGet (HttpServletRequest req, HttpServletResponse res)
	throws IOException, ServletException
    {



	sessionServerPort = 6870;
	sessionServerHost = "localhost";
	encryption = false;
	manualEntry = false;
	available = "hollis.harvard.edu|23|HOLLIS";
	filterList = new Vector();
	filterMode = 0;
	loadBalancing = false;
	helpAbout = "Wecome to freehost 0.1!";


	ServletOutputStream out = res.getOutputStream();

	res.setContentType("text/html");

	try
	    {
		FileInputStream fis = new FileInputStream(rootpath + File.separator + "JavaHtml.htm");
		DataInputStream dis = new DataInputStream(fis);
		String line = dis.readLine();
		while(line != null)
		    {
			if(line.toLowerCase().indexOf("</body>") != -1)
			    {	
				//out.print(req.getHeader("User-Agent"));
				out.print(getAppletTag());
			    }
			else
			    out.print(line + "\n");
			line = dis.readLine();
		    }
		dis.close();
		fis.close();
	    }
	catch(Exception ee)
	    {
		cat.error(ee.getMessage());
		//a.error(ee.getMessage());
	    }
    }

    protected String getAppletTag()
    {
	StringBuffer sb = new StringBuffer();
    	sb.append("<applet codebase = ./Classes code=RH.class width=0 height=0>\n");
	sb.append("<param name=RightHostServer value=" + 
		  FreeHostConfiguration.getInstance().getSessionServerHost() + 
		  ">\n");
	sb.append("<param name=RightHostPort value=\"" + 
		  FreeHostConfiguration.getInstance().getSessionServerPort() + 
		  "\">\n");
	sb.append("<param name=available value=\"" + 
		  FreeHostConfiguration.getInstance().getHostList() + 
		  "\">\n");
	sb.append("<param name=manualEntry value=" + 
		  FreeHostConfiguration.getInstance().getManualEntry() + 
		  ">\n");
	sb.append("<param name=helpabout value=\"" +
		  escapeNewLine(FreeHostConfiguration.getInstance().getHelpAbout()) + 
		  "\">\n");
	sb.append("</applet>");
	sb.append("</body>\n");
	return new String(sb);
    }		
    /**
     * Method that escapes line break characters ('\n') so that they are passed unmarred 
     * in the applet parameter tags
     */
    public String escapeNewLine(String s)
    {
  	StringBuffer sb = new StringBuffer();
	int index = 0;
	int lastindex = 0;
	while((index = s.indexOf("\n", index)) != -1)
	    {
		sb.append(s.substring(lastindex, index) + "<nl>");
		lastindex = index;
		index += 2;
	    }
	return new String(sb);
    }
		
    /**
     * Overridden init method (from superclass HttpServlet).  Provides for the initialization
     * of the servlet.
     *
     * @param config passed by the calling server.
     */
    public void init(ServletConfig config)
	throws ServletException
    {
        super.init(config);
	cat = Category.getInstance("freehost3270.RightHost3270");
	rootpath = config.getServletContext().getRealPath("");
    }
    /**
     * Overridden destroy method (from superclass HttpServlet). Performs necessary
     * cleanup duties.
     */
    public void destroy()
    {
        super.destroy();
    }

}
