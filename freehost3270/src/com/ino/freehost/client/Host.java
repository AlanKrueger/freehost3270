package com.ino.freehost.client;

public class Host
{
   public Host(String hostName, int port, String friendlyName)
   {
      this.hostName = hostName;
      this.port = port;
      this.friendlyName = friendlyName;
   }
   public String hostName;
   public int port;
   public String friendlyName;
   public boolean isDefault;
}
