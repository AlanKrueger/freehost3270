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
import java.math.*;
import com.ino.freehost.client.*;
import org.apache.log4j.Category;
import org.apache.log4j.BasicConfigurator;

public class SessionServer extends HttpServlet 
{
    private Category cat;
    /**
     * This servlet's context object.
     */

    ServletContext sc               = null;
    /**
     * The default RightHost servlet configuration (host, port, etc.)
     */
    ServletConfig rightHost         = null;
    /**
     * The RightHost3270 object/servlet/service.
     */
    RightHost3270 rh3270            = null;
    /**
     * The default port for accepting client connections.
     */
    String  rightHostPort           = null;
    /**
     * The default host for tn3270 sessions.
     */
    String  rightHostHost           = null;
    /** 
     * The default host's port
     */
    String  rightHostHostPort       = null;
    /** 
     * The default setting for encryption on or off (t/f)
     */
    String  rightHostEncryption     = null;
    /**
     * Admin's user name
     */
    private String adminName;
    /**
     * Admin's password
     */
    private String adminPass;
    /**
     * Admin's email notification address
     */
    private String adminEmail;
    /**
     * SMTP Server used for sending notification emails
     */
    private String smtpserver;
    /** 
     * Should the server notify the admin and when
     */
    protected boolean emailNotifyNotResponding;
    protected boolean emailNotifyHostNotResponding;
    protected boolean emailNotifyXSessionsAreActive;
    protected int     emailNotifyXSessions;
    /**
     * Logfile
     */
    private File logFile;
    protected boolean logAdminLogin;
    protected boolean logBroadcastMessage;
    protected boolean logKilledClient;
    protected boolean logSessionStart;
    protected boolean logSessionEnd;
    protected boolean logServerErrors;
    /**
     * SessionServer variables
     */
    //the hostname/ip address of the sessionserver.  This
    //value is used in the <applet> tag on the html that
    //serves up a client.
    private String sessionServerHost;
    private int sessionServerPort;
    //indicates whether loadbalancing is on or off
    private boolean loadBalancing;
    //vector of loadbalancing hosts
    private Vector loadBalancingServers;
    //indicates whether loadbalancing is on or off
    private boolean encryption;
    //the ip filter mode
    private int filterMode;//0 = none; 1 = allExcept; 2 = only
    //the list of hosts to filter
    private Vector filterList;
    //the list of 3270 hosts...
    //TO DO:  Create a class for this Friendly Name, Host Name, port, isDefault, etc.
    private Vector hostList;
    //indicates whether the client can manually enter hosts to access
    //this value is used in the <applet> tag on the html that
    //serves up the client
    private boolean manualEntry;
    //the directory in which the admin.properties
    //file can be found.
    public static File propsDirectory;
    //If we're running with a RightHost HTTP server
    //as our context;
    private String helpabout;
    private boolean isRightHostHTTP;
    private int httpPort;
    private File adminFile;
    private Properties config;
    private Proxy p;
    private Thread process;
    private boolean serverRunning;
    private int connections;
    private Date expirationDate;
    private int encryptionType;
    private int licenseType;
    public String getPropsDirectory()
    {
	return adminFile.getParent();
    }
    /**
     * This overrides the Servlet.init() method and initializes all of our
     * property values from the admin.properties file.
     */
    public void init(ServletConfig c)
	throws ServletException
    {
	//System.out.println("SessionServer loading");
	super.init(c);
	BasicConfigurator.configure();
	cat = Category.getInstance("freehost3270.SessionServer");
	cat.info("Initializing freehost3270");
	String rootpath = c.getServletContext().getRealPath("");
	cat.debug("Grabbing File: " + rootpath + File.separator + c.getInitParameter("freehost.properties"));
	adminFile = new File(rootpath + File.separator + c.getInitParameter("freehost.properties"));
	cat.info("Configuring freehost3270 with " + c.getInitParameter("freehost.properties"));
	//Now that we have the directory, we'll load in the admin.properties
	//file.
	FileInputStream fis = null;
	try
	    {
		fis = new FileInputStream(adminFile);
		config = new Properties();
		config.load(fis);
		fis.close();
	    }
	catch(Exception e)
	    {
		System.out.println("Cannot find admin.properties file.  Please ensure that it has not been deleted." + e.getMessage());
		error("Could not find admin.properties file at " + adminFile + ". Please ensure that it has not been deleted.");

	    }


	adminName = config.getProperty("username");
	adminPass = config.getProperty("password");
	adminEmail = config.getProperty("adminEmail");
	smtpserver = config.getProperty("smtpserver");
	//TO DO: Admin emailNotify and logging:
	logAdminLogin = Boolean.valueOf(config.getProperty("logAdminLogin")).booleanValue();
	logBroadcastMessage = Boolean.valueOf(config.getProperty("logBroadcastMessage")).booleanValue();
	logKilledClient = Boolean.valueOf(config.getProperty("logKilledClient")).booleanValue();
	logSessionStart = Boolean.valueOf(config.getProperty("logSessionStart")).booleanValue();
	logSessionEnd = Boolean.valueOf(config.getProperty("logSessionEnd")).booleanValue();
	logServerErrors = Boolean.valueOf(config.getProperty("logServerErrors")).booleanValue();
	emailNotifyHostNotResponding = Boolean.valueOf(config.getProperty("emailNotifyHostNotResponding")).booleanValue();
	emailNotifyXSessionsAreActive = Boolean.valueOf(config.getProperty("emailNotifyXSessionsAreActive")).booleanValue();
	emailNotifyXSessions = Integer.parseInt(config.getProperty("emailNotifyXSessions"));
	logFile = new File(config.getProperty("logFile"));
	sessionServerHost = config.getProperty("sessionServerHost");
	sessionServerPort = Integer.parseInt(config.getProperty("sessionServerPort"));
	loadBalancing = Boolean.valueOf(config.getProperty("loadBalancing")).booleanValue();
	loadBalancingServers = getLoadBalancingServers(config.getProperty("loadBalancingServers"));
	encryption = Boolean.valueOf(config.getProperty("encryption")).booleanValue();
	filterMode = Integer.parseInt(config.getProperty("filterMode"));
	filterList = getFilterList(config.getProperty("filterList"));
	hostList = getHostList(config.getProperty("hostList"));
	manualEntry = Boolean.valueOf(config.getProperty("manualEntry")).booleanValue();
	if(getServletContext().getServerInfo().equals("RightWare SessionServer HTTP Server"))
	    isRightHostHTTP = true;
	else
	    isRightHostHTTP = false;
	if(isRightHostHTTP)
	    {
		try
		    {
			FileInputStream fis2 = new FileInputStream("props" + File.separator + "server.properties");
			Properties p = new Properties();
			p.load(fis2);
			fis2.close();
			httpPort = Integer.parseInt(p.getProperty("server.port"));
		    }
		catch(Exception e)
		    {
			error(e.getMessage());
		    }
	    }
	activeSessionMessages = new Vector();
	helpabout = config.getProperty("helpabout");
	startServer();
	serverRunning = true;
	//sendmail("The SessionServer was started at " + new Date());
    }
    private Vector getLoadBalancingServers(String in)
    {
	Vector ret = new Vector();
	if(in == null)
	    return ret;
	StringTokenizer st = new StringTokenizer(in, "|");
	while(st.hasMoreTokens())
	    {
		ret.addElement(st.nextToken());
	    }
	return ret;
    }
    private Vector getFilterList(String in)
    {
	Vector ret = new Vector();
	if(in == null)
	    return ret;
	StringTokenizer st = new StringTokenizer(in, "|");
	while(st.hasMoreTokens())
	    {
		ret.addElement(new RWFilterAddress(st.nextToken()));
	    }
	return ret;
    }
    public int getFilterMode()
    {
	return filterMode;
    }
    public Vector getFilterList()
    {
	return filterList;
    }
    private Vector getHostList(String hostList)
    {
	Vector ret = new Vector();
	StringTokenizer st2 = new StringTokenizer(hostList, "|");
	while(st2.hasMoreTokens())
	    {
		String hostName = st2.nextToken();
		int hostPort = Integer.parseInt(st2.nextToken());
		String friendlyName = st2.nextToken();
		Host h = new Host(hostName, hostPort, friendlyName);
		ret.addElement(h);
	    }
	return ret;
    }
    public String getHostList()
    {
	return config.getProperty("hostList");
    }
    public Enumeration getHosts()
    {
    	return hostList.elements();
    }
    public boolean getManualEntry()
    {
	return manualEntry;
    }
    public boolean getEncryption()
    {
	return encryption;
    }
    public String getSessionServerHost()
    {
	return sessionServerHost;
    }
    public int getSessionServerPort()
    {
	return sessionServerPort;
    }
    public boolean moreConnections()
    {
    	if(connections == 0)
	    return true;
    	if(p.connections.size() >= connections)
	    return false;
    	return true;
    }

