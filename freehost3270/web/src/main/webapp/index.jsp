<?xml version="1.0"?>
<!--

Presents a user the page with a corresponding 'applet' tag. This tag
passess configuration parameters to the applet being launched.

Using plain 'applet' XHTML in favor of jsp:applet tag, because jsp:applet
generated invalid XHTML code.

-->
<jsp:root xmlns:jsp="http://java.sun.com/JSP/Page"
	  xmlns="http://www.w3.org/1999/xhtml"
	  version="1.2">
  <jsp:directive.page language="java" session="false" contentType="text/html" />
  <jsp:scriptlet>
    Integer port = new Integer(0);
    String avail_hosts = "";
    try {
	String key = net.sf.freehost3270.web.ProxyLauncherServlet.PROXY_PORT_KEY;
	javax.naming.InitialContext ctx = new javax.naming.InitialContext();
	port = (Integer) ctx.lookup("java:comp/env/" + key);
	avail_hosts = (String) ctx.lookup("java:comp/env/freehost3270/avail-hosts");
    } catch (javax.naming.NamingException e) {
	e.printStackTrace();
    }
  </jsp:scriptlet>
  <html>
    <head>
      <title>FreeHost3270 terminal emulation applet</title>
      <link rel="stylesheet" href="default.css" type="text/css"/>
    </head>
    <body onload="doResize()">
        <script type="text/javascript">
//<![CDATA[
 function doResize() {
 // adjusts the size of the place taken by the applet on the page
 // to the preferred size of the applet.
   var applet = document.applets["FreeHostApplet"];
   var dim = applet.getPreferredSize();
   dimX = dim.getWidth();
   dimY = dim.getHeight();
   applet.width = dim.getWidth();
   applet.height = dim.getHeight();;
 }
//]]>
</script>
      <h2>FreeHost3270 terminal emulation applet</h2>
      <p><a href="#keylist">Hot keys description</a></p>
      <applet code="net.sf.freehost3270.applet.FreeHostApplet"
	      id="FreeHostApplet" name="FreeHostApplet"
	      archive="freehost3270-client-0.2-CURRENT.jar,freehost3270-gui-0.2-CURRENT.jar,freehost3270-applet-0.2-CURRENT.jar"
	      width="620" height="460" align="middle"
	      jreversion="1.4">
	&lt;param name=&quot;avail-hosts&quot; value=&quot;<jsp:expression>avail_hosts</jsp:expression>&quot; /&gt;
	&lt;param name=&quot;proxy-port&quot; value=&quot;<jsp:expression>port</jsp:expression>&quot; /&gt;
	In order to use <span class="prodname">freehost3270</span>
	terminal emulator applet, you have to have the Java plugin
	installed and configured in your system.
      </applet>

      <!-- Comment this `div' out if hot keys description is not
           desired. -->
      <div class="help">
	<jsp:include page="help.jsp"/>
      </div>
    </body>
  </html>
</jsp:root>
