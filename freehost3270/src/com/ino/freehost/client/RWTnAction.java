package com.ino.freehost.client;
/**
 * This interface provides a callback to consumers from the 
 * RWTelnet oject.  When developing a 3270 client using the
 * RightHost APIs, you must implement this interface and pass
 * it the RW3270 Object when it's instantiated.  It provides
 * several important methods that the 3270 engine calls to
 * inform the implementation of 3270 events.
 * @see com.ino.freehost.client.RW3270
 * 
 */
public interface RWTnAction
{
	 /**
	  * This method is called by the 3270 engine to communicate status
	  * changes to the client.  The possible values for int are outlined
	  * in the constants for this field.
	  * @param msg The status message:
	  *		<UL>
	  *			<LI>RWtnAction.DISCONNECTED_BY_REMOTE_HOST: This status message indicates
	  *					that the connection to the host was lost.
	  *			<LI>RWTnAction.CONNECTION_ERROR:  This status message that there was a
	  *					problem connecting to the SessionServer or that the SessionServer had
	  *					trouble connecting to the 3270 host.
	  *			<LI>RWTnAction.X_WAIT: This status message means the 3270 engine is
	  *					waiting for a response from the 3270 host.
	  *			<LI>RWTnAction.READY:  This status message means the 3270 engine is
	  *					ready for input from the client implementation.
	  */
   public abstract void status(int msg);
   /**
    * This method is called by the 3270 engine when it has received new
    * data.  This method would usually call a paint() method or other display
    * update.  Note:  this method is useful when the client implementation is
    * stateful.  If the 3270 engine does not have direct access to the user's
    * display, the RW3270.waitForNewData() method will suspend the calling thread
    * until new data arrives.
    */
   public abstract void incomingData();
   /**
    * This method is called when the cursor position (current buffer position)
    * of the 3270 engine is changed by host commands or by user input.  When drawing
    * the cursor to the client's display, use this method to fire cursor draw logic.
    * @param oldPos The previous cursor position.  This is useful for 'undrawing' the
    *								cursor before drawing it in its new position.  The position is returned
    *								as an integer value between 0 (first character position) and <code>
    *      					length of dataBuffer - 1</code> (last character position).  For example
    *								using 3270 Model 2.  The value would be between 0 and 1919.
    * @param newPos The new (current) cursor position.  This is useful for drawing the
    *								new cursor position.  
    */
   public abstract void cursorMove(int oldPos, int newPos);
   /**
    * This method is called when the SessionServer admin sends a broadcast message to
    * all currently connected clients.  If you're implementing Broadcast Messaging in
    * your client, this method should paint <code>String msg</code> to the client's screen.
    * @param msg The broadcast message from the system administrator.
    */
   public abstract void broadcastMessage(String msg);
   /**
    * This method is called when the host application sends the beep command.  It should
    * fire some sort of audible alarm on the client's system.  For example:
    * <code>
    * 	public void beep()
    *		{
    *				Toolkit.getDefaultToolkit().beep();
    *		}
    * </code>
    */
   public abstract void beep();
   /**
    * Status constant passed to <code>status()</code> indicating that the
    * client's connection to the host has been closed.
    */
   static final int DISCONNECTED_BY_REMOTE_HOST       = 0;
   /**
   	* Status constant passed to <code>status()</code> indicating that there
   	* was a problem connecting to the 3270 host or SessionServer
   	*/
   static final int CONNECTION_ERROR                  = 1;
   /**
    * Status constant passed to <code>status()</code> indicating that the 3270
    * engine is waiting for a response from the 3270 host.
    */
   static final int X_WAIT                            = 2;
   /**
    * Status constant passed to <code>status()</code> indicating that the 3270
    * engine is ready for user input
    */
   static final int READY                             = 3;
}
