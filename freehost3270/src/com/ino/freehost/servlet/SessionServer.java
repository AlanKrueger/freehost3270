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
	 * Not Used...
	 */
	if(req.getParameter("initialactivesessions") != null)
	    {
		res.setContentType("text/html");
		res.getOutputStream().print(getInitialLoggedIn());
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
		/*
		 * Remove a host from the client configuration
		 */  	
		if(action.equals("removeHost"))
		    {
			removeHost(req.getParameter("hostname"));
		    }
		/*
		 * Add a host to the client configuration
		 */
		if(action.equals("Add Host"))
		    {
			//if host address is blank, send back page with error
			if(req.getParameter("hostaddress").equals(""))
			    {
				out.print(get3270hostsHTML(1));
				return;
			    }
			//if hostport is blank, send back page with error
			if(req.getParameter("hostport").equals(""))
			    {
				out.print(get3270hostsHTML(2));
				return;
			    }
			//if hostport is not a number, send back page with error
			try
			    {
				Integer.parseInt(req.getParameter("hostport"));
			    }
			catch(Exception e)
			    {
				//out.print(get3270hostsHTML(3));
				error(e.getMessage());
				return;
			    }
			//if friendlyHostName is blank, send back page with error
			if(req.getParameter("friendlyhostname").equals(""))
			    {
				out.print(get3270hostsHTML(4));
				return;
			    }
			addHost(req.getParameter("hostaddress"), req.getParameter("hostport"), req.getParameter("friendlyhostname"));
		    }
		/*
		 * Some of the parameters from the Admin interface are only stored
		 * when they're submitted.  For these, there is an additional HTML
		 * form button on the page called 'Save and Apply'...  On pages that
		 * use save and apply, a 'page' parameter must be passed so the appropriate
		 * page can be sent back
		 */
		if(action.equals("Save and Apply"))
		    {
			boolean flag = false;
			/*
			 * Checks for the manualEntry radio box from the Client Config
			 * page.
			 */
			if(req.getParameter("manualEntry") != null)
			    manualEntry = true;
			else
			    manualEntry = false;
			if(req.getParameter("page").equals("serverports"))
			    {
				//If they leave the host name blank, send it back with an error
				if (req.getParameter("sessionServerHost").equals(""))
				    {
					out.print(getserverportsHTML(1));
					return;
				    }
				//If we've made it this far, the host name is valid, assign it
				//to the sessionServerHost variable
				sessionServerHost = req.getParameter("sessionServerHost");
				/*
				 * If we're running under the RightWare HTTP server, the user
				 * can configure the HTTP port from here.  We need to check to
				 * insure that it's not blank or an invalid number.
				 */
				if(isRightHostHTTP)
				    {
					/*
					 * If it's blank send back the page with an error
					 */
					if(req.getParameter("httpPort").equals(""))
					    {
						out.print(getserverportsHTML(3));
						return;
					    }
					/*
					 * If it's not a valid number, parseInt will throw an Exception
					 * In this case, send back the page with an error.
					 */
					try
					    {
						httpPort = Integer.parseInt(req.getParameter("httpPort"));
					    }
					catch(Exception e)
					    {
						error(e.getMessage());
						//out.print(getserverportsHTML(4));
						return;
					    }
				    }
				/*
				 * If the sessionServerPort parameter is blank, send back the page
				 * with an error
				 */
				if(req.getParameter("sessionServerHost").equals(""))
				    {
					out.print(getserverportsHTML(5));
					return;
				    }
				/*
				 * If it's not a number, send back the page with an error
				 */
				try
				    {
					sessionServerPort = Integer.parseInt(req.getParameter("sessionServerPort"));
				    }
				catch(Exception e)
				    {
					error(e.getMessage());
					//out.print(getserverportsHTML(2));
					return;
				    }
			    }
			/*
			 * This section tests for the filterMode radio buttons
			 * 0 = no filtering
			 * 1 = allow all except those in list
			 * 2 = allow only those in list
			 */
			if(req.getParameter("page").equals("accessfilters"))
			    {
				if(req.getParameter("filtermode").equals("0"))
				    filterMode = 0;
				if(req.getParameter("filtermode").equals("1"))
				    filterMode = 1;
				if(req.getParameter("filtermode").equals("2"))
				    filterMode = 2;
				flag = true;
			    }
			/*
			 * Updates the logging information
			 */
			if(req.getParameter("page").equals("logging"))
			    {
				logAdminLogin = (req.getParameter("logAdminLogin") == null)?false:true;
				logBroadcastMessage = (req.getParameter("logBroadcastMessage") == null)?false:true;
				logKilledClient = (req.getParameter("logKilledClient") == null)?false:true;
				logSessionStart = (req.getParameter("logSessionStart") == null)?false:true;
				logSessionEnd = (req.getParameter("logSessionEnd") == null)?false:true;
				logServerErrors = (req.getParameter("logServerErrors") == null)?false:true;
			    } 
			/*
			 * Updates the email notification information
			 */
			if(req.getParameter("page").equals("emailnotify"))
			    { 
				if(req.getParameter("email").equals(""))
				    {
					out.print(getemailnotifyHTML(1));
					return;
				    }
				if(req.getParameter("smtpserver").equals(""))
				    {
					out.print(getemailnotifyHTML(2));
					return;
				    }
				if(req.getParameter("xsessionsareactive") != null)
				    {
					emailNotifyXSessionsAreActive = true;
					emailNotifyXSessions = Integer.parseInt(req.getParameter("xsessions").trim());
				    }
				else
				    {
					emailNotifyXSessionsAreActive = false;
				    }
				if(req.getParameter("hostnotresponding") != null)
				    {
					emailNotifyHostNotResponding = true;
				    }
				else
				    {
					emailNotifyHostNotResponding = false;
				    }
				adminEmail = req.getParameter("email");
				smtpserver = req.getParameter("smtpserver");
			    }
			/*
			 * Updates the default applet loading html.
			 */

			if(req.getParameter("page").equals("appletloading"))
			    {
				if(req.getParameter("html").equals(""))
				    {
					out.print(getappletloadingHTML(1));
					return;
				    }
				try
				    {
					FileOutputStream fos = new FileOutputStream(adminFile.getParent() + File.separator + "JavaHtml.htm");
					DataOutputStream dos = new DataOutputStream(fos);
					dos.writeBytes(req.getParameter("html"));
					dos.flush();
					dos.close();
					fos.close();
					out.print(getappletloadingHTML(0));
					return;
				    }
				catch(Exception e)
				    {
					error(e.getMessage());
				    }
			    }
			if(req.getParameter("page").equals("html3270"))
			    {
				if(req.getParameter("html").equals(""))
				    {
					out.print(get3270tohtmlHTML(1));
					return;
				    }
				try
				    {
					FileOutputStream fos = new FileOutputStream(adminFile.getParent() + File.separator + "html3270.htm");
					DataOutputStream dos = new DataOutputStream(fos);
					dos.writeBytes(req.getParameter("html"));
					dos.flush();
					dos.close();
					fos.close();
					out.print(get3270tohtmlHTML(0));
					return;
				    }
				catch(Exception e)
				    {
					error(e.getMessage());
				    }
			    }					 
			if(req.getParameter("page").equals("helpabout"))
			    {
				helpabout = req.getParameter("helpabout");
				out.print(gethelpaboutHTML());
				return;
			    }
			saveAndApply();
			if(flag)
			    restartServer();
		    }//end of the save and apply section
		/*
		 * This restores the contents of the applet loading file
		 * with the original HTML
		 */
		if(action.equals("Restore Default") && req.getParameter("page").equals("appletloading"))
		    {
			try
			    {
				//Open up the defht.rw file to copy to usrht.rw
				FileInputStream fis = new FileInputStream(adminFile.getParent() + File.separator + "defht.rw");
				DataInputStream dis = new DataInputStream(fis);
				//Open up the usrht.rw file to receive the default contents
				FileOutputStream fos = new FileOutputStream(adminFile.getParent() + File.separator + "JavaHtml.htm");
				DataOutputStream dos = new DataOutputStream(fos);
				String line = dis.readLine();
				while(line != null)
				    {
					dos.writeBytes(line + "\n");
					line = dis.readLine();
				    }
				dos.flush();
				dos.close();
				dis.close();
				fis.close();
			    }
			catch(Exception e)
			    {
				error(e.getMessage());
			    }
			out.print(getappletloadingHTML(0));
		    }
		if(action.equals("Restore Default") && req.getParameter("page").equals("html3270"))
		    {
			try
			    {
				//Open up the defht.rw file to copy to usrht.rw
				FileInputStream fis = new FileInputStream(adminFile.getParent() + File.separator + "defaultHtml3270.htm");
				DataInputStream dis = new DataInputStream(fis);
				//Open up the usrht.rw file to receive the default contents
				FileOutputStream fos = new FileOutputStream(adminFile.getParent() + File.separator + "Html3270.htm");
				DataOutputStream dos = new DataOutputStream(fos);
				String line = dis.readLine();
				while(line != null)
				    {
					dos.writeBytes(line + "\n");
					line = dis.readLine();
				    }
				dos.flush();
				dos.close();
				dis.close();
				fis.close();
			    }
			catch(Exception e)
			    {
				error(e.getMessage());
			    }
			out.print(get3270tohtmlHTML(0));
		    }
	    
		/*
		 * Turns on encryption and restarts the server
		 */
		if(action.equals("Enable Encryption"))
		    {
			encryption = true;
			out.print(getencryptionHTML());
			saveAndApply();
			restartServer();
			return;
		    }
		/*
		 * Turns off encryption and restarts the server
		 */
		if(action.equals("Disable Encryption"))
		    {
			encryption = false;
			out.print(getencryptionHTML());
			saveAndApply();
			restartServer();
			return;
		    }
		/*
		 * Sets the admin identification and password
		 */
		if(action.equals("adminpass"))
		    {
			//Admin ID can't be blank
			if(req.getParameter("adminName").equals(""))
			    {
				out.print(getserveradminaccountHTML(1));
				return;
			    }
			//Password can't be blank
			if(req.getParameter("pass").equals(""))
			    {
				out.print(getserveradminaccountHTML(2));
				return;
			    }
			/*
			 * We forced the user to enter his password twice on the
			 * page... here we'll check if they're equal.  If so, we'll
			 * send back the page, otherwise, we'll send back the page
			 * with the appropriate error message.
			 */
			if(req.getParameter("pass").equals(req.getParameter("pass2")))
			    {
				adminPass = req.getParameter("pass");
				adminName = req.getParameter("adminName");
				saveAndApply();
				out.print(getserveradminaccountHTML(0));
				return;
			    }
			else
			    {
				out.print(getserveradminaccountHTML(3));
				return;
			    }
		    }
		/*
		 * Adds another sessionserver to the loadbalancing list
		 * TO DO: Check for valid hostnames?
		 */
		if(action.equals("addloadbalancingserver"))
		    {
			if(req.getParameter("addsessionserver").equals(""))
			    {	
				out.print(getloadbalancingHTML(1));
				return;
			    }
			addLoadBalancingServer(req.getParameter("addsessionserver"));
			out.print(getloadbalancingHTML(0));
			return;
		    }
		/*
		 * Removes an existing sessionserver from the loadbalancing list
		 */
		if(action.equals("removeloadbalance"))
		    {
			removeLoadBalancingServer(req.getParameter("serverid"));
			out.print(getloadbalancingHTML(0));
			return;
		    }
		/*
		 * Toggle load balancing
		 */
		if(action.equals("enabledisableloadbalancing"))
		    {
			loadBalancing = !loadBalancing;
			out.print(getloadbalancingHTML(0));
			return;
		    }
		if(action.equals("copysessionserver"))
		    {
			//TO-DO: add the ability to copy configurations from other sessionservers
			out.print(getloadbalancingHTML(0));
			return;
		    }
		if(action.equals("search"))
		    {
			//TO-DO: add the ability to search for other sessionservers
			out.print(getloadbalancingHTML(0));
			return;
		    }
		/*
		 * Removes an access filter from the access filter list
		 */
		if(action.equals("removefilter"))
		    {
			removeFilter(req.getParameter("ip"));
			out.print(getaccessfiltersHTML(0));
			return;
		    }
		/* 
		 * Adds a filter to the access filter list
		 */
		if(action.equals("Add Filter"))
		    {
			//Check to see if the filter is blank, if so return the
			//page with an error message
			if(req.getParameter("addaddress").equals(""))
			    {
				out.print(getaccessfiltersHTML(1));
				return;
			    }
			//Now, check to see if the submitted filter is valid by calling
			//the checkIPAddress method
			boolean isValid = checkIPAddress(req.getParameter("addaddress"));
			if(!isValid)
			    {
				out.print(getaccessfiltersHTML(2));
				return;
			    }
			//If we've gotten this far, the filter is probably valid, send back
			//the page.
			else
			    {
				filterList.addElement(new RWFilterAddress(req.getParameter("addaddress")));
				out.print(getaccessfiltersHTML(0));
				return;
			    }
		    }
	    }

	String page = req.getParameter("page");
	if(page == null)
	    out.print(getadminmainHTML());
	else
	    {
		if(page.equals("actsesnav"))
		    out.print(getactsesnavHTML());             
		else if(page.equals("activesessions"))
		    out.print(getactivesessionsHTML());
		else if(page.equals("header"))
		    out.print(getheaderHTML());
		else if(page.equals("3270hosts"))
		    out.print(get3270hostsHTML(0));
		else if(page.equals("3270tohtml"))
		    out.print(get3270tohtmlHTML(0));
		else if(page.equals("accessfilters"))
		    out.print(getaccessfiltersHTML(0));
		else if(page.equals("appletloading"))
		    out.print(getappletloadingHTML(0));
		else if(page.equals("clientcfgnav"))
		    out.print(getclientcfgnavHTML());
		else if(page.equals("default"))
		    out.print(getdefaultHTML());
		else if(page.equals("emailnotify"))
		    out.print(getemailnotifyHTML(0));
		else if(page.equals("encryption"))
		    out.print(getencryptionHTML());
		else if(page.equals("helpabout"))
		    out.print(gethelpaboutHTML());
		else if(page.equals("htmlemulationdefault"))
		    out.print(gethtmlemulationdefaultHTML());
		else if(page.equals("loadbalancing"))
		    out.print(getloadbalancingHTML(0));
		else if(page.equals("logging"))
		    out.print(getloggingHTML());
		else if(page.equals("map"))
		    out.print(getmapHTML());
		else if(page.equals("nojava"))
		    out.print(getnojavaHTML());
		else if(page.equals("securitynav"))
		    out.print(getsecuritynavHTML());
		else if(page.equals("servcfgnav"))
		    out.print(getservcfgnavHTML());
		else if(page.equals("serveradminaccount"))
		    out.print(getserveradminaccountHTML(0));
		else if(page.equals("serverports"))
		    out.print(getserverportsHTML(0));
              
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
    private void removeFilter(String s)
    {
	Enumeration e = filterList.elements();
	while(e.hasMoreElements())
	    {
		RWFilterAddress s2 = (RWFilterAddress)e.nextElement();
		if(s2.getDisplayName().equals(s))
		    filterList.removeElement(s2);
	    }
    }
    private void removeLoadBalancingServer(String s)
    {
	Enumeration e = loadBalancingServers.elements();
	while(e.hasMoreElements())
	    {
		String s2 = (String)e.nextElement();
		if(s2.equals(s))
		    loadBalancingServers.removeElement(s2);
	    }
    }
    /**
     * This method adds a host to the loadbalancing vector
     */
    private void addLoadBalancingServer(String s)
    {
	loadBalancingServers.addElement(s);
    }
    /**
     * This method removes a host from the host vector
     */
    private void removeHost(String hostname)
    {
	Enumeration e = hostList.elements();
	while(e.hasMoreElements())
	    {
		Host h = (Host)e.nextElement();
		if(h.hostName.equals(hostname))
		    hostList.removeElement(h);
	    }
    }
    /**
     * This method adds a host to the host vector
     */
    private void addHost(String hostName, String hostPort, String friendlyName)
    {
	try
	    {
		Host h = new Host(hostName, Integer.parseInt(hostPort), friendlyName);
		hostList.addElement(h);
	    }
	catch(Exception e)
	    {
		return;
	    }
    }
    private void saveAndApply()
    {
	//we have to convert our vectors back
	//into delimited strings
	StringBuffer hosts = new StringBuffer();
	Enumeration e = hostList.elements();
	while(e.hasMoreElements())
	    {
		Host h = (Host)e.nextElement();
		hosts.append(h.hostName + "|" + h.port + "|" + h.friendlyName);
		if(e.hasMoreElements())
		    hosts.append("|");
	    }
	StringBuffer lb = new StringBuffer();
	e = loadBalancingServers.elements();
	while(e.hasMoreElements())
	    {
		lb.append((String)e.nextElement());
		if(e.hasMoreElements())
		    lb.append("|");
	    }
	StringBuffer filters = new StringBuffer();
	e = filterList.elements();
	while(e.hasMoreElements())
	    {
		filters.append(((RWFilterAddress)e.nextElement()).getDisplayName());
		if(e.hasMoreElements())
		    filters.append("|");
	    }
	config.put("filterList", new String(filters));
	config.put("filterMode", new Integer(filterMode).toString());
	config.put("loadBalancingServers", new String(lb));
	config.put("loadBalancing", new Boolean(loadBalancing).toString());
	config.put("sessionServerHost", sessionServerHost);
	config.put("sessionServerPort", new Integer(sessionServerPort).toString());
	config.put("helpabout", helpabout);
	config.put("emailNotifyHostNotResponding", (new Boolean(emailNotifyHostNotResponding)).toString());
	config.put("emailNotifyXSessionsAreActive", (new Boolean(emailNotifyXSessionsAreActive)).toString());
	config.put("emailNotifyXSessions", new Integer(emailNotifyXSessions).toString());
	config.put("logAdminLogin", new Boolean(logAdminLogin).toString());
	config.put("logBroadcastMessage", new Boolean(logBroadcastMessage).toString());
	config.put("logKilledClient", new Boolean(logKilledClient).toString());
	config.put("logSessionStart", new Boolean(logSessionStart).toString());
	config.put("logSessionEnd", new Boolean(logSessionEnd).toString());
	config.put("logServerErrors", new Boolean(logServerErrors).toString());
	if(isRightHostHTTP)
	    {
		Properties p = new Properties();
		try
		    {
			FileOutputStream fos = new FileOutputStream("props" + File.separator + "server.properties");
			p.put("server.port", new Integer(httpPort).toString());
			p.save(fos, "");
			fos.close();
		    }
		catch(Exception ex)
		    {
			error(ex.getMessage());
		    }
	    }
	config.put("hostList", new String(hosts));
	config.put("manualEntry", new Boolean(manualEntry).toString());
	config.put("encryption", new Boolean(encryption).toString());
	config.put("username", adminName);
	config.put("password", adminPass);
	config.put("adminEmail", adminEmail);
	config.put("smtpserver", smtpserver);
	try
	    {
		FileOutputStream fos = new FileOutputStream(adminFile);
		config.save(fos, "");
		fos.close();
	    }
	catch(IOException ioe)
	    {
		error(ioe.getMessage());
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
    /**
     * Returns the user-defined help-->about text for the applet
     */
    public String getHelpAbout()
    {
	return helpabout;
    }
    /**
     * Returns HTML corresponding to file: serveradminaccount.html
     */
    private String getserveradminaccountHTML(int errorStatus)
    {
   	//Error status:
   	//0 = no error
   	//1 = AdminName blank
   	//2 = Password 1 blank
   	//3 = Passwords don't match
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>serveradminaccount.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>SessionServer Administrator Account</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
	if(errorStatus == 3)
	    {
		s.append("<BR><FONT face=arial, helvetica size=+1 COLOR=FF0000>");
		s.append("The passwords you entered did not match.  Please try again.");
		s.append("</FONT><BR>");
	    }
	if(errorStatus == 1)
	    {
		s.append("<BR><FONT face=arial, helvetica size=+1 COLOR=FF0000>");
		s.append("Admin ID can't be left blank.  Please try again.");
		s.append("</FONT><BR>");      	 
	    }
	if(errorStatus == 2)
	    {	
		s.append("<BR><FONT face=arial, helvetica size=+1 COLOR=FF0000>");
		s.append("Password cannot be blank.  Please try again.");
		s.append("</FONT><BR>");      
	    }
   	s.append("<FORM action=Admin method=post>\n");
	s.append("<INPUT type=hidden name=action value=adminpass>");
   	s.append("\n");
   	s.append("<table border=0 cellspacing=\"10\" cellpadding=\"0\" align=\"LEFT\">\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Administrator ID:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("		<INPUT name = adminName type=text size=15 value=\"" + adminName + "\">\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Password:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("		<INPUT name=pass type=password size=15>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Confirm Password:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("		<INPUT name=pass2 type=password size=15>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("\n");
   	s.append("	</td>\n");
   	s.append("	<td ALIGN=\"LEFT\">\n");
   	s.append("		<INPUT type=submit value=\"Save and Apply\" id=submit1 name=submit1>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>Administrator ID</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	ID used to log in to the session server administration interface.  Does not affect user sessions.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Password</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Password associated with the administrator ID.  Type twice to prevent errors.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: adminmain.html
     */
    private String getadminmainHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("<!-- RightHost 3270 adminmain.html-->\n");
   	s.append("\n");
   	s.append("<html>\n");
   	s.append("\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>RightHost 3270 Administration</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<frameset rows=\"68, *\" border=\"1\">\n");
   	s.append("\n");
   	s.append("<frame src=\"Admin?page=header\" name=\"header\" noresize SCROLLING=\"NO\">\n");
   	s.append("\n");
   	s.append("<frameset cols=\"21%,*\" FRAMEBORDER=\"0\" FRAMESPACING=\"0\" BORDER=\"0\">\n");
   	s.append("<frame src=\"Admin?page=actsesnav\" name=\"nav\" scrolling=\"auto\" noresize marginheight=\"0\" marginwidth=\"0\">\n");
   	s.append("<frame src=\"Admin?page=activesessions\" name=\"content\" scrolling=\"auto\" noresize marginheight=\"0\" marginwidth=\"0\" frameborder=\"0\">\n");
   	s.append("</frameset>\n");
   	s.append("</frameset>\n");
   	s.append("\n");
   	s.append("</html>\n");
   	s.append("\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: header.html
     */
    private String getheaderHTML()
    {
	cat.debug("GETTING HEADER HTML");
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>RightHost 3270 Administration Header</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY BGCOLOR=\"white\">\n");
   	s.append("\n");
   	s.append("<center>\n");
   	s.append("\n");
   	s.append("<IMG SRC=\"graphics/rhlogo.jpg\" border=0 align=\"top\">\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2>\n");
	cat.debug("GETTING SESSION SERVER HOST");
   	s.append("Server: " + getSessionServerHost() + "&nbsp; &nbsp; Version 1.5 &nbsp; &nbsp; &#169; 1998 RightWare Inc.\n");
   	s.append("&nbsp; All Rights Reserved.\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("</center>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: map.html
     */
    private String getmapHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n");
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>Admin Map</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<IMG SRC=\"graphics/map.gif\" border=0>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: actsesnav.html
     */
    private String getactsesnavHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>RightHost 3270 Administration Header</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY BGCOLOR=\"white\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileopen.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=activesessions\" onClick=\"window.self.location='Admin?page=actsesnav'\" target=\"content\">\n");
   	s.append("Activity</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=serveradminaccount\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("Server Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=encryption\" onClick=\"window.self.location='Admin?page=securitynav'\" target=\"content\">\n");
   	s.append("Security Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=3270hosts\" onClick=\"window.self.location='Admin?page=clientcfgnav'\" target=\"content\">\n");
   	s.append("Client Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: servcfgnav.html
     */
    private String getservcfgnavHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>RightHost 3270 Administration Header</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY BGCOLOR=\"WHITE\">\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=activesessions\" onClick=\"window.self.location='Admin?page=actsesnav'\" target=\"content\">\n");
   	s.append("Activity</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileopen.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=serveradminaccount\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("Server Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectdown.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=serveradminaccount\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("Admin Account</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectdown.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=emailnotify\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("E-Mail Notify</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectdown.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=logging\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("Logging</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectdown.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=serverports\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("Server Ports</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectbottom.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=loadbalancing\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("Load Balancing</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=encryption\" onClick=\"window.self.location='Admin?page=securitynav'\" target=\"content\">\n");
   	s.append("Security Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=3270hosts\" onClick=\"window.self.location='Admin?page=clientcfgnav'\" target=\"content\">\n");
   	s.append("Client Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: securitynav.html
     */
    private String getsecuritynavHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>RightHost 3270 Administration Header</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY BGCOLOR=\"WHITE\">\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=activesessions\" onClick=\"window.self.location='Admin?page=actsesnav'\" target=\"content\">\n");
   	s.append("Activity</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=serveradminaccount\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("Server Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileopen.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=encryption\" onClick=\"window.self.location='Admin?page=securitynav'\" target=\"content\">\n");
   	s.append("Security Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectdown.jpg\" align=\"bottom\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=encryption\" onClick=\"window.self.location='Admin?page=securitynav'\" target=\"content\">\n");
   	s.append("Encryption</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectbottom.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=accessfilters\" onClick=\"window.self.location='Admin?page=securitynav'\" target=\"content\">\n");
   	s.append("Access Filters</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=3270hosts\" onClick=\"window.self.location='Admin?page=clientcfgnav'\" target=\"content\">\n");
   	s.append("Client Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: clientcfgnav.html
     */
    private String getclientcfgnavHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>RightHost 3270 Administration Header</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY BGCOLOR=\"WHITE\">\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0>\n");
   	s.append("<a href=\"Admin?page=activesessions\" onClick=\"window.self.location='Admin?page=actsesnav'\" target=\"content\">\n");
   	s.append("Activity</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=serveradminaccount\" onClick=\"window.self.location='Admin?page=servcfgnav'\" target=\"content\">\n");
   	s.append("Server Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileclosed.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=encryption\" onClick=\"window.self.location='Admin?page=securitynav'\" target=\"content\">\n");
   	s.append("Security Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("<IMG SRC=\"graphics/fileopen.jpg\" border=0> \n");
   	s.append("<a href=\"Admin?page=3270hosts\" onClick=\"window.self.location='Admin?page=clientcfgnav'\" target=\"content\">\n");
   	s.append("Client Config</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectdown.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=3270hosts\" onClick=\"window.self.location='Admin?page=clientcfgnav'\" target=\"content\">\n");
   	s.append("3270 Hosts</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectdown.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=appletloading\" onClick=\"window.self.location='Admin?page=clientcfgnav'\" target=\"content\">\n");
   	s.append("Applet Loading</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectdown.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=helpabout\" onClick=\"window.self.location='Admin?page=clientcfgnav'\" target=\"content\">\n");
   	s.append("Help->About</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<IMG SRC=\"graphics/connectbottom.jpg\" border=0>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2 COLOR=\"#910000\">\n");
   	s.append("<a href=\"Admin?page=3270tohtml\" onClick=\"window.self.location='Admin?page=clientcfgnav'\" target=\"content\">\n");
   	s.append("3270->HTML</a>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: activesessions.html
     */
    private String getactivesessionsHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>activesessions.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>Active Sessions</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<APPLET codebase=./Classes/Admin code=ActiveSessions.class width=600 height=200 ALIGN=\"middle\"></APPLET>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>Kill Selected</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Select single or multiple sessions (CTRL+click or SHIFT+click on Windows systems) then click <i>Kill Selected</i> to terminate\n");
   	s.append("	the selected sessions immediately.  Any work being performed by the user will be interrupted.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>Kill All</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Kills all active sessions immediately.  \n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
	//   	s.append("<tr>\n");
	//   	s.append("\n");
	//   	s.append("	<td valign=\"top\">\n");
	//   	s.append("	<br>\n");
	//   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
	//   	s.append("	<b>Broadcast Selected</b>\n");
	//   	s.append("	</FONT>\n");
	//   	s.append("	</td>\n");
	//   	s.append("\n");
	//   	s.append("	<td>\n");
	//   	s.append("	<br>\n");
	//   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
	//   	s.append("	Select one or more sessions then send a message to those users.  Useful when the server will be\n");
	//   	s.append("	taken down and to notify users before killing their sessions, allowing some time to save their work\n");
	//   	s.append("	and log out.\n");
	//   	s.append("	</FONT>\n");
	//   	s.append("	</td>\n");
	//   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>Broadcast All</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Sends a message to all active users.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>Stop SessionServer</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Immediately deactivates the session server.  User sessions will be terminated and no additional connections\n");
   	s.append("	will be accepted until the server is restarted.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>	\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: emailnotify.html
     */
    private String getemailnotifyHTML(int errorStatus)
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>emailnotify.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>Administrator E-Mail Notification</b>\n");
   	if(errorStatus > 0)
	    s.append("<br>Update Failed.<br>");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FORM action=Admin method=post>\n");
   	s.append("\n");
   	s.append("<table border=0 cellspacing=\"10\" cellpadding=\"0\">\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Admin E-mail Address:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	if(errorStatus == 1)
	    s.append("	<font face=\"arial, helvetica\" color=red size=2><b>Admin email cannot be left blank. Please try again.</b></font><BR>");
   	s.append("		<INPUT type=text name=email size=20 value=\"" + adminEmail + "\"><BR>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		SMTP Server:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	if(errorStatus == 2)
	    s.append("   <font face=\"arial, helvetica\" color=red size=2><b>Mail server cannot be left blank.  Please try again.</b></font><BR>");
   	s.append("		<INPUT type=text name=smtpserver size=20 value=\"" + smtpserver + "\">\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");   	
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td VALIGN=\"TOP\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Notify me when:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("				<FONT FACE=\"ARIAL, HELVETICA\">\n");
	// 	s.append("				<INPUT type=checkbox id=checkbox1 name=checkbox1>The SessionServer stops responding<BR>\n");
   	s.append("                <INPUT ");
   	if(emailNotifyHostNotResponding)
	    s.append("CHECKED ");
   	s.append("type=checkbox id=checkbox2 name=hostnotresponding>The 3270 host doesn't respond (Host Timeout)<BR>\n");
   	s.append("                <INPUT ");
   	if(emailNotifyXSessionsAreActive)
	    s.append("CHECKED ");
   	s.append(" type=checkbox id=checkbox3 name=xsessionsareactive>More than <INPUT type=text value=\"" + emailNotifyXSessions + "\" size=\"4\" name=xsessions> sessions are active<BR>\n");
   	s.append("				</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("\n");
   	s.append("	</td>\n");
   	s.append("	<td ALIGN=\"LEFT\">\n");
   	s.append("		<INPUT type=hidden name=page value=emailnotify>\n");
   	s.append("		<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>Admin e-mail address</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	The e-mail address of the that notification messages generated by this SessionServer will be sent to.  \n");
   	s.append("	Should be the e-mail address of the administrator or group responsible for SessionServer management.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>SMTP Server</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("  An SMTP server that will send the mail to you.  \n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Notification types</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
	// 	s.append("	<i>Session server stops responding</i>.  An e-mail message will be sent to the administrator's address\n");
	// 	s.append("	when a problem with the session server occurs that causes it to stop responding.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>The 3270 host does not respond</i>.  An e-mail message will be sent to the administrator's address when\n");
   	s.append("	the 3270 host doesn't respond (Host Timeout error in the datastream).\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>More than x sessions active</i>.  An e-mail message will be sent to the administrator's address when\n");
   	s.append("	more than the selected number of sessions are active on the SessionServer.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: logging.html
     */
    private String getloggingHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>logging.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>Logging Configuration</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FORM action=Admin method=post>\n");
   	s.append("\n");
   	s.append("<table border=0 cellspacing=\"10\" cellpadding=\"0\" align=\"LEFT\">\n");
   	//s.append("<tr>\n");
   	//s.append("	<td>\n");
   	//s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	//s.append("		Log path and filename:\n");
   	//s.append("		</font>\n");
   	//s.append("	</td>\n");
   	//s.append("	<td align=\"left\">\n");
   	//s.append("  <INPUT type=hidden name=page value=logging>");
   	//s.append("		<INPUT type=text name=logFile size=40 value=\"" + logFile + "\">\n");
   	//s.append("	</td>\n");
   	//s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td VALIGN=\"TOP\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Log these events:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("				<FONT FACE=\"ARIAL, HELVETICA\">\n");
	//   	s.append("				<INPUT ");
	//   	if(logAdminLogin)
	//   		s.append("CHECKED ");
	//   	s.append("type=checkbox name=logAdminLogin><font face=helvetica size=2>Administrator logs in<BR>\n");
   	s.append("                <INPUT "); 
   	if(logBroadcastMessage)
	    s.append("CHECKED ");
   	s.append("type=checkbox name=logBroadcastMessage>Broadcast message sent to clients<BR>\n");
   	s.append("                <INPUT ");
   	if(logKilledClient)
	    s.append("CHECKED ");
   	s.append("type=checkbox name=logKilledClient>Client(s) killed<BR>\n");
   	s.append("                <INPUT ");
   	if(logSessionStart)
	    s.append("CHECKED ");
   	s.append("type=checkbox name=logSessionStart>Client session start<BR>\n");
   	s.append("                <INPUT ");
   	if(logSessionEnd)
	    s.append("CHECKED ");
   	s.append("type=checkbox name=logSessionEnd>Client session end<BR>\n");
   	s.append("                <INPUT ");
   	if(logServerErrors)
	    s.append("CHECKED ");
   	s.append("type=checkbox name=logServerErrors>Server Errors<BR>\n");
   	s.append("				</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<TD>\n");
   	s.append("\n");
   	s.append("	</td>\n");
   	s.append("	<td ALIGN=\"LEFT\">\n");
   	s.append("    <INPUT type=hidden name=page value=logging>\n");
   	s.append("		<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
	//   	s.append("<tr>\n");
	//   	s.append("	<td valign=\"top\">\n");
	//   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
	//   	s.append("	<b>Log path and filename</b>\n");
	//   	s.append("	</FONT>\n");
	//   	s.append("	</td>\n");
	//   	s.append("\n");
	//   	s.append("	<td>\n");
	//   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
	//   	s.append("	Enter a complete path and filename for log storage.  Logs are in ASCII format for easy import into\n");
	//   	s.append("	a database or other presentation and management tool.  Logging to file is an additional way to track\n");
	//   	s.append("	SessionServer events and errors.  Logging to file can be used alone or in conjunction with e-mail notification.\n");
	//   	s.append("	</FONT>\n");
	//   	s.append("	</td>\n");
	//   	s.append("</tr>\n");
	//   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Event types</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
	//   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
	//   	s.append("	<i>Administragor logs in.</i>.  Logs every access to the SessionServer by a system administrator.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Broadcast message sent to clients</i>.  Logs broadcast messages sent to active clients from the\n");
   	s.append("	ACTIVE SESSIONS screen.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Client(s) killed</i>.  Logs active client sessions terminated from the ACTIVE SESSIONS screen.\n");
   	s.append("	\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Client session start</i>.  Logs every client session established through this SessionServer.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Client session end</i>.  Logs client sessions ended on this SessionServer.	Client session \n");
   	s.append("	start and client session end are user events - sessions started and ended in regular\n");
   	s.append("	use.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Server errors</i>.  Logs errors generated by this SessionServer.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: serverports.html
     */
    private String getserverportsHTML(int errorStatus)
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>serveradminaccount.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>Server Ports</b>\n");
	if(errorStatus > 0)
	    s.append("<h2>Update Failed</h2>");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FORM>\n");
   	s.append("\n");
   	s.append("<table border=0 cellspacing=\"10\" cellpadding=\"0\" align=\"LEFT\">\n");
	if(getServletContext().getServerInfo().equals("RightWare SessionServer HTTP Server"))
	    {
		s.append("<tr>\n");
		s.append("	<td>\n");
		s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
		s.append("		Client/Admin HTTP port:\n");
		s.append("		</font>\n");
		s.append("	</td>\n");
		s.append("	<td align=\"left\">\n");
		if(errorStatus == 3)
		    s.append("<font face=\"arial, helvetica\" size=2 color=red><b>The HTTP Port cannot be left blank. Please try again.</b></font><BR>");
		if(errorStatus == 4)
		    s.append("<font face=\"arial, helvetica\" size=2 color=red><b>The HTTP Port must be a number. Please try again.</b></font><BR>");
		s.append("		<INPUT type=text name=httpPort size=5 value=\"" + httpPort + "\">\n");
		s.append("<font face=\"arial, helvetica\" size=1>\n");
		s.append("HTTP port changes will not take effect until the server is restarted from the command line.</font>");
		s.append("	</td>\n");
		s.append("</tr>\n");
	    }
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Server Hostname/IP address:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
	if(errorStatus == 1)
	    {
		s.append("<font face=\"arial, helvetica\" size=2 color=red><B>Host name cannot be left blank.  Please try again.</B></font><BR>");
	    }
   	s.append("		<INPUT name=sessionServerHost type=text size=25 value=\"" + sessionServerHost + "\">\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		SessionServer Port:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
	if(errorStatus == 2)
	    {
		s.append("<font face=\"arial, helvetica\" size=2 color=red><B>Invalid Port Number.  Please try again.</B></font><BR>");
	    }
	if(errorStatus == 5)
	    s.append("<font face=\"arial, helvetica\" size=2 color=red><B>Session Server Port cannot be left blank.  Please try again.</B></font><BR>");
   	s.append("		<INPUT type=text name=sessionServerPort size=5 value=\"" + sessionServerPort + "\">\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<TD>\n");
   	s.append("\n");
   	s.append("	</td>\n");
   	s.append("	<td ALIGN=\"LEFT\">\n");
	s.append("     <INPUT type=hidden name=page value=serverports>");
   	s.append("		<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
	if(isRightHostHTTP)
	    {
		//      	s.append("<tr>\n");
		//      	s.append("	<td valign=\"top\">\n");
		//      	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
		//      	s.append("	<b>Admin HTTP port</b>\n");
		//      	s.append("	</FONT>\n");
		//      	s.append("	</td>\n");
		//      	s.append("\n");
		//      	s.append("	<td>\n");
		//      	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
		//      	s.append("	The HTTP (web) port the server will use for administration (the functions you are using now), originally configured\n");
		//      	s.append("	during installation.  This port can be blocked to outside access by firewalls provided you don't need to administer the server from \n");
		//      	s.append("	external locations.  You must restart the server before this setting takes effect and you will need to connect to\n");
		//      	s.append("	this administration interface using the new port number thereafter.\n");
		//      	s.append("	</FONT>\n");
		//      	s.append("	</td>\n");
		//      	s.append("</tr>\n");
		s.append("<tr>\n");
		s.append("	<td valign=\"top\">\n");
		s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
		s.append("	<br>\n");
		s.append("	<b>Client/Admin HTTP port</b>\n");
		s.append("	</FONT>\n");
		s.append("	</td>\n");
		s.append("\n");
		s.append("	<td>\n");
		s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
		s.append("	<br>\n");
		s.append("	The HTTP (web) port the server will use for 3270-to-HTTP conversion.  Leave at the default of 80 unless there is\n");
		s.append("	another web server running on the same hardware with this SessionServer.\n");
		s.append("	</FONT>\n");
		s.append("	</td>\n");
		s.append("</tr>\n");
	    }
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>SessionServer\n");
   	s.append("	<br>\n");
   	s.append("	Hostname/IP\n");
   	s.append("	<br>\n");
   	s.append("	address</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	This parameter is important for the server's operation.  It should coincide with the IP address,\n");
   	s.append("	or, if you have an internal DNS configuration, the hostname associated with the hardware \n");
   	s.append("	this SessionServer is running on.  Be sure to keep this parameter up to date if you change the\n");
   	s.append("	server's IP address and/or host name.\n");
   	s.append("	\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("\n");
   	s.append("	If you use DNS to manage internal IP-to-hostname mapping,\n");
   	s.append("	you shouldn't need to change the hostname here once it has been set since the DNS configuration\n");
   	s.append("	should always have the correct IP address.\n");
   	s.append("	\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	If there are multiple network interfaces installed in the server, use the address or hostname of\n");
   	s.append("	the network interface that user workstations (emulator applet and 3270-to-HTML emulation) will \n");
   	s.append("	connect to.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<IMG SRC=\"graphics/whichinterface.gif\" border=0>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>SessionServer Port\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	This is the port that the emulator applet uses to communicate with the SessionServer.  It is important\n");
   	s.append("	that this port be opened on any firewalls between user nodes and the SessionServer.  If you are using\n");
   	s.append("	3270-to-HTML emulation, the Client HTTP port should also be opened on firewalls.	\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	These settings will not take effect until the SessionServer is restarted.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	</FONT>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>	\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: loadbalancing.html
     */
    private String getloadbalancingHTML(int errorStatus)
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>serveradminaccount.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>Load Balancing is currently: ");
	if(loadBalancing)
	    s.append("Enabled");
	else
	    s.append("Disabled");
	s.append("</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<table border=0 cellspacing=\"10\" cellpadding=\"0\">\n");
   	s.append("\n");
   	//s.append("<tr>\n");
   	//s.append("<FORM action=Admin method=post>\n");
   	//s.append("	<td>\n");
	//s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	//s.append("		Copy configuration from:\n");
   	//s.append("		</font>\n");
   	//s.append("	</td>\n");
   	//s.append("	<td align=\"left\">\n");
   	//s.append("		<INPUT name=copyFrom type=text size=25 value=\"www.otherserver.com\">\n");
   	//s.append("	</td>\n");
   	//s.append("	<td align=\"left\">\n");
	//s.append("     <INPUT type=hidden name=action value=copysessionserver>");
   	//s.append("		<INPUT type=submit value=\"Copy\" id=submit1 name=submit1>\n");
   	//s.append("	</td>	\n");
	//s.append("     </FORM>");
   	//s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
	s.append("     <FORM action=Admin method=post>");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Add a SessionServer:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
	if(errorStatus == 1)
	    {
		s.append("<font face=\"arial, helvetica\" size=2 color=red><B>Invalid Host Name. Please Try Again.</B></font><BR>");
	    }
   	s.append("		<INPUT type=text size=25 name=\"addsessionserver\" value=\"www.addthisserver.com\">\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
	s.append("     <INPUT type=hidden name=action value=addloadbalancingserver>");
   	s.append("		<INPUT type=submit value=\"Add\" id=submit1 name=submit1>\n");
   	s.append("	</td>	\n");
	s.append("</FORM>");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Participating SessionServers:\n");
   	s.append("		</font>\n");
   	s.append("	</td><td></td><td></TD></TR>\n");
   	s.append("\n");
	Enumeration e = loadBalancingServers.elements();
	while(e.hasMoreElements())
	    {
		String h = (String)e.nextElement();
		s.append("  <tr><td>&nbsp;</td>\n");
		s.append("<FORM>");
		s.append("	<td>\n");
		s.append("		<INPUT type=text size=25 value=\"" + h + "\">\n");
		s.append("	</td>\n");
		s.append("	<td>\n");
		s.append("		<A HREF=\"Admin?serverid="+ h +"&action=removeloadbalance\"><IMG SRC=\"graphics/removebutton.jpg\" border=0 alt=\"Remove this SessionServer\"></a>\n");
		s.append("	</td>	\n");
		s.append("	\n");
		s.append("</FORM>");
		s.append("</tr>\n");
		s.append("\n");
	    }
   	s.append("\n");
	//   	s.append("<tr>\n");
	//   	s.append("	<td valign=top>\n");
	//   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
	//   	s.append("		Search for SessionServers:\n");
	//   	s.append("		</font>\n");
	//   	s.append("	</td>\n");
	//   	s.append("\n");
	//   	s.append("	<td>\n");
	//      s.append("  <FORM action=Admin method=post>");
	//      s.append("  <INPUT type=hidden name=action value=search>");
	//   	s.append("  <INPUT type=submit value=\"Search\" id=submit1 name=submit1>\n");
	//      s.append("  </FORM>");
	//   	s.append("	</td><TD></TD>	\n");
	//   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=top>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
	if(loadBalancing)
	    s.append("		Disable Load Balancing:\n");
	else
	    s.append("		Enable Load Balancing:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
	s.append("     <FORM action=Admin method=post>");
	s.append("     <INPUT type=hidden name=action value=enabledisableloadbalancing>");
   	s.append("		<INPUT type=submit value=\"");
	if(loadBalancing)
	    s.append("Disable");
	else
	    s.append("Enable");
	s.append("\" id=submit1 name=button>\n");
	s.append("</FORM>");
   	s.append("	</td><TD></TD>	\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
	s.append("     <FORM action=Admin method=post>");
	s.append("     <INPUT type=hidden value=loadbalancing name=page>");
   	s.append("		<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
	s.append("     </FORM>");
   	s.append("	</td><td></td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table border=0 cellspacing=\"6\" cellpadding=\"0\">\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>Copy configuration from</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	The IP addresses of all SessionServers participating in Load Balancing, including this one, should be\n");
   	s.append("	included in the parcipating servers list.\n");
   	s.append("\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	If another server already has the appropriate Load Balancing configuration, you may simply copy it\n");
   	s.append("	to this server automatically by entering the source server's address here.\n");
   	s.append("	\n");
   	s.append("	<br>\n");
   	s.append("	</FONT>\n");
   	s.append("	<IMG SRC=\"graphics/loadbaloverview.gif\" border=0>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Add a SessionServer</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	If this is the first server participating in Load Balancing or if you don't have another server to\n");
   	s.append("	copy the configuration from, use this field to manually enter the hostname or IP address of a\n");
   	s.append("	SessionServer to add to the participating server list.\n");
   	s.append("\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("    Remember to include the hostname or IP address of this SessionServer.\n");
   	s.append("	\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Participating SessionServers</B>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Lists hostnames and/or IP addresses of the SessionServers currently participating in Load Balancing.  If you enable Load Balancing\n");
   	s.append("	on this SessionServer, its IP address should be included.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	Click <IMG SRC=\"graphics/removebutton.jpg\" border=0> to eliminate the selected SessionServer from participation.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
	//   	s.append("<tr>\n");
	//   	s.append("\n");
	//   	s.append("	<td valign=\"top\">\n");
	//   	s.append("\n");
	//   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
	//   	s.append("	<br>\n");
	//   	s.append("	<b>Search for SessionServers</b>\n");
	//   	s.append("	</FONT>\n");
	//   	s.append("	</td>\n");
	//   	s.append("\n");
	//   	s.append("	<td valign=\"top\"> \n");
	//   	s.append("	<br>\n");
	//   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
	//   	s.append("	Select search if you do not know the hostnames and/or IP addresses of other SessionServers that you'd\n");
	//   	s.append("	like to include in Load Balancing participation.  This server will attempt to locate other SessionServers\n");
	//   	s.append("	on the network.\n");
	//   	s.append("	</FONT>\n");
	//   	s.append("	</td>\n");
	//   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Disable<br>\n");
   	s.append("	Load Balancing</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Remove this SessionServer from participation in Load Balancing.\n");
   	s.append("	</FONT>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>	\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: encryption.html
     */
    private String getencryptionHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>encryption.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>Encryption is currently: ");
	if(encryption)
	    s.append("Enabled");
	else
	    s.append("Disabled");
	s.append("</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FORM>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<INPUT type=submit value=\"");
	if(encryption)
	    s.append("Disable ");
	else
	    s.append("Enable ");
	s.append("Encryption\" id=submit1 name=action>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>About Encryption</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	This setting controls the encryption of the data stream between the RightHost SessionServer and the\n");
   	s.append("	emulation applet.\n");
   	s.append("	\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
	//   	s.append("	If you are using 3270-to-HTML conversion, you must integrate RightHost with a Web server that\n");
	//   	s.append("	supports SSL (Secure Sockets Layer) for the HTML data stream to be encrypted.\n");
	//   	s.append("\n");
   	s.append("	See the documentation for instructions on how to integrate RightHost with a commercial Web server.\n");
   	s.append("	\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Enable/Disable</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Changes to encryption are applied when the server is restarted (select <i>Activity</i> then <i>Stop SessionServer</i>).\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: accessfilters.html
     */
    private String getaccessfiltersHTML(int errorStatus)
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>accessfilters.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>Access Filter Configuration</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FORM action=Admin method=post>\n");
   	s.append("\n");
   	s.append("<table border=0 cellspacing=\"10\" cellpadding=\"0\">\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td VALIGN=\"TOP\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Access filter mode:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("			<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("            <INPUT ");
	if(filterMode == 0)
            s.append("CHECKED ");
	s.append("type=radio name=\"filtermode\" value=\"0\">None<BR>	\n");
   	s.append("			<INPUT ");
	if(filterMode == 1)
	    s.append("CHECKED ");
	s.append("type=radio name=\"filtermode\" value=\"1\">Allow <i>all</i> IP addresses <i>except</i> :<BR>\n");
   	s.append("			<INPUT ");
	if(filterMode == 2)
	    s.append("CHECKED ");
	s.append(" type=radio name=\"filtermode\" value=\"2\">Allow <i>only</i> these IP addresses :<BR>\n");
   	s.append("			</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\" colspan = 3>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Filtered addresses:\n");
   	s.append("		</font>\n");
   	s.append("	</td></tr>\n");
	Enumeration e = filterList.elements();
	while(e.hasMoreElements())
	    {
		String f = ((RWFilterAddress)e.nextElement()).getDisplayName();
		s.append("<tr>\n");
		s.append("\n");
		s.append("	<td>\n");
		s.append("\n");
		s.append("	</td>\n");
		s.append("\n");
		s.append("	<td>\n");
		s.append("		<INPUT type=text size=28 name=\"sessionserver\" value=\"" + f + "\">\n");
		s.append("	</td>\n");
		s.append("	\n");
		s.append("	<td>\n");
		s.append("		<A HREF=\"Admin?ip=" + f + "&action=removefilter\"><IMG SRC=\"graphics/removebutton.jpg\" border=0 alt=\"Remove this IP address\"></a>\n");
		s.append("	</td>\n");
		s.append("\n");
		s.append("</tr>\n");
	    }
   	s.append("\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Add an address:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
	if(errorStatus == 2)   	
	    {
	  	s.append("<td></td>\n");
	  	s.append("<td></td>\n");
	  	s.append("<tr>");
	   	s.append("	<td align=\"left\" colspan=3>\n");
		s.append("		<font face=\"arial, helvetica\" size=2 color=red><b>You submitted an invalid filter address.  Please Try Again.</b></font><BR>");   	
		s.append("</td>\n");
		s.append("</tr>");
		s.append("<tr><td></td>");
	    }
	s.append("<td align=\"left\">\n");
   	s.append("		<INPUT type=text size=28 name=\"addaddress\">\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
	s.append("		<INPUT type=submit value=\"Add Filter\" id=submit1 name=action>\n");
   	s.append("	</td>	\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("\n");
   	s.append("	</td>\n");
   	s.append("	\n");
   	s.append("	<td>\n");
	s.append("     <INPUT type=hidden value=accessfilters name=page>");
   	s.append("		<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>About access filters</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	This optional setting allows you to control which IP addresses and/or network addresses will be allowed\n");
   	s.append("	access to this SessionServer.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	This is an additional security measure above and beyond data stream encryption.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Access filter modes</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	<i>None.</i>  Access filtering on this SessionServer is disabled.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Allow all IP address except:</i> Allows connections from any IP address except the ones entered.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Allow only these IP addresses:</i> Allows connections only from the IP addresses entered.  All others are\n");
   	s.append("	blocked.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Filtered addresses\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	List of addresses currently in the filter table. \n");
   	s.append("	Click <IMG SRC=\"graphics/removebutton.jpg\" border=0> to remove the selected address.\n");
   	s.append("	<br>\n");
   	s.append("\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Add an address\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Enter IP addresses to be compared against the filter mode selected.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	You may enter individual IP addresses or entire network ranges with the \"*\" wildcard.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	Example:\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<IMG SRC=\"graphics/filterexplanation.jpg\" border=0>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>	\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: 3270hosts.html
     */
    private String get3270hostsHTML(int errorStatus)
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>3270hosts.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>3270 Hosts</b>\n");
   	if(errorStatus > 0)
	    s.append("<BR><B>Add Host Failed.</B><BR>");   	
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<table border=0 cellspacing=\"4\" cellpadding=\"0\">\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td VALIGN=\"TOP\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Hosts:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("	\n");
   	s.append("		<table border=1 CELLSPACING=\"2\" CELLPADDING=\"1\">\n");
   	s.append("		<tr>\n");
   	s.append("			<td align=\"center\">\n");
   	s.append("				<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("				<b>Host Address</b>\n");
   	s.append("				</FONT>\n");
   	s.append("			</td>\n");
   	s.append("			<td align=\"center\">\n");
   	s.append("				<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("				<b>Host Port</b>\n");
   	s.append("				</FONT>\n");
   	s.append("			</td>\n");
   	s.append("			<td align=\"center\">\n");
   	s.append("				<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("				<b>\"Friendly\" Hostname</b>\n");
   	s.append("				</FONT>\n");
   	s.append("			</td>\n");
   	s.append("			<td align=\"center\">\n");
   	s.append("				<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("				<b>Remove</b>\n");
   	s.append("				</FONT>\n");
   	s.append("			</td>\n");
   	s.append("		</tr>\n");
	Enumeration e = hostList.elements();
	while(e.hasMoreElements())
	    {
		Host h = (Host)e.nextElement();
		s.append("		<tr>\n");
		s.append("			<td align=\"center\">\n");
		s.append("				<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
		s.append("				" + h.hostName + "\n");
		s.append("				</FONT>\n");
		s.append("			</td>\n");
		s.append("			<td align=\"center\">\n");
		s.append("				<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
		s.append("				" + h.port + "\n");
		s.append("				</FONT>\n");
		s.append("			</td>\n");
		s.append("			<td align=\"center\">\n");
		s.append("				<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
		s.append("				" + h.friendlyName + "\n");
		s.append("				</FONT>\n");
		s.append("			</td>\n");
		s.append("			<td align=\"center\" valign=\"center\">\n");
		s.append("				<A HREF=Admin?hostname=" + h.hostName + "&action=removeHost&page=3270hosts>\n");
		s.append("				<IMG SRC=\"graphics/removebutton.jpg\" border=0 alt=\"Remove this host\"></a>\n");
		s.append("			</td>\n");
		s.append("		</tr>\n");
	    }
   	s.append("		\n");
   	s.append("<FORM action=Admin method=post>\n");
   	s.append("		</table>\n");
   	s.append("	</td>	\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<br>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Add:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("		<table>\n");
   	s.append("			<tr>\n");
   	s.append("				<td valign=top>\n");
   	s.append("					<br>\n");
   	s.append("					<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2>\n");
   	s.append("					<center>Host Address<br></center>\n");
   	s.append("					</font>\n");
   	s.append("					<INPUT type=text size=20 name=\"hostaddress\"><BR>\n");
	if(errorStatus == 1)
	    s.append("<font face=\"arial, helvetica\" size=2 color=red><B>Host Address cannot be blank.</b></font><BR>");   	
   	s.append("				</td>\n");
   	s.append("				<td valign=top>\n");
   	s.append("					<br>\n");
   	s.append("					<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2>\n");
   	s.append("					<center>Host Port<br></center>\n");
   	s.append("					</font>\n");
   	s.append("					<INPUT type=text size=4 name=\"hostport\" value=\"23\"><BR>\n");
   	if(errorStatus == 2)
	    s.append("<font face=\"arial, helvetica\" size=2 color=red><b>Host Port cannot be blank.</b></font><BR>"); 
   	if(errorStatus == 3)
	    s.append("<font face=\"arial, helvetica\" size=2 color=red><b>Host Port must be a number.</b></font><BR>");   		  	
   	s.append("				</td>\n");
   	s.append("				<td valign=top>\n");
   	s.append("					<br>\n");
   	s.append("					<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-2>\n");
   	s.append("					<center>\"Friendly\" hostname<br></center>\n");
   	s.append("					</font>\n");
   	s.append("					<INPUT type=text size=20 name=\"friendlyhostname\"><BR>\n");
   	if(errorStatus == 4)
	    s.append("<font face=\"arial, helvetica\" size=2 color=red><b>Friendly Name cannot be left blank. Please try again.</b></font><BR>");   	
   	s.append("				</td>\n");
   	s.append("				<td>\n");
   	s.append("					<br>\n");
   	s.append("					<br>\n");
	s.append("              <INPUT type=hidden value=3270hosts name=page>\n");  
   	s.append("					<INPUT type=submit value=\"Add Host\" id=submit1 name=action>\n");
   	s.append("				</td>				\n");
   	s.append("			</tr>\n");
   	s.append("		</table>\n");
   	s.append("	\n");
	s.append("</FORM>");
	s.append("<FORM action=Admin method=post>");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<br>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		Options:\n");
   	s.append("		</font>\n");
   	s.append("	</td>\n");
   	s.append("	<td align=\"left\">\n");
   	s.append("		<br>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		<INPUT ");
	if(manualEntry)
	    s.append("CHECKED ");
	s.append("type=checkbox name=manualEntry value=true>Users can enter host addresses manually\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
   	s.append("		<br>\n");
	s.append("     <INPUT type=hidden value=3270hosts name=page>\n");
   	s.append("		<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>Hosts</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	These are the TN3270 hosts available to users of the RightHost emulator applet.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	If only one host is listed, users will be automatically connected to that host unless\n");
   	s.append("	\"Users can enter host addresses manually\" is checked below, in which case users will be\n");
   	s.append("	presented with a selection box.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	If more than one host is listed, users will be presented with a selection box showing the\n");
   	s.append("	\"friendly\" hostnames.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<IMG SRC=\"graphics/removebutton.jpg\" border=0> removes the selected host entry.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Add a<br>\n");
   	s.append("	host</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	<i>Host Address</i> can be either the DNS name or IP address of a TN3270 host.  The host must\n");
   	s.append("	be accessible from the SessionServer.  \n");
   	s.append("\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Host Port</i> is the port number (default is 23) that the SessionServer will use to connect with\n");
   	s.append("	the TN3270 host.  If the host being added is external to your network, all firewalls\n");
   	s.append("	between the SessionServer and the host must have this port number open.\n");
   	s.append("	\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>Friendly Hostname</i> presents a recognizable name to users when choosing which host to connect\n");
   	s.append("	to in the selection box.\n");
   	s.append("\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Options\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\"> \n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	If <i>Users can enter host addresses manually</i> is checked, users will be allowed to specify any\n");
   	s.append("	specific destination hostname and/or IP address.  \n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	Otherwise, users will be only be able to access hosts specified in the list above.\n");
   	s.append("	</FONT>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: default .html
     */
    private String getdefaultHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("<HTML> \n");
   	s.append("<BODY BGCOLOR=\"white\">\n");
   	s.append("\n");
   	s.append("<center>\n");
   	s.append("\n");
   	s.append("<!-- 1 of 2: REPLACE RIGHTHOST3270.JPG WITH YOUR COMPANY LOGO -->\n");
   	s.append("<Img src=\"graphics/righthost3270.jpg\" border=0 align=\"middle\">\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE WIDTH=80%>\n");
   	s.append("\n");
   	s.append("<!-- 2 of 2: ADD ANY ADDITIONAL INFORMATION FOR THE USER BELOW -->\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</FONT>\n");
   	s.append("<!-- BEGIN KEYBOARD SHORTCUTS DISPLAY -->\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<table ALIGN=\"CENTER\" CELLPADDING=\"1\" CELLSPACING=\"1\" BORDER=\"1\">\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td colspan=2 align=\"center\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("			<b>HOST KEYBOARD SHORTCUTS:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("		<br>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<b>PF1 thu PF12:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		Press the appropriate \"F\" key on your keyboard (F1=PF1, etc.)\n");
   	s.append("		</FONT>		\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<b>PF13 thu PF24:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		Hold SHIFT and press the appropriate \"F\" key (PF13=SHIFT+F1, etc.)\n");
   	s.append("		</FONT>		\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<b>Help:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		Hold CTRL and press x\n");
   	s.append("		</FONT>		\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<b>Reset:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		Hold CTRL and press x \n");
   	s.append("		</FONT>		\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<b>RollUp:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		Hold CTRL and press x\n");
   	s.append("		</FONT>		\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<b>RollDn:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		Hold CTRL and press x\n");
   	s.append("		</FONT>		\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<b>Sys Req:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("	<td>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		Hold CTRL and press x\n");
   	s.append("		</FONT>		\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<TR>\n");
   	s.append("	<td colspan=2 ALIGN=\"CENTER\">\n");
   	s.append("		<!-- THE APPLET IS LOADED HERE -->\n");
   	s.append("		<APPLET code=RightHostPrototype.class width=300 height=70 ALIGN=\"bottom\"></APPLET>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</center>\n");
   	s.append("\n");
   	s.append("<!-- END KEYBOARD SHORTCUTS DISPLAY -->\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: appletloading.html
     */
    private String getappletloadingHTML(int errorStatus)
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>appletloading.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>HTML display during applet loading</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FORM>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("<TEXTAREA rows=20 cols=58 name=html>\n");
	/*
	 * The default applet loading HTML is loaded in from the file usrht.rw
	 */
	try
	    {
		FileInputStream fis = new FileInputStream(adminFile.getParent() + File.separator + "JavaHtml.htm");
		DataInputStream dis = new DataInputStream(fis);
		String line = dis.readLine();
		while(line != null)
		    {
			s.append(line + "\n");
			line = dis.readLine();
		    }
		dis.close();
		fis.close();
	    }
	catch(Exception e)
	    {
		//System.out.println(e.toString());
		error(e.toString());
	    }
   	s.append("</TEXTAREA>\n");
   	s.append("</font>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<INPUT type=hidden value=appletloading name=page>");
   	s.append("<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
   	s.append("\n");
   	s.append("<INPUT type=submit value=\"Restore Default\" id=submit1 name=action>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>About this option</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	This is the HTML displayed in the user's browser when the RightHost 3270 applet is loading.  This\n");
   	s.append("	HTML will also remain displayed in the browser after the applet is loaded.  \n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	You may use any valid HTML to display any appropriate information to the user, including your\n");
   	s.append("	company logo, help desk phone numbers, etc.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Restore Default</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Returns to the default HTML display shipped with RightHost 3270, which outlines keyboard shortcuts\n");
   	s.append("	provided by the applet.\n");
   	s.append("	</FONT>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>	\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: helpabout.html
     */
    private String gethelpaboutHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>helpabout.html</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>Text to display when users select Help->About</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FORM>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("<TEXTAREA rows=3 cols=41 name=helpabout wrap=virtual>\n");
   	s.append(helpabout);
   	s.append("</TEXTAREA>\n");
   	s.append("</font>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<INPUT type=hidden name=page value=helpabout>");
   	s.append("<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>About this option</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Customize the text displayed when users select Help->About in the RightHost\n");
   	s.append("	3270 applet.  Use to guide users to the right source for help, to \"brand\" public Internet\n");
   	s.append("	applications, etc.\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	Example:\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<i>For help with mainframe applications call 800-DataCenter</i>\n");
   	s.append("	<br>\n");
   	s.append("	<i>For help with PC applications call 800-AppHelp</i>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	Keep this text as short as possible.\n");
   	s.append("	</FONT>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>		\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: htmlemulationdefault.html
     */
    private String gethtmlemulationdefaultHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("<HTML> \n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>RightHost 3270-to-HTML emulation</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("<BODY BGCOLOR=\"white\">\n");
   	s.append("\n");
   	s.append("<FORM>\n");
   	s.append("\n");
   	s.append("<center>\n");
   	s.append("\n");
   	s.append("<!-- 1 of 2: REPLACE RIGHTHOST3270.JPG WITH YOUR COMPANY LOGO -->\n");
   	s.append("<Img src=\"graphics/righthost3270.jpg\" border=0 align=\"middle\">\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE WIDTH=80%>\n");
   	s.append("\n");
   	s.append("<!-- 2 of 2: ADD ANY ADDITIONAL INFORMATION FOR THE USER BELOW -->\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<!-- ART: REMOVE BELOW AND REPLACE WITH AUTOMATED LIST OF HOSTS -->\n");
   	s.append("\n");
   	s.append("<table ALIGN=\"CENTER\" CELLPADDING=\"1\" CELLSPACING=\"1\" BORDER=\"1\" WIDTH=\"70%\">\n");
   	s.append("\n");
   	s.append("<TR>\n");
   	s.append("	<TD BGCOLOR=\"#910000\" ALIGN=\"CENTER\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\" COLOR=\"WHITE\">\n");
   	s.append("		<b>CONNECT WITH \"ONE CLICK\" HOSTS:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("<TR>\n");
   	s.append("	<TD ALIGN=\"CENTER\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("		<A HREF=\"autogenerated/hostname1\">Big Box Number 1</a>\n");
   	s.append("		<br>\n");
   	s.append("	</TD>\n");
   	s.append("</tr>\n");
   	s.append("<TR>\n");
   	s.append("	<TD ALIGN=\"CENTER\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<A HREF=\"autogenerated/hostname2\">Big Box Number 2</a>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("<TR>\n");
   	s.append("	<TD ALIGN=\"CENTER\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<A HREF=\"autogenerated/hostname3\">Big Box Number 3</a>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<!-- ART: SUPPORT OPTION BELOW IF \"USERS CAN ENTER HOST ADDRESS MANUALLY\" IS ENABLED -->\n");
   	s.append("\n");
   	s.append("<TR>\n");
   	s.append("	<TD>\n");
   	s.append("		&nbsp;\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<TR>\n");
   	s.append("	<TD BGCOLOR=\"#910000\" ALIGN=\"CENTER\">\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\" COLOR=\"WHITE\">\n");
   	s.append("		<b>CONNECT WITH ALTERNATE HOST:</b>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<TR>\n");
   	s.append("	<TD ALIGN=\"CENTER\">\n");
   	s.append("		<br>\n");
   	s.append("		<FONT FACE=\"ARIAL, HELVETICA\">	\n");
   	s.append("		<INPUT type=text size=20 name=\"hostaddress\"><br>\n");
   	s.append("		<INPUT type=submit value=\"Connect\" id=submit1 name=submit1>\n");
   	s.append("		</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("</tr>\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("</center>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: 3270tohtml.html
     */
    private String get3270tohtmlHTML(int errorStatus)
    {
      	StringBuffer s = new StringBuffer();
   	s.append("\n");
   	s.append("<HTML>\n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>HTML--&gt;3270 Display</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("\n");
   	s.append("<BODY bgcolor=\"#FFFFFF\">\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=+1 COLOR=\"#910000\">\n");
   	s.append("&nbsp;<b>HTML display for HTML --&gt;3270 page.</b>\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FORM>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("<TEXTAREA rows=20 cols=58 name=html>\n");
	/*
	 * The default HTML --> 3270 HTML is loaded in from the file html3270.htm
	 */
	try
	    {
		FileInputStream fis = new FileInputStream(adminFile.getParent() + File.separator + "html3270.htm");
		DataInputStream dis = new DataInputStream(fis);
		String line = dis.readLine();
		while(line != null)
		    {
			s.append(line + "\n");
			line = dis.readLine();
		    }
		dis.close();
		fis.close();
	    }
	catch(Exception e)
	    {
		//System.out.println(e.toString());
		error(e.toString());
	    }
   	s.append("</TEXTAREA>\n");
   	s.append("</font>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");

   	s.append("<INPUT type=submit value=\"Save and Apply\" id=submit1 name=action>\n");
   	s.append("\n");
   	s.append("<INPUT type=hidden value=html3270 name=page>");   	
   	s.append("<INPUT type=submit value=\"Restore Default\" id=submit1 name=action>\n");
   	s.append("\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE>\n");
   	s.append("<table width=\"100%\" border=0 cellspacing=\"6\" cellpadding=\"0\" align=\"left\">\n");
   	s.append("<tr>\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<b>About this option</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	This is the HTML displayed in the user's browser when they select HTML emulation.  You may also\n");
   	s.append("  edit the file &lt;<i>install directory</i>&gt;/adminprops/Html3270.htm in your favorite html editor\n");
   	s.append("  if you prefer not to use this interface.");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	You may use any valid HTML to display any appropriate information to the user, including your\n");
   	s.append("	company logo, help desk phone numbers, etc.  A table listing the available hosts (as specified in\n");
   	s.append("  Client Config --&gt; 3270 Hosts) will automatically be appended to the HTML.\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("<tr>\n");
   	s.append("\n");
   	s.append("	<td valign=\"top\">\n");
   	s.append("\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1 COLOR=\"#910000\">\n");
   	s.append("	<br>\n");
   	s.append("	<b>Restore Default</b>\n");
   	s.append("	</FONT>\n");
   	s.append("	</td>\n");
   	s.append("\n");
   	s.append("	<td>\n");
   	s.append("	<br>\n");
   	s.append("	<FONT FACE=\"ARIAL, HELVETICA\" SIZE=-1>\n");
   	s.append("	Returns to the default HTML display shipped with RightHost 3270.\n");
   	s.append("	</FONT>\n");
   	s.append("	<br>\n");
   	s.append("	<br>\n");
   	s.append("	<A HREF=\"#top\"><IMG SRC=\"graphics/up2.jpg\" height=\"30\" width=\"30\" border=0></a>	\n");
   	s.append("	</td>\n");
   	s.append("</tr>\n");
   	s.append("\n");
   	s.append("</table>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
    }
    /**
     * Returns HTML corresponding to file: nojava.html
     */
    private String getnojavaHTML()
    {
   	StringBuffer s = new StringBuffer();
   	s.append("<HTML> \n");
   	s.append("<HEAD>\n");
   	s.append("	<TITLE>RightHost 3270 Redirect</TITLE>\n");
   	s.append("</HEAD>\n");
   	s.append("<BODY BGCOLOR=\"white\">\n");
   	s.append("\n");
   	s.append("<FORM>\n");
   	s.append("\n");
   	s.append("<center>\n");
   	s.append("\n");
   	s.append("<!-- 1 of 1: REPLACE RIGHTHOST3270.JPG WITH YOUR COMPANY LOGO -->\n");
   	s.append("<Img src=\"graphics/righthost3270.jpg\" border=0 align=\"middle\">\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<HR SIZE=1 NOSHADE WIDTH=80%>\n");
   	s.append("\n");
   	s.append("\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\" COLOR=\"#910000\" SIZE=+1>\n");
   	s.append("<B>ERROR</b>\n");
   	s.append("</FONT>\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("<FONT FACE=\"ARIAL, HELVETICA\">\n");
   	s.append("Your browser does not support Java emulation and there are no<br>\n");
   	s.append("HTML connections defined on this server.\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("It is not possible to connect you to a host with your <br>\n");
   	s.append("current browser configuration.\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("Please contact the network administrator for assistance or<br>\n");
   	s.append("install a browser with Java support.\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("Netscape's latest browser may be <A HREF=\"http://cgi.netscape.com/cgi-bin/upgrade.cgi\">downloaded here</a>.\n");
   	s.append("<br>\n");
   	s.append("<br>\n");
   	s.append("\n");
   	s.append("Microsoft's latest browser\n");
   	s.append("may be <A HREF=\"http://www.microsoft.com/ie/download\">downloaded here</a>.\n");
   	s.append("</FONT>\n");
   	s.append("\n");
   	s.append("</center>\n");
   	s.append("\n");
   	s.append("</BODY>\n");
   	s.append("</HTML>\n");
   	return new String(s);
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
	if(!connectToDest())//this fucks me up every time... remember
	    //that this if statement actually fires the connectToDest()
	    //method.
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
//class ConnectionThread implements Runnable
//{
//	public ConnectionThread(Socket newSocket, ConnectionMonitor cm, String host, int hostPort, boolean encrypt, SessionServer ss)
//  {
//  	this.newSocket = newSocket;
//  	this.cm = cm;
//  	this.host = host;
//  	this.hostPort = hostPort;
//  	this.encryption = encrypt;
//  	this.ss = ss;
//  }
//  	
//  public void run()
//  {		
//  	new Connection(newSocket, cm, host, hostPort, encryption, ss);
//  }
//  private Socket newSocket;
//  private ConnectionMonitor cm;
//  private String host;
//  private int hostPort;
//  private boolean encryption;
//  private SessionServer ss;
//}