    /**
     * Handles HTTP Post
     * @param HttpServletRequest  the HTTP request object.
     * @param HttpServletResponse the HTTP response object.
     * @exception ServletException
     * @exception IOException
     * @see javax.servlet.http.HttpServletRequest
     * @see javax.servlet.http.HttpServletResponse
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
        if(loadBalancing)
	    {
	        if(req.getParameter("load") != null)
		    {
	        	res.getOutputStream().print(p.connections.size());
		    }
	    }
        doGet(req, res);
    }
    /** 
     * Handles an HTTP get request.
     * @param req The HTTP request object.
     * @param res The HTTP response object.
     * @exception Servlet Exception
     * @exception IOException
     */
    public void doGet (HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
	/*
	 * For load balancing
	 */	
	if(req.getParameter("load") != null)
	    {
		res.setContentType("text/html");
		res.getOutputStream().write((byte)p.connections.size());
		return;
	    }
	/*
	 * Returns a list of current sessions for the Admin Applet
	 */
	if(req.getParameter("activesessions") != null)
	    {
		res.setContentType("text/html");
		res.getOutputStream().print(getCurrentlyLoggedIn());
		return;
	    }

	/*
	 * Stops the Session Server... used by the Admin Applet
	 */
	if(req.getParameter("stopserver") != null)
	    {
		stopServer();
	    }
	/*
	 * Starts the Session Server... used by the Admin Applet
	 */
	if(req.getParameter("startserver") != null)
	    {
		startServer();
	    }
	/*
	 * Kills all active session... used by the Admin Applet
	 */
	if(req.getParameter("killall") != null)
	    {
		if(!serverRunning)
		    return;
		Enumeration e = p.connections.elements();
		while(e.hasMoreElements())
		    {
			Connection c = (Connection)e.nextElement();
			c.kill();
		    }
	    }
	/*
	 * Kills the selected session passed in by the Admin Applet
	 */
	if(req.getParameter("killselected") != null)
	    {
		if(!serverRunning)
		    return;
		String selected = req.getParameter("killselected");
		StringTokenizer st = new StringTokenizer(selected, "|");
		while(st.hasMoreTokens())
		    {
			String killsession = st.nextToken();
			//System.out.println(killsession);
			StringTokenizer st2 = new StringTokenizer(killsession, ",");
			while(st2.hasMoreTokens())
			    {
				String client = st2.nextToken().trim();
				String started = st2.nextToken().trim();
				String host = st2.nextToken().trim();
				//System.out.println("Host:" + host + " Client:"+client+" Started:"+started);
				Enumeration e = p.connections.elements();
				while(e.hasMoreElements())
				    {
					Connection c = (Connection)e.nextElement();
    				//System.out.println("Host:" + c.getDestHost() + " Client:"+c.getSrcHost()+" Started:"+c.sessionStarted);
					if(client.equals(c.getSrcHost().trim()) && started.equals(c.sessionStarted.toString().trim()))
					    {
						//System.out.println("Match...");	
						c.kill();
					    }
				    }
			    }
		    }
	    }
	/*
	 * Sends a broadcast message to all current clients
	 */
	if(req.getParameter("broadcast") != null)
	    {
		//System.out.println("Broadcast message...");
		broadcastMessage(req.getParameter("broadcast"));
		return;
	    }
	/*
	 * We authorize the Admin here using standard HTTP authentication
	 */
	boolean authorized = true;
	String auth = req.getHeader("authorization");
	ServletOutputStream out = res.getOutputStream();
	/*
	 * If there's no authorization header provided with the request, send
	 * back the 401 code
	 */    
	if(auth == null)
	    {
		res.setStatus(res.SC_UNAUTHORIZED);
		return;
	    }
	else
	    {
		//trim off the auth-type BASIC xxxxx:xxxxx
		auth = auth.substring(auth.indexOf(' ') + 1);
       
		//decode the remaining data xxxxx:xxxxx
		//auth = b64.processData(auth);
		if(auth.equals(":"))
		    {
			res.sendError(res.SC_UNAUTHORIZED, "You don't have access to this resource");
			return;
		    }
		//get the user and pass tokens (separated by :)
		StringTokenizer st = new StringTokenizer(auth, ":");
		String user = st.nextToken();
		String pass = st.nextToken();
		if(!user.equals(adminName) || !pass.equals(adminPass))
		    {
			res.sendError(res.SC_UNAUTHORIZED, "You don't have access to this resource");
			return;
		    }
	    }
	if(!authorized)
	    {
		res.sendError(res.SC_FORBIDDEN, "You don't have access to this resource.");
		return;
	    }
	res.setContentType("text/html");
	/*
	 * The next long block of code tests the incoming URL parameters and fires off
	 * The appropriate methods.  The "action" parameter is used to denote what
	 * needs to be performed.  Additional parameters are identified for different
	 * functions of the Admin interface.
	 */
	String action = req.getParameter("action");
	if(action != null && !action.equals(""))
	    {
	    }

    }
    public boolean isRunning()
    {
  	return serverRunning;
    }
    public boolean checkFilter(ServletRequest req)
    {
	RWFilterAddress client = new RWFilterAddress(req.getRemoteAddr());
	Enumeration e = filterList.elements();
	boolean flag = false;
	while(e.hasMoreElements())
	    {
		RWFilterAddress tmp = (RWFilterAddress)e.nextElement();
		if(filterMode == 2)
		    {
			if(!tmp.equals(client))
			    continue;
			else
			    {
				flag = true;
				break;
			    }
		    }
		if(filterMode == 1)
		    {
			if(tmp.equals(client))
			    continue;
			else
			    {
				flag = true;
				break;
			    }
		    }
	    }
	return flag;
    }  	 
    /**
     * This method will return the hostname of the least loaded
     * loaded server from the loadbalancing Vector, or null if the
     * current SessionServer is the least loaded, or if loadBalancing
     * is not enabled.
     * @return Name of the least loaded server
     */
    public String getLeastLoaded()
	throws SessionServerNotRunningException
    {
	if(!serverRunning)
	    throw new SessionServerNotRunningException();
	//if we're not loadBalancing, then the
	//client shouldn't have called this method
	if(!loadBalancing)
	    return null;
	String ret = null;
	Enumeration e = loadBalancingServers.elements();
	while(e.hasMoreElements())
	    {
			  
		int clients = 0;
		if(p.connections != null)
		    clients = p.connections.size();
		String h = (String)e.nextElement();
		String u = "http://" + h + "/Admin?load=1";
		//System.out.println(u);
		try
		    {
			URL url = new URL(u);
			URLConnection c = url.openConnection();
			if(c.getContent() instanceof BufferedInputStream)
			    {
				BufferedInputStream bis = (BufferedInputStream)c.getContent();
				int b = bis.read();
				if(b < clients)
				    {
					clients = b;
					ret = h;
				    }
			    }
		    }
		catch(MalformedURLException ee){continue;}
		catch(IOException ee){continue;}
	    }
	return ret;  	
    }
    public boolean getLoadBalancing()
    {
	return loadBalancing;
    }
    public void restartServer()
    {
	stopServer();
	startServer();
    }
    public void stopServer()
    {
	process.stop();
	serverRunning = false;
	try
	    {
		p.mainSocket.close();
	    }
	catch(IOException e)
	    {}
	p.mainSocket = null;
				//Remove all the connections from the sessionServer
	Enumeration e = p.connections.elements();
	while(e.hasMoreElements())
	    ((Connection)e.nextElement()).kill();
	p = null;
	System.gc();
    }
    public void startServer()
    {
	try
	    {
		//Initialize and start the proxy server.
		p = new Proxy(sessionServerPort, encryption, this);
		process = new Thread(p);
		process.start();
		serverRunning = true;
	    }
	catch(IOException e)
	    {
		error("ERROR: While trying to start the SessionServer, the following error was encountered: " + e.getMessage());	
	    }
    }
    /**
     * This method sends a message down the tn3270 stream using a special header
     * that is ignored by 3270.  Implementations can vary how they handle this response
     * which fires the broadcastMessage method in the 3270 interface.
     *
     * @param The message to send to clients
     */
    public void broadcastMessage(String message)
    {
	Enumeration e = p.connections.elements();
	String broadcast = message;
	while(e.hasMoreElements())
	    {
		Connection c = (Connection) e.nextElement();        
		byte buffer[] = new byte[1028];
		//the sequence of 0xFF & 0xF5 tells the engine
		//that the bytes that follow are a message.
		buffer[0] = (byte)0xFF;
		buffer[1] = (byte)0xF5;
		char msg[] = broadcast.toCharArray();
		for(int i = 0; i < msg.length; i++)
		    {
			buffer[i+2] = (byte)msg[i];
		    }
		try
		    {
			synchronized(c.fromDestToSrc)
			    {
				c.fromDestToSrc.out.write(buffer, 0, msg.length + 2);
				c.fromDestToSrc.out.flush();
			    }
		    }
		catch(IOException ex)
		    {
			error(ex.getMessage());
		    }
	    }
    }
    private boolean checkIPAddress(String s)
    {
	StringTokenizer st = new StringTokenizer(s, ".");
	//only four bytes
	if(st.countTokens() > 4)
	    return false;
	while(st.hasMoreTokens())
	    {
   	  	String t = st.nextToken();
   	  	if(t.equals("*"))
		    continue;
   	  	try
		    {
   	  		int i = Integer.parseInt(t);
   	  		if(i > 255 || i < 0)
			    return false;   	  	
		    }
   	  	catch(Exception e)
		    {
   	  		return false;
		    }

	    }
	return true;
    }		
    protected String getInitialLoggedIn()
    {
	StringBuffer sb = new StringBuffer();
	sb.append(serverRunning + "\n");
	if(p == null)
	    return new String(sb);
	Enumeration e = p.connections.elements();   		
	while(e.hasMoreElements())
	    {
		Connection c = (Connection)e.nextElement();
		sb.append(c.getSrcHost() + "|" + c.sessionStarted + "|" + c.getDestHost() + "\n");
	    }
	activeSessionMessages.removeAllElements(); 
	return new String(sb);  				
    }   	
    protected String getCurrentlyLoggedIn()
    {
	StringBuffer sb = new StringBuffer();
	sb.append(serverRunning + "\n");
	Enumeration e = activeSessionMessages.elements();
	while(e.hasMoreElements())
	    {
		String s = (String)e.nextElement();
		sb.append(s + "\n");
	    }
	activeSessionMessages.removeAllElements(); 
	return new String(sb);  				
    }
    protected void setActiveSessionMessage(String s)
    {
	activeSessionMessages.addElement(s);
    }
    public void log(int code, String msg, Connection c)
    {
	FileWriter logOutput = null;
	Date now = new Date();
	try
	    {
		logOutput = new FileWriter(logFile.toString(), true);
	    }
	catch(IOException e)
	    {
		debug(e.getMessage());
	    }
	if(code >= 200)
	    {
		switch(code)
		    {
		    case SS_GENERAL_ERROR:
			break;
		    }
	    }
	else if(code >= 100 && code < 200)
	    {
		//information
		switch(code)
		    {
		    case SS_CONNECT:
			try
			    {
				logOutput.write(now + "\t" + getSessionServerHost() + "\t" + "session" + "\t" + "101" + "\t" + "Connect" + "\t" + c.getSrcHost() + "\t" + c.getDestHost() + "\t" + c.getDestPort() + "\t" + msg + "\n");
			    }
			catch(IOException e){}
			break;
		    case SS_DISCONNECT:
			//disconnect
			try
			    {
				logOutput.write(now + "\t" + getSessionServerHost() + "\t" + "session" + "\t" + "101" + "\t" + "Disconnect" + "\t" + c.getSrcHost() + "\t" + c.getDestHost() + "\t" + c.getDestPort() + "\t" + msg + "\n");
			    }
			catch(IOException e){}
			break;
		    }
	    }
	else
	    {
		//debug
	    }
	try
	    {
		logOutput.close();
	    }
	catch(IOException e)
	    {
		debug(e.getMessage());
	    }
    }
    public void error(String msg)
    {
	if(logServerErrors)
	    log(SS_GENERAL_ERROR, msg, null);
    }
    public void debug(String msg)
    {	
	log(SS_GENERAL_DEBUG, msg, null);
    }
    /**
     * This method sends mail to the administrator at the
     * address specified in the admin interface.
     * @param msg The message to be sent
     */
    public void sendmail(String msg)
    {
	try
	    {
		Socket smtp = new Socket(smtpserver, 25);
		OutputStream os = smtp.getOutputStream();
		PrintStream ps = new PrintStream(os);
		InputStream is = smtp.getInputStream();
		DataInputStream mailDis = new DataInputStream(is);
		ps.println("HELO " + getSessionServerHost());
		ps.flush();
		mailDis.readLine();
		ps.println("MAIL FROM: \"RightHost SessionServer\" <righthost@" + sessionServerHost + ">");
		ps.flush();
		mailDis.readLine();
		ps.println("RCPT TO: " + adminEmail);
		ps.flush();
		mailDis.readLine();
		ps.println("DATA");
		ps.flush();
		mailDis.readLine();
		ps.println("SUBJECT: RightWare SessionServer Error Notification");
		ps.flush();
		mailDis.readLine();
		ps.println(msg + "\r\n.\r\n");
		mailDis.readLine();
		smtp.close();
	    }
	catch(UnknownHostException e)
	    {
	  	error(e.getMessage());
	    }
	catch(IOException e)
	    {
		error(e.getMessage());
	    }		
	System.gc();
    }
    public final static int SS_CONNECT = 101;
    public final static int SS_DISCONNECT = 102;
    public final static int SS_GENERAL_ERROR = 200;
    public final static int SS_GENERAL_DEBUG = 0;
	
