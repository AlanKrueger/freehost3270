package net.sf.freehost3270.gui;

import net.sf.freehost3270.client.IsProtectedException;
import net.sf.freehost3270.client.RW3270;
import net.sf.freehost3270.client.RWTnAction;

import java.util.Enumeration;
import java.util.Vector;

import java.text.NumberFormat;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * DefaultTerminalModel is the datamodel class for the JTerminalScreen widget.
 * The state of this object is what the widget will control, and render.
 * No state beyond presentation and layout is kept within the JTerminalScreen.
 * 
 * @author Bryan.Varner bman@varnernet.com
 */
public class DefaultTerminalModel implements RWTnAction {
	RW3270 rw;
	NumberFormat rowFormat;
	
	String currentPosition;
	String message;
	String status;
	
	Vector listeners;
	
	public DefaultTerminalModel() {
		this(2);
	}
	
	public DefaultTerminalModel(int modelType) {
		listeners = new Vector();
		rw = new RW3270(modelType, this);
		
		rowFormat = NumberFormat.getInstance();
		rowFormat.setMinimumIntegerDigits(3);
		
		setCursor(0);
		message = "";
		status = "";
	}
	
	public RW3270 getEmulator() {
		return rw;
	}
	
	public void beep() {
		TerminalEvent te = new TerminalEvent(this, TerminalEvent.BEEP);
		fireEvent(te);
	}
	
	public void broadcastMessage(String message) {
		TerminalEvent te = new TerminalEvent(this, TerminalEvent.BROADCAST_MESSAGE);
		fireEvent(te);
	}
	
	public void cursorMove(int oldPos, int newPos) {
		TerminalEvent te = new TerminalEvent(this, TerminalEvent.CURSOR_MOVE);
		te.setPositions(oldPos, newPos);
		fireEvent(te);
	}
	
	public void incomingData() {
		System.out.println("incomingData");
		TerminalEvent te = new TerminalEvent(this, TerminalEvent.DATA_RECEIVED);
		fireEvent(te);
	}
	
	public void status(int msg) {
		switch (msg) {
			case RWTnAction.X_WAIT: {
					status = "X-WAIT";
					break;
			}
			
			case RWTnAction.READY: {
					status = "Ready";
					break;
			}
			
			case RWTnAction.DISCONNECTED_BY_REMOTE_HOST: {
					message = "Your connection to the FreeHost 3270 Session Server was lost or could not be established. " +
                "Please try your session again, and contact your system administrator if the problem persists.";
					break;
			}
		}
		TerminalEvent te = new TerminalEvent(this, TerminalEvent.STATUS_CHANGED);
		fireEvent(te);
	}
	
	public void addTerminalEventListener(TerminalEventListener tel) {
		listeners.add(tel);
	}
	
	public void removeTerminalEventListener(TerminalEventListener tel) {
		listeners.remove(tel);
	}
	
	/**
	 * Called by JTerminal to update the model on cursor moves
	 */
	public void setCursor(int position) {
		int row = position / rw.getCols();
		int col = position - (row * rw.getCols());
		
		this.currentPosition = rowFormat.format(row + 1) + "/" + 
		                       rowFormat.format(col + 1);
	}
	
	public void connect(String host, int port,
	                    String host3270, int port3270, 
					boolean encryption)
	{
		if (host == null) {
			return;
		}
		
		message = "";
		status = "Connecting";
		fireEvent(new TerminalEvent(this, TerminalEvent.STATUS_CHANGED));
		rw.connect(host, port, host3270, port3270, encryption);
	}
	
	public void connect(String host, int port) 
		throws IOException, UnknownHostException 
	{
		if (host == null) {
			return;
		}
		
		message = "";
		status = "Connecting " + host + ":" + port;
		fireEvent(new TerminalEvent(this, TerminalEvent.STATUS_CHANGED));
		rw.connect(host, port);
	}
	
	public void connect(String host, int port, boolean encryption) 
		throws IOException, UnknownHostException
	{
		rw.setEncryption(encryption);
		this.connect(host, port);
	}
	
	public void disconnect() {
		rw.disconnect();
		status = "Disconnected";
		fireEvent(new TerminalEvent(this, TerminalEvent.STATUS_CHANGED));
	}
	
	public String getCurrentPosition() {
		return this.currentPosition;
	}
	
	/**
	 * Gets the current Status Message
	 */
	public String getStatus() {
		return this.status;
	}
	
	// TODO: The rest of this stuff....
	public boolean tooManyConnections() {
		return false;
	}
	
	public boolean showMessage() {
		return !message.equals("");
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public int getMessageNumber() {
		return 0;
	}
	
	/**
	 * Internally fires TerminalEvents to all registered Listeners
	 */
	private void fireEvent(TerminalEvent te) {
		Enumeration listEnu = listeners.elements();
		while (listEnu.hasMoreElements()) {
			TerminalEventListener handler = 
					(TerminalEventListener)listEnu.nextElement();
			handler.terminalEventReceived(te);
		}
	}
}