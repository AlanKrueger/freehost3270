package net.sf.freehost3270.gui;

import java.awt.event.KeyEvent;

public interface KeyMap {
	public KeyEvent[] translate(KeyEvent ke);
}