    private Vector activeSessionMessages;
}
/**
 * This class represents an application 3270 host
 */
 
/**
 * This class provides the mechanism by which host and client are able to communicate.
 * Essentially a proxy, this class handles encryption/decryption of the client stream,
 * and thread and connection management.
 */
class Proxy implements ConnectionMonitor, Runnable
{

    Category cat;
    /**
     * The socket used for listening for client requests.
     */
    protected ServerSocket mainSocket = null;
    /**
     * The vector of active connections.
     */
    protected Vector connections     = null;
    /**
     * The host 
     */
    String host                      = null;
    /** 
     * The port
     */
    int hostPort;
    /**
     * Encryption true/false.
     */
    boolean encrypt;

    /**
     * Public constructor... called from RightHost3270 servlet upon initialization.
     * @param    portNumber integer representing the port on which to listen for clients.
     * @param    hostAddr    string representing the default host's hostname or ip address.
     * @param    hostP       integer representing the default host's port number.
     * @param    encryption  default encryption on or off(t/f).
     */
    public Proxy(int portNumber, boolean encryption, SessionServer ss) throws IOException
    {
	this.ss = ss;
	connections = new Vector();
	mainSocket  = new ServerSocket(portNumber);
	encrypt = encryption;
	cat = Category.getInstance("freehost3270.Proxy");
    }

