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
