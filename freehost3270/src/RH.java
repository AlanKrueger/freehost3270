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

import java.applet.*;
import java.util.*;

import com.ino.freehost.client.Host;

public class RH extends Applet
{
  public static void main(String args[])
  {}  
   public void init()
   {

        String available = getParameter("available");
        Hashtable h = new Hashtable();
        StringTokenizer st = new StringTokenizer(available, "|");
        while(st.hasMoreElements())
       {
         String hostName = st.nextToken();
         int hostPort = Integer.parseInt(st.nextToken());
         String friendlyName = st.nextToken();
         h.put(friendlyName, new Host(hostName, hostPort, friendlyName));
         }
       rht = new RHTest(getParameter("RightHostServer"), Integer.parseInt(getParameter("RightHostPort")), getParameter("DefaultHost"), h, null, this);
   }
   public void stop()
   {
   		rht.disconnect();
   }
   public void destroy()
   {
   		rht.disconnect();
   }
   private RHTest rht;
}