    /**
     * Start the thread.  This is designed to use up as little overhead as possible
     * by spawining the Connection object (and it's corresponding threads) immeadiately.
     */
    public void run()
    {
	while(true)
	    {
		try
		    {
			Socket newSocket = mainSocket.accept();
			cat.debug("New connection...");
			new Connection(newSocket, (ConnectionMonitor)this, host, hostPort, encrypt, ss);
			cat.debug("Connection successful...");
			//System.gc();
		    }
		catch(IOException e)
		    {
             		e.printStackTrace();
             		ss.error(e.getMessage());	
		    }
	    }
    }

    /**
     * Empty method.  Satisfies implementation of the Connection Monitor interface.
     */
    public void attemptingConnection(Connection c)
    {
    }

    /**
     * Adds a Connection object to the connections Vector.
     * @param    c The connection object to add.
     */
    public void addConnection(Connection c)
    {
	synchronized(connections)
	    {
		connections.addElement(c);
	    }
	if(ss.logSessionStart)
	    ss.log(ss.SS_CONNECT, "", c);
    }
    /**
     * Removes a Connection object from the connections Vector.
     * @param c The connection object to remove.
     */
    public void removeConnection(Connection c)
    {
	synchronized(connections)
	    {
		connections.removeElement(c);
	    }
	if(ss.logSessionEnd)
	    ss.log(ss.SS_DISCONNECT, "", c);
    }
    /**
     * Empty Method. TO DO:  This should probably write to a log
     * file or send a message to the admin, since in most environments
     * there will probably only be a few hosts, and the inability to 
     * connect to them is of somewhat immediate concern.
     */
    public void connectionError(Connection c, String errMsg)
    {
    }
    SessionServer ss;
}
/**
 * This class supplies the AgentMonitor interface to the Connection class.
 * It is simply useful in letting one connection know when the other has died.
 * (i.e. disconnect from the host if the client bails, etc.)
 */
