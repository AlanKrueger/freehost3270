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


package com.ino.freehost.html3270;

import java.util.*;
import javax.servlet.http.*;

public class RWHTTPSessionContext implements HttpSessionContext
{
	public RWHTTPSessionContext()
	{
		h = new Hashtable();
	}
	public Enumeration getIds()
	{
		return h.keys();
	}
	public HttpSession getSession(String s)
	{
		HttpSession ret = null;
		try
		{
			ret = (HttpSession)h.get(s);
		}
		catch(NullPointerException e)
		{
			ret = new RWHTTPSession(this);
			h.put(ret.getId(), ret);
		}
		return ret;
	}
	protected Hashtable h;
}
		
