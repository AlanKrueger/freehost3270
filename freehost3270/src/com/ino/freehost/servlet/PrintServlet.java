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

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * This subclass of HttpServlet implements a servlet that simply
 * takes an inputStream of pre-formatted text and outputs it to the
 * output stream.  It's a kludge designed to allow for printing on
 * 1.02 VMs... the client opens a new browser window and posts the current
 * screen to this servlet, which sends it back to the new window as text.
 * The user can then use the browser's print function to print the screen.
 * Kludgeville, folks.
 */
public class PrintServlet extends HttpServlet
{

    public void doGet (HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
    {
        //System.out.println("got to doGet");
        doPost(req, res);
    }

    public void doPost (HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
      	res.setContentType("text/html");
	    ServletOutputStream out = res.getOutputStream();
	    //System.out.println("Sending HTML...");
	    out.println("<HTML>");
	    out.println("<HEAD>");
	    out.println("\t<TITLE>");
	    out.println("\t\tRightHost HTML 3270 Emulator");
	    out.println("\t</TITLE>");
	    out.println("</HEAD>");
	    out.println("<BODY bgcolor=FFFFFF>");
	    out.println("<CENTER><font face = arial>Press your Browser's Print Button</font></CENTER><P><PRE>");
	    out.println(req.getParameter("screen"));
	    out.println("</PRE></BODY>");
	    out.println("</HTML>");
	}
}
