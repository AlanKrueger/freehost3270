package net.sf.freehost3270.gui;

import java.awt.Color;

public interface ColorMap {
	static final int CURSOR_FOREGROUND = 0;
	static final int CURSOR_BACKGROUND = 1;
	static final int BOLD = 2;
	static final int FIELD_BACKGROUND = 3;
	
	public Color translateColor(int c);
}