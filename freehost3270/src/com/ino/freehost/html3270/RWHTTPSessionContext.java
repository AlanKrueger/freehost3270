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
		