interface AgentMonitor
{
    public void agentHasDied(AgentIn a);
    public void agentHasDied(AgentOut a);
}
/**
 * This class represents a thread that is the connection to the client.
 */
class AgentIn implements Runnable
{
    /**
     * The InputStream from the client.
    */
    private InputStream in  = null;
    /**
    * The OutputStream to the host.
    */
    private OutputStream out         = null;
    /**
    * An agent monitor to notify the AgentOut when this connection dies.
    */
    private AgentMonitor am          = null;
    private byte buffer[]            = null;
    private static int BUFFER_SIZE   = 5000;

    /**
    * Public constructor for creating an AgentIn object.  Called from the
    * Connection object.
    * @param eis EncryptedInputStream object passed from the Connection object.
    *           Connection to client.
    * @param out OutputStream object passed from the Connection object.
    *           Connection to host.
    * @param am AgentMonitor object passed from the Connection object.
    * @see Connection
    */
    public AgentIn(InputStream eis, OutputStream out, AgentMonitor am)
    {
	this.in  = eis;
	this.out = out;
	this.am  = am;
	buffer = new byte[BUFFER_SIZE];
	//System.out.println("Agent In Started...");
	Thread t = new Thread(this);
	t.start();
    }

    /** 
    * Start the thread
    */
    public void run()
    {
	try
	    {
		int bytesRead = 0;

		while(true)
		    {
			if((bytesRead = in.read(buffer, 0, BUFFER_SIZE)) == -1)
			    {
				break;//Connection closed or lost.
			    }
			//System.out.println("Read " + bytesRead + " bytes...");
			out.write(buffer, 0, bytesRead);
			/*
			  FileWriter fw = new FileWriter("debug.this", true);
			  fw.write("FROM CLIENT:\n");
			  for(int i = 0; i < bytesRead; i++)
			  {
			  if(i % 16 == 0)
			  fw.write('\n');
			  int x = 0;                    
			  if(( x = buffer[i] ) < 0)
			  x += 256;
                                    
			  fw.write(Integer.toHexString(x) + " ");
			  }
			  fw.write("\n\n\n\n");
			  fw.close();
			*/
			//System.gc();
		    }
	    }
	//Once the loop is broken by a -1 return from in.read, the am.agentHasDied method
	//is fired, notifying the corresponding AgentOut that it's time to drop the 
	//connection.
	catch(IOException e) 
	    {
		//error(e.getMessage());
	    }
	am.agentHasDied(this);
    }
}

