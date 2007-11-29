package net.sf.freehost3270.gui;

import java.awt.AWTEvent;

public class TerminalEvent extends AWTEvent {
	public static final int BEEP = 1 + AWTEvent.RESERVED_ID_MAX;
	public static final int BROADCAST_MESSAGE = 2 + AWTEvent.RESERVED_ID_MAX;
	public static final int CURSOR_MOVE = 3 + AWTEvent.RESERVED_ID_MAX;
	public static final int DATA_RECEIVED = 4 + AWTEvent.RESERVED_ID_MAX;
	public static final int STATUS_CHANGED = 5 + AWTEvent.RESERVED_ID_MAX;
	
	private int oldPosition;
	private int newPosition;
	private int status;
	
	public TerminalEvent(Object source, int id) {
		super(source, id);
	}
	
	public void setPositions(int old, int n) {
		this.oldPosition = old;
		this.newPosition = n;
	}
	
	public int getOldCursorPosition() {
		return oldPosition;
	}
	
	public int getNewCursorPosition() {
		return newPosition;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
}
