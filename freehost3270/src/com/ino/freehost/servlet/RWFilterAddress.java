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

public class RWFilterAddress
{
   public RWFilterAddress(String displayName)
   {
      this.displayName = displayName;
      address = parseName(displayName);
   }
   private int[] parseName(String s)
   {
      int b[] = {0, 0, 0, 0};
      StringTokenizer st = new StringTokenizer(s, ".");
      int i = 0;
      while(st.hasMoreTokens())
      {
         String tmpString = st.nextToken();
         if(tmpString.equals("*"))
            b[i] = 0;
         else
            b[i] = Integer.parseInt(tmpString);
         i++;
      }
      return b;
   }
   public String getDisplayName()
   {
      return displayName;
   }
   public void setDisplayName(String s)
   {
      displayName = s;
   }
   public int[] getAddress()
   {
      return address;
   }
   public boolean equals(Object o)
   {
      if(!(o instanceof RWFilterAddress))
         return false;
      RWFilterAddress filter = (RWFilterAddress)o;
      int client[] = filter.getAddress();
      for(int i = 0; i < 4; i++)
      {
         if(address[i] > 0)
         {
         		if(client[i] != address[i])
         			return false;
         		continue;
         }
         if((client[i] | address[i])!= client[i])
            return false;
      }
      return true;
   }
   private int address[];
   private String displayName;
}