/**
 * This class represents a thread that is the connection to the host.
 */
class AgentOut implements Runnable
{
    /**
     * The InputStream from the host.
    */
    private   InputStream in           = null;
    /**
    * The EncryptedOutputStream to the client.
    */
    protected OutputStream out         = null;
    /**
    * The AgentMonitor for the connection.
    */
    private   AgentMonitor am          = null;
    private byte buffer[]            = null;
    private static int BUFFER_SIZE   = 4096;
    /**
    * Public constructor for creating an AgentOut object.  Called from the
    * Connection object.
    * @param in InputStream object passed from the Connection object.
    *           Connection to host.
    * @param eos EncryptedOutputStream object passed from the Connection object.
    *           Connection to client.
    * @param am AgentMonitor object passed from the Connection object.
    * @see Connection
    */
    public AgentOut(InputStream in, OutputStream eos, AgentMonitor am)
    {
	this.in  = in;
	this.out = eos;
	this.am  = am;
	buffer = new byte[BUFFER_SIZE];

	Thread t = new Thread(this);
	t.start();
	//System.out.println("Agent out started...");
    }
    /**
    * Start the thread
    */
    public void run()//see AgentIn for additional comments.
    {
	try
	    {
		int bytesRead = 0;

		while(true)
		    {
			if((bytesRead = in.read(buffer, 0, BUFFER_SIZE)) == -1)
			    break;

			out.write(buffer, 0, bytesRead);
			out.flush();
			/*
			  FileWriter fw = new FileWriter("debug.this", true);
			  fw.write("FROM HOST:\n\n");
			  for(int i = 0; i < bytesRead; i++)
			  {
			  if(i % 16 == 0)
			  fw.write('\n');
			  int x = 0;                    
			  if(( x = buffer[i] ) < 0)
			  x += 256;
                                    
			  fw.write(Integer.toHexString(x) + " ");
			  }
			  fw.write("\n\n\n\n");
			  fw.close();
			*/
			//System.gc();
		    }
	    }
	catch(IOException e)
	    {
		//error(e.getMessage());
	    }
	am.agentHasDied(this);
    }
}

