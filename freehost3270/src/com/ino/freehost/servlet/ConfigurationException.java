package com.ino.freehost.servlet;

public class ConfigurationException
    extends Exception {
    public ConfigurationException (String msg) {
	super(msg);
    }
    public ConfigurationException () {
	super();
    }
}
