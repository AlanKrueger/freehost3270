package com.ino.freehost.html3270;

import javax.servlet.http.*;
import java.util.*;

public class RWHTTPSession implements HttpSession
{
    public Object getAttribute(String key) {
	return null;
    }
    public Enumeration getAttributes() {
	return null;
    }
    public Enumeration getAttributeNames() {
	return null;
    }
    public int getMaxInactiveInterval() {
	return 3000;
    }
    public void removeAttribute(String key) {
    }
    public void setAttribute(String key, Object val) {
    }
    public void setMaxInactiveInterval(int interval) {
    }
    
	public RWHTTPSession(RWHTTPSessionContext context)
	{
		this.context = context;
		Random r = new Random();
		id = Integer.toString(r.nextInt());
		h = new Hashtable();
		creationtime = System.currentTimeMillis();
		lastaccessedtime = System.currentTimeMillis();
		isNew = true;
	}
	public String getId()
	{
		return id;
	}
	public HttpSessionContext getSessionContext()
	{
		return context;
	}
	public long getCreationTime()
	{
		return creationtime;
	}
	public long getLastAccessedTime()
	{
		return lastaccessedtime;
	}
	public void setLastAccessedTime()
	{
		lastaccessedtime = System.currentTimeMillis();
	}
	public void invalidate()
	{
		RWHtml3270 rw = (RWHtml3270)h.get("3270Session");
		rw.disconnect();
		context.h.remove(this);
	}
	public void putValue(String s, Object o)
	{
		h.put(s, o);
	}
	public Object getValue(String s)
	{
		return h.get(s);
	}
	public void removeValue(String s)
	{
		h.remove(id);
	}
	public String[] getValueNames()
	{
		String ret[] = new String[h.size()];
		Enumeration e = h.keys();
		int i = 0;
		while(e.hasMoreElements())
		{
			ret[i] = (String)e.nextElement();
			i++;
		}
		return ret;
	}
	public boolean isNew()
	{
		return isNew;
	}
	public void isNew(boolean b)
	{
		isNew = b;
	}
	private String id;
	private Hashtable h;
	private RWHTTPSessionContext context;
	private long creationtime, lastaccessedtime;
	private boolean isNew;
}
		