/**
 * This class is the 'container' that represents a discrete client/host session.
 */
class Connection implements Runnable, AgentMonitor
{
    Category cat;
    /**
     * The default host.
     */
    private String            destHost         = null;
    /**
     * The default host's port number.
     */
    private int               destPort         = 23;
    /**
     * An AgentIn object for the connection to the client.
     * @see AgentIn
     */
    protected AgentIn           fromSrcToDest    = null;
    /**
     * An AgentOut object for the connection to the host.
     * @see AgentOut
     */
    protected AgentOut          fromDestToSrc    = null;
    /**
     * A socket for the client's connection.
     */
    protected Socket            srcSocket        = null;
    /**
     * A socket for the host's connection.
     */
    protected Socket            destSocket       = null;
    /**
     * Input stream <B>FROM</B> the client.
     */
    private InputStream       srcIn            = null;
    /**
     * Output stream <B>TO</B> the client.
     */
    private OutputStream      srcOut           = null;
    /**
     * Input stream <B>FROM</B> the host.
     */
    private InputStream       destIn           = null;
    /**
     * Ouput stream <B>TO</B> the host.
     */
    private OutputStream      destOut          = null;
    /**
     * A ConnecitonMonitor for the connection.
     */
    private ConnectionMonitor cm               = null;
    /**
     * Is the connection closed?
     */
    private boolean           connectionClosed = false;
    /**
     * For secure communications <B>TO</B> the client.
     * @see EncryptedOutputStream
     */
    private OutputStream eos          = null;
    /**
     * For secure communications <B>FROM</B> the client.
     * @see EncryptedInputStream
     */
    private InputStream eis           = null;
    /**
     * When the connection object was created
     */
    protected Date   sessionStarted            = null;
    /**
     * Encryption on/off (t/f)
     */
    private boolean encrypt                    = true;
   
    /**
     * Public constructor for a Connection object.  Called as soon as the
     * server socket receives a request on this port.
     * @param s Socket representing the clients request.
     * @param cm ConnectionMonitor object to provide feedback on the connections
     *           state.
     * @param host The default host.
     * @param hostPort The default host's port.
     * @param encrypt on/off(t/f)
     */
    public Connection(Socket s, ConnectionMonitor cm, String host, int hostPort, boolean encryption, SessionServer sessionserver)
    {
	cat = Category.getInstance("SessionServer.Connection");
	cat.debug("Creating new connection...");
	srcSocket = s;
	this.cm   = cm;
	destHost = host;
	destPort = hostPort;
	cm.attemptingConnection(this);
	sessionStarted = new Date();
	encrypt = encryption;
	this.sessionserver = sessionserver;
	cat.debug("Connection variables initialized...");
	try
	    {
		// Establish read/write for the socket
		srcIn  = s.getInputStream();
		srcOut = s.getOutputStream();
		//if we're encrypting, do the diffie-helman thing
		//rwc = new RWCipher("123456");		
		byte prime[] = {20, 120, -86, 121, -45, -121, 100, -28, 112, 105, 
				-20, 1, -87, 95, 41, 69, -66, -75, 63, 4, 
				8, -71, -101, 25, -66, -125, -74, -29, -24, -104, 
				70, 79, 60, -96, -108, 33, 45, -43, -29, 55, 
				-7, 108, -92, -114, 71, -53, -75, -19, 120, 127, 
				58, 28, 15, 85, 33, -118, 47, -63, 33, 40, 
				-11, -7, -16, -123, };
		byte gprime[] = {42, -75, -126, 69, 69, -115, 
				 -95, -63, 109, 9, 64, 110, 121, -119, -119, 52, 
				 37, -112, 73, -63, 82, 89, -22, 23, 110, 4, 
		};	        
		//System.out.println("Creating cipher...");

		//System.out.println(encrypt);
		eos = srcOut;
		eis = srcIn;
      
		//check for host info;
		int i = 0;
		byte hostNameIn[] = new byte[512];
		byte thisByte = 0x00;
		//the client will send 0xCC when it
		//is done sending the hostname, if any.
		cat.debug("Reading host request...");
		while(thisByte != (byte)0xCC)
		    {
			thisByte = (byte)eis.read();
			hostNameIn[i] = thisByte;
			i++;
		    }
		//the byte immediately following 0xCC will
		//be the port number.
		byte port = (byte)eis.read();
		byte hostNameDone[] = new byte[i];
		System.arraycopy(hostNameIn, 0, hostNameDone, 0, i - 1);
		cat.debug("Connecting to: " + new String(hostNameDone));
		if(i > 1)//if the new string != "", then we have a client-assigned host.
		    {
			destHost = new String(hostNameDone).trim();
			destPort = (int)port;
			//System.out.println(destHost + " port: " + destPort);
		    }

		//System.out.println("Hostname: " + new String(hostNameDone));   
		// Start ourself, so there's no delay in getting back
		// to the server to listen for new connections
		Thread t = new Thread(this);
		t.start();
	    }
	catch(IOException e)
	    {
		cat.error(e.getMessage());
		cm.connectionError(this, "" + e);
	    }
	System.gc();
    }
    /**
     * Start the thread.
     */
    public void run()
    {
	//System.out.println("Connecting to destination...");
	if(!connectToDest())
	    {
		closeSrc();//if the connection to the host fails,
		//dump the client.
	    }
	else
	    {
		//System.out.println("Successfully connected to destination...");
		// Ok, we're all ready ... since we've gotten this far,
		// add ourselves into the connection list
		//System.out.println("Adding connection...");
		cm.addConnection(this);
		//System.out.println("Checking Encryption...");
		// Create our two agents

		//System.out.println("Creating agents...");
		fromSrcToDest = new AgentIn(eis, destOut, this);
		fromDestToSrc = new AgentOut(destIn, eos, this);
		//System.out.println("Done...");
		// No need for our thread to continue, we'll be notified if
		// either of our agents dies
	    }
	//System.gc();
    }

