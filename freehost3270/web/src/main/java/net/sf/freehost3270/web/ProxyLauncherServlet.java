/*
 * FreeHost3270 a suite of terminal 3270 access utilities.
 * Copyright (C) 1998, 2001  Art Gillespie
 * Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *                        Project Contributors.
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
 */


package net.sf.freehost3270.web;

import net.sf.freehost3270.proxy.Proxy;

import java.io.IOException;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Starts and halts Freehost3270 Proxy service. Upon servlet initialization
 * starts proxy service, and halts it upon servlet disposal. Suggests, that
 * it will get a port to bind the Proxy service to as a servlet
 * initialization parameter. The parameter suggested parameter key is
 * <code>proxy-port</code>.
 *
 * @see net.sf.freehost3270.proxy.Proxy
 * @since 0.2
 */
public class ProxyLauncherServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(ProxyLauncherServlet.class.getName());

    /** Configuration parameter key that specifies proxy service port. */
    public static final String PROXY_PORT_KEY = "freehost3270/proxy-port";
    private Thread proxyThread = null;

    /**
     * Halts proxy service thread.
     */
    public void destroy() {
        if (proxyThread != null) {
            log.info("halting proxy thread");
            proxyThread.interrupt();
        } else {
            log.info("proxy thread is null, nothing to halt");
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        // print proxy service state summary
    }

    /**
     * Configures and starts proxy service thread.
     *
     * @param config DOCUMENT ME!
     *
     * @throws ServletException DOCUMENT ME!
     */
    public void init(ServletConfig config) throws ServletException {
        try {
            InitialContext ctx = new InitialContext();
            Integer port = (Integer) ctx.lookup("java:comp/env/" +
                    PROXY_PORT_KEY);
            int portNum = port.intValue();

            // got port number config parameter, start the proxy
            log.info("trying to start the proxy server");
            proxyThread = new Thread(new Proxy(portNum, false));
            proxyThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            log.severe("failed to initialize proxy service: " +
                e.getMessage());
            throw new ServletException(e);
        } catch (NamingException e) {
            log.severe(PROXY_PORT_KEY +
                " - failed to get from initial context");
            throw new ServletException("failed to configure proxy");
        }
    }
}
