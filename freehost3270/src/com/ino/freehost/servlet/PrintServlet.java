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