    /**
     * Called when the connection to the client is lost.
     * @param a The connection to the client.
     * @see AgentIn
     */
    public synchronized void agentHasDied(AgentIn a)
    {
	if(connectionClosed) return;

	closeSrc();
	closeDest();

	cm.removeConnection(this);
	connectionClosed = true;
    }
    /**
     * Called when the connection to the host is lost.
     * @param a the connection to the host.
     * @see AgentOut
     */
    public synchronized void agentHasDied(AgentOut a)
    {
	if(connectionClosed) return;

	closeSrc();
	closeDest();

	cm.removeConnection(this);
	connectionClosed = true;
    }
    public void kill()
    {
	closeDest();
	closeSrc();
	cm.removeConnection(this);
	connectionClosed = true;
    }
    /**
     * This method establishes a connection to the host for this Connection object
     */
    private boolean connectToDest()
    {
	// Ok, we've got the host name and port to which we wish to
	// connect, try to establish a connection

	try
	    {
		//System.out.println("Creating new dest socket..." + destHost + " " + destPort);
		destSocket = new Socket(destHost, destPort);
		//System.out.println("Socket created, getting inputstream...");
		destIn     = destSocket.getInputStream();
		//System.out.println("Getting outputstream...");
		destOut    = destSocket.getOutputStream();
	    }
	catch(Exception e)
	    {
		//send email to the admin
		if(sessionserver.emailNotifyHostNotResponding)
		    {	
			StringBuffer sb = new StringBuffer();
			sb.append((new Date()).toString() + ": the TN3270 Gateway at " + destHost + " failed to respond ");
			sb.append("to a request from this RightHost 3270 SessionServer.\n\n");
			sb.append("The error may have several causes, including:\n\n");
			sb.append("\t1. The 3270 host may not be responding to the TN3270 gateway.\n");
			sb.append("\t2. The TN3270 gateway to the host may be down.\n");
			sb.append("\t3. The DNS name or IP address for the TN3270 gateway as specified in the RightHost 3270. ");
			sb.append("administrator's interface may not be valid.\n");
			sb.append("\t4. Excessive network traffic may have prevented a response from the TN3270 gateway within the specified timeout period\n\n");
			sb.append("Should you require it, support for RightHost 3270 is available at no charge via our web ");
			sb.append("site: http://www.rightware.com.  In addition, telephone support is available at the rate of ");
			sb.append("$100 USD per incident by calling (973)378-2300.\n\n");
			sb.append("Thank you for using RightHost 3270.");
			sessionserver.sendmail(sb.toString());												
		    }
		sessionserver.error("The 3270 Host failed to respond.");
		cm.connectionError(this, "connect error: "
				   + destHost + "/" + destPort + " " + e);
		return(false);
	    }

	return(true);
    }
    /**
     * Closes the connection to the client.
     */
    private void closeSrc()
    {
	try
	    {
		srcIn.close(); srcOut.close(); srcSocket.close();
	    }
	catch(Exception e) 
	    {
		sessionserver.error(e.getMessage());
	    }
    }

    /**
     * Close the connection to the host.
     */
    private void closeDest()
    {
	try
	    {
		destIn.close(); destOut.close(); destSocket.close();
	    }
	catch(Exception e)
	    {
		sessionserver.error(e.getMessage());	
	    }
    }
    /**
     * @return The clients hostname.
     */
    public String getSrcHost()
    {
	return(srcSocket.getInetAddress().toString());
    }
    /**
     * @return The host's hostname.
     */
    public String getDestHost()
    {
	return(destSocket.getInetAddress().toString());
    }
    /**
     * @return The host's port
     */
    public int getDestPort()
    {
	return(destPort);
    }
    private SessionServer sessionserver;
}
interface ConnectionMonitor         
{
    public void attemptingConnection(Connection c);
    public void addConnection(Connection c);
    public void removeConnection(Connection c);
    public void connectionError(Connection c, String errMsg);
}
