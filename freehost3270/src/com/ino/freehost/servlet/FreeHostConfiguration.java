package com.ino.freehost.servlet;

import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.io.File;
import java.io.FileInputStream;
import org.apache.log4j.Category;
import com.ino.freehost.client.Host;

/**
 * A singleton that represents the FreeHost 3270 Configuration
 * @author Art Gillespie
 */

public class FreeHostConfiguration {

    /**
     * reference to singleton instance
     */
    private static FreeHostConfiguration _instance;
    /**
     * The full path to the freehost.properties configuration file 
     */
    private String _configfile;
    /**
     * Logging category
     */
    private Category _cat;
    /**
     * Admin's email notification address
     */
    private String _adminEmail;
    /**
     * SMTP Server used for sending notification emails
     */
    private String _smtpserver;
    /** 
     * Should the server notify the admin and when
     */
    protected boolean _emailNotifyNotResponding;
    protected boolean _emailNotifyHostNotResponding;
    protected boolean _emailNotifyXSessionsAreActive;
    protected int     _emailNotifyXSessions;
    /**
     * Logfile
     */
    private File _logFile;
    /**
     * The hostname we're running on.
     */
    private String _sessionServerHost;
    /**
     * The port the session server will listen on
     */
    private int _sessionServerPort;
    /**
     * The ip filtering mode:
     * 0 = none
     * 1 = all except
     * 2 = only
     */
    private int _filterMode;//0 = none; 1 = allExcept; 2 = only
    /**
     * List of hosts to filter
     */
    private Vector _filterList;
    /**
     * Lists of host to connect to
     */
    private String _hostList;
    /**
     * Can clients connect to arbitrary hosts (not in _hostList)?
     */
    private boolean _manualEntry;
    /**
     * String for help about dialog
     */
    private String _helpabout;
    
    /**
     * Standard singleton implementation.  Private constructor;
     */
    private FreeHostConfiguration () {
	_cat = Category.getInstance("freehost.Configuration");
    }

    /*
     * Getters for all of the relevant properties
     */
    public boolean getEmailHostNotResponding () { return _emailNotifyNotResponding; }
    public String getAdminEmail () { return _adminEmail; }
    public String getSmtpServer () { return _smtpserver; }
    public String getSessionServerHost () { return _sessionServerHost; }
    public int getSessionServerPort () { return _sessionServerPort; }
    public int getFilterMode () { return _filterMode; }
    public Enumeration getFilterList () { return _filterList.elements(); }
    public String getHostList () { return _hostList; }
    public boolean getManualEntry () { return _manualEntry; }
    public String getHelpAbout () { return _helpabout; }

    /**
     * Returns the global instance of FreeHostConfiguration
     */
    public static FreeHostConfiguration getInstance() {
	if ( _instance == null) {
	    _instance = new FreeHostConfiguration();
	    return _instance;
	} else {
	    return _instance;
	} 
    }
    /**
     * Initializes the configuration with the supplied root path.
     * @param rootpath The full path to the freehost.properties
     * configuration file
     */
    public void init (String configfile) 
    throws ConfigurationException {
	_configfile = configfile;
	_cat.info("Configuring FreeHost 3270 with: " + configfile);
	File f = new File(configfile);
	if ( ! f.exists() ) {
	    _cat.error("No such configuration file: " + configfile);
	    throw new ConfigurationException("No Such File: " + configfile);
	} 
	Properties config = null;
	try {
	    FileInputStream fis = new FileInputStream(f);
	    config = new Properties();
	    _cat.info("Loading: " + configfile);
	    config.load(fis);
	    fis.close();
	} catch (Exception anException) {
	    _cat.error("Configuration Error while reading config file: " + 
		       anException.getMessage());
	    throw new ConfigurationException("Configuration Error: " + 
					     anException.getMessage());
	}
	_adminEmail = config.getProperty("adminEmail");
	_smtpserver = config.getProperty("smtpserver");
	_emailNotifyHostNotResponding = 
	    Boolean.valueOf(config.getProperty("emailNotifyHostNotResponding")).booleanValue();
	_emailNotifyXSessionsAreActive = 
	    Boolean.valueOf(config.getProperty("emailNotifyXSessionsAreActive")).booleanValue();
	_emailNotifyXSessions = 
	    Integer.parseInt(config.getProperty("emailNotifyXSessions"));
	//_logFile = new File(config.getProperty("logFile"));
	_sessionServerHost = config.getProperty("sessionServerHost");
	_sessionServerPort = 
	    Integer.parseInt(config.getProperty("sessionServerPort"));
	_filterMode = 
	    Integer.parseInt(config.getProperty("filterMode"));
	_filterList = getFilterList(config.getProperty("filterList"));
	_hostList = config.getProperty("hostList");
	_manualEntry = 
	    Boolean.valueOf(config.getProperty("manualEntry")).booleanValue();
	_helpabout = config.getProperty("helpabout");
	
	
	    
	
    }

    private Vector getFilterList(String in) {
	Vector ret = new Vector();
	if(in == null)
	    return ret;
	StringTokenizer st = new StringTokenizer(in, "|");
	while(st.hasMoreTokens()) {
		ret.addElement(new RWFilterAddress(st.nextToken()));
	}
	return ret;
    }
    private Vector getHostList(String hostList) {
	Vector ret = new Vector();
	StringTokenizer st2 = new StringTokenizer(hostList, "|");
	while(st2.hasMoreTokens()) {
		String hostName = st2.nextToken();
		int hostPort = Integer.parseInt(st2.nextToken());
		String friendlyName = st2.nextToken();
		Host h = new Host(hostName, hostPort, friendlyName);
		ret.addElement(h);
	}
	return ret;
    }



}
