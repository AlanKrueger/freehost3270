/*
 * 5/12/98 - Added connect(String host, int port) to facilitate
 * the 3270 Servlet Development Toolkit.  This method enables
 * implementations to connect directly to a TN3270 host
 */
package com.ino.freehost.client;

import java.net.*;
import java.io.*;
import java.math.*;


public class RWTelnet implements Runnable
{
   
    /**
     * @param rw - the Parser for the incoming data stream
     *
     * @param tn3270Model - the tn3270 model number corresponding to this
     *    session.
     */
    public RWTelnet(RWTn3270StreamParser rw, int tn3270Model)
    {                                      
	this.rw = rw;
	this.tn3270Model = tn3270Model;
	buffer3270 = new short[4096];
	buffer3270Len = 0;
	subOptionBuffer = new short[2];
	subOptionBufferLen = 0;
	tnState = TN_DEFAULT;
	keyCounter = 0;
	doHistory = new boolean[3];
	willHistory = new boolean[3];
    }
    /**
     * @param   host Hostname or IP address of the SessionServer/Host
     * @param   port the port number the SessionServer/Host is running on
     * @param   host3270 the Hostname or IP address of the 3270Host (if using SessionServer)
     */
    protected void connect(String host, int port, String host3270, int port3270)
    {
	try
	    {
		tnSocket = new Socket(host, port);
		is = tnSocket.getInputStream();
		os = tnSocket.getOutputStream();
		//eos = new EncryptedOutputStream(rwc, os);
		//eis = new EncryptedInputStream(rwc, is); 
		if(true)
		    {
			//eos.setEncrypting(false);
			//eis.setDecrypting(false);
		    }        		
		if(host3270 != null)
		    {
			//System.out.println("Sending host: " + host3270);
			int i = 0;
			byte byteBuf[] = new byte[host3270.length() + 2];
			for(; i < host3270.length(); i++)
			    {
				byteBuf[i] = (byte) host3270.charAt(i);
			    }
			byteBuf[host3270.length()] = (byte)0xCC;
			byteBuf[host3270.length() + 1] = (byte)port3270;
			os.write(byteBuf);
			os.flush();
			//System.out.println("Sent host");
		    }
		else
		    {
			byte byteBuf[] = new byte[1];
			byteBuf[0] = (byte)0xCC;
			os.write(byteBuf);
		    }
		sessionThread = new Thread(this);
		sessionThread.start();
		//System.err.println("Connected...");
	    }
	catch(Exception e)
	    {
		System.err.println(e.getMessage());//""
	    }
    }
    /**
     * Connect method for implementations not using SessionServer
     */
    //added 5/12/98 to facilitate packaging of the 3270 Servlet Developer's Toolkit
    protected void connect(String host, int port)
    {
	try
	    {
		tnSocket = new Socket(host, port);
		is = tnSocket.getInputStream();
		os = tnSocket.getOutputStream();
		sessionThread = new Thread(this);
		sessionThread.start();
	    }
	catch(Exception e)
	    {
		//System.err.println(e.getMessage());
	    }
    }

    /**
     * Disconnects the current session
     */
    protected void disconnect()
    {
	if(tnSocket == null)
	    return;
	try
	    {
		sessionThread.stop();
		is.close();
		os.close();
		tnSocket.close();
		tnSocket = null;
		os = null;
		is = null;
		willHistory = new boolean[3];
		doHistory = new boolean[3];
		//System.out.println("Disconnected ...");
	    }
	catch(Exception e)
	    {
		System.err.println(e.getMessage());
	    }
    }
    /*
     * receiveMessage - this method gets called when a broadcast message is received
     */

    protected void receiveMessage(short netBuf[])
    {
        char msg[] = new char[netBuf.length];
        for(int i = 2; i < netBuf.length; i++)
	    {
		msg[i - 2] = (char)netBuf[i];
	    }
        rw.client.broadcastMessage(new String(msg).trim());
        //j.paintWindowMessage();
    }   
    /**
     * Checks the input stream for commands and routes the stream appropriately
     * standard data is stored in the <code>buffer3270</code> array and passed to
     * the <B>RWTelnetAction<B> interface's <code>incomingData(buf, int)</code> method
     * when an EOR (End-of-record) byte is detected.  Other telnet commands 
     * (WILL WONT DO DONT IAC) are handled in accordance to RFC 845
     */
    private void parseData()
	throws IOException
    {
	short curr_byte;
	//this if clause traps the inputStream if it is a broadcast message
    	if(inBuf[0] == IAC && inBuf[1] == BROADCAST)
	    {
		receiveMessage(inBuf);
		inBufLen = 0;
		return;
	    }
	for(int i = 0; i < inBufLen; i++)
	    {
		curr_byte = inBuf[i];
		switch(tnState)
		    {
		    case TN_DEFAULT:
			if(curr_byte == IAC)
			    {
				tnState = TN_IAC;
			    }
			else
			    {
				try
				    {
					buffer3270[buffer3270Len++] = curr_byte;
				    }
				catch(ArrayIndexOutOfBoundsException ee)
				    {
					//System.err.println("buffer3270 size: " + buffer3270.length + " len: " +buffer3270Len);
					return;
				    }
			    }
			break;
		    case TN_IAC:
			switch(curr_byte)
			    {
			    case IAC:
				//Two IACs in a row means this is really a single occurrence
				//of byte 255 (0xFF).  (255 is its own escape character)
				buffer3270[buffer3270Len++] = curr_byte;
				//Since it wasn't really an IAC, reset the tnState to
				//default:
				tnState = TN_DEFAULT;
				break;
			    case EOR:
				//Done with this data record, send to Implementation
				//via RWTnAction interface
				rw.parse(buffer3270, buffer3270Len);
				buffer3270Len = 0;
				tnState = TN_DEFAULT;
				break;
			    case WILL:
			    case WONT:
			    case DO:
			    case DONT:
				tnCommand = curr_byte;
				tnState = TN_CMD;
				break;
			    case SB:
				//System.err.println("Sub-option: " + subOptionBufferLen);
				subOptionBufferLen = 0;
				tnState = TN_SUB_CMD;
				break;
			    }
			break;
		    case TN_CMD:
			//System.out.println("CMD...");
			doTnCommand(tnCommand, curr_byte);
			//System.out.println("did command...");
			tnState = TN_DEFAULT;
			break;
		    case TN_SUB_CMD:
			if(curr_byte != SE)
			    {
				if(subOptionBufferLen < 2)
				    subOptionBuffer[subOptionBufferLen++] = curr_byte;
				break;
			    }
			tnState = TN_DEFAULT;
			if(subOptionBufferLen != 2)
			    break;
			if(subOptionBuffer[0] != TERMINAL_TYPE)
			    break;
			if(subOptionBuffer[1] != SEND_OPTION)
			    break;
			doTerminalTypeCommand();
			break;
		    }
	    }
    }
    /**
     * 
     */
    private int readSocket()
	throws IOException
    {
	inBufLen = 2048;
	inBuf = new short[inBufLen];
	//Since inputstreams require a byte array as
	//a parameter (and not a short[]), we have to
	//use this temporary buffer to store the results.
	byte tmpByteBuf[] = new byte[inBufLen];
	int bytes_read = is.read(tmpByteBuf, 0, inBufLen);
	//System.out.println("Done... ");
	//Convert all negative numbers
	for(int i = 0; i < inBufLen; i++)
	    {
		//Cast our results from the byte array to our
		//short array, inBuf[].  If the byte is less than
		//0 (negative), add 256.
		if((inBuf[i]=tmpByteBuf[i]) < 0)
		    inBuf[i] += 256;
	    }
	//System.out.println("Bytes read: " + bytes_read);
	return bytes_read;
    }
    /**
     * This method provides outbound communication to the Telnet host.
     * @param out[] an array of shorts, representing the data to be sent
     *       to the host.
     * @param outLen the number of valid bytes in the out array
     */
    protected void sendData(short out[], int outLen)
	throws IOException
    {
	//System.out.println("Sending...");
	//Since EncryptedOutputStream needs a byte
	//array as input (as opposed to a short[]), we
	//convert our array to this temporary byte array
	//with two extra bytes for the telnet codes.
	//System.out.println(outLen);
	byte tmpByteBuf[] = new byte[outLen + 2];
	for(int i = 0; i < outLen; i++)
	    {
		tmpByteBuf[i] = (byte)out[i];
	    }
	//add the is a command telnet command
	tmpByteBuf[outLen] = (byte)IAC;
	//add the end of record telnet command
	tmpByteBuf[outLen + 1] = (byte)EOR;
	//write the data out to the EncryptedOutputStream
	os.write(tmpByteBuf, 0, tmpByteBuf.length);
	//System.out.println("Sent " + tmpByteBuf.length + " bytes");
	//for(int i = 0; i < tmpByteBuf.length; i++)
	//System.out.print(Integer.toHexString(tmpByteBuf[i]) + " ");
    }
    synchronized protected void setSessionData(String key, String value)
    {
	System.out.println("SessionData Key: " + key + " Value: " + value);
	byte[] keyByte = charToByte(key.toCharArray());
	byte[] valueByte = charToByte(value.toCharArray());
	byte[] outData = new byte[keyByte.length + valueByte.length + 4];
	outData[0] = (byte)0xCC;
	outData[1] = (byte)0xCC;
	System.arraycopy(keyByte, 0, outData, 2, keyByte.length);
	outData[keyByte.length+2] = (byte)0xCC;
	System.arraycopy(valueByte, 0, outData, keyByte.length + 3, valueByte.length);
	outData[keyByte.length+valueByte.length+3] = (byte)0xCC;
	try
	    {
		os.write(outData, 0, outData.length);
		System.out.println("SessionData sent to server");
	    }
	catch(IOException e)
	    {
		e.printStackTrace();
	    }
    }
    private byte[] charToByte(char[] c)
    {
	byte[] ret = new byte[c.length];
	for(int i = 0; i < c.length; i++)
	    {
		ret[i] = (byte)c[i];
	    }
	return ret;
    }
    /**
     * This method sends TELNET specific commands to the telnet
     * host.  Primarily this would be used for protocol feature
     * negotiation.
     */ 
    private void doCommand(short tnCmd, short tnOption)
    {
	byte tmpBuffer[] = new byte[3];
	tmpBuffer[0] = (byte)IAC;
	tmpBuffer[1] = (byte)tnCmd;
	tmpBuffer[2] = (byte)tnOption;
	try
	    {
		os.write(tmpBuffer, 0, 3);
		//System.err.println("Sent: " + tmpBuffer[0] + " " + tmpBuffer[1] + " " + tmpBuffer[2]);
	    }
	catch(IOException e)
	    {
		//System.err.println("Problem sending...");
	    }
    }
    /**
     * This is a special instance of doCommand where the client
     * specifies that it is a 3270 client
     */
    private void doTerminalTypeCommand()
	throws IOException
    {
	byte tmpBuffer[] = { (byte)IAC, (byte)SB, (byte)TERMINAL_TYPE,
			     (byte)OPTION_IS, (byte)'I', (byte)'B', (byte)'M', (byte)'-', (byte)'3',
			     (byte)'2', (byte)'7', (byte)'8', (byte)'-', (byte)('0'+tn3270Model),
			     (byte)IAC, (byte)SE
	};
	os.write(tmpBuffer, 0, 16);
	os.flush();
	//System.err.println("Sent terminal type...");
    }
    /**
     * This method handles incoming TELNET-specific commands; specifically,
     * WILL, WONT, DO, DONT
     * @param tnCmd the incoming telnet command
     * @param tnOption the option for which the command is being sent
     */
    private void doTnCommand(short tnCmd, short tnOption)
	throws IOException
    {
	//System.err.print("doTnCommand: ");
	short cmd;
	switch(tnCmd)
	    {

	    case WILL:
	    case DO:
		switch(tnOption)
		    {
		    case BINARY:
		    case OPT_EOR:
		    case TERMINAL_TYPE:
			if(tnCmd == WILL)
			    cmd = DO;
			else
			    cmd = WILL;
			if(checkCmdHistory(cmd, tnOption))
			    {
				//System.err.println("History for " + tnOption + " true");
				return;
			    }
			else
			    //System.err.println("History for " + tnOption + " false");
			    doCommand(cmd, tnOption);
			return;
		    }
		if(tnCmd == WILL)
		    cmd = DONT;
		else
		    cmd = WONT;
		doCommand(cmd, tnOption);
		return;
	    case WONT:
	    case DONT:
		switch(tnOption)
		    {
		    case BINARY:
		    case OPT_EOR:
		    case TERMINAL_TYPE:
			throw new IOException();
		    }
		return;
	    }
    }
    /**
     * This method prevents endless request response loops as outlined
     * in RFC 854:  "If a party receives what appears to be a request to
     * to enter some mode it is already in, the request should not be 
     * acknowledged.  This non-response is essential to prevent endless
     * loops in the neotiation.
     * @param cmd the TELNET command - WILL WONT DO DONT
     * @param cmd the TELNET option - BINARY EOR TERMINAL_TYPE
     * @return false - if this command has not already been processed.
     *          true - if this command has already been processed.
     */
    private boolean checkCmdHistory(short cmd, short tnOption)
    {
	boolean history[];
	history = (cmd == WILL) ? willHistory : doHistory;
	int c;
	switch(tnOption)
	    {
	    case BINARY:
		c = 0;
		break;
	    case OPT_EOR:
		c = 1;
		break;
	    default:
		c = 2;
		break;
	    }
	if(history[c])
	    return(true);
	history[c] = true;
	return(false);
    }
    /**
     * The 'thread code' for this class.  Consumers need to invoke this
     * method to begin the communications process.  It will run indefinitely
     * until the socket read returns -1 (Host disconnected) or disconnect is
     * called.
     * Usage <code> Thread t = new Thread(<i>RWTelnet instance</i>);<BR>
     *              t.run;</CODE>
     * Any problems encountered (IOException, Host Disconnect) will be transmitted
     * back to the consumer via the RWTnAction interface.
     */
    public void run()
    {
	int n = 0;
	try
	    {
		for (;;)
		    {
			if ((inBufLen = readSocket()) == -1)
			    {
				//System.out.println("Returned -1");
				rw.client.status(RWTnAction.DISCONNECTED_BY_REMOTE_HOST);
				break;
			    }
			synchronized(this) 
			    {
				if(inBufLen < 20)
				    {
					//for(int i = 0; i < inBufLen; i++)
					//System.err.print(inBuf[i] + " ");
					//System.err.println("");
				    }
				//System.out.println("Parsing data...");
				parseData();
			    }
				//System.out.flush();
		    }
		//rw.connectionStatus(RWTnAction.DISCONNECTED_BY_REMOTE_HOST);
	    }
	catch (IOException e)
	    {
		//System.out.println("Error: " + e + "... disconnecting.");
		rw.client.status(RWTnAction.DISCONNECTED_BY_REMOTE_HOST);
		disconnect();
	    }
    }
    /**
     * Turns the encryption on and off
     * @param encryption True = on False = off
     */
    protected void setEncryption(boolean encryption)
    {
	this.encryption = encryption;
    }

    private Socket                tnSocket;
    private InputStream           is;
    private OutputStream          os;
    private short                 inBuf[];//put raw data from the inputstream here
    private short                 buffer3270[];//put the 3270 bytes in here
    private short                 subOptionBuffer[];
    private int                   tnState, inBufLen, buffer3270Len, subOptionBufferLen;
    private int                   tn3270Model;
    private RWTn3270StreamParser  rw;
    private short                 tnCommand;
    private boolean               willHistory[];
    private boolean               doHistory[];
    private byte                  key[];
    private int                   keyCounter;
    private boolean               encryption;
    private Thread                sessionThread;

   
    /********************************************/
    /*          TELNET protocol constants       */
    /*          found on page 14 of RFC 845     */
    /********************************************/
    final static short SE            =        240;//End of subnegotiation parameters
    final static short NOP           =        241;//No Operation
    final static short SB            =        250;//Begin subnegotiation
    final static short WILL          =        251;//Will perform an indicated option
    final static short WONT          =        252;//Won't perform an indicated option
    final static short DO            =        253;//Please perform an indicated option
    final static short DONT          =        254;//Please don't perform an indicated option
    final static short IAC           =        255;//The following bytes are a telnet command
    final static short EOR           =        239;//End of record
    /*             TELNET OPTIONS                */
    final static short BINARY        =        0;//Use 8-bit data path
    final static short TERMINAL_TYPE =        24;//Tn3270
    final static short OPT_EOR       =        25;//End of Record
    final static short OPTION_IS     =        0;//option is
    final static short SEND_OPTION   =        1;//send option
    /*             TELNET STATES (internal)      */
    final static short TN_DEFAULT    =        0;//default incoming data
    final static short TN_IAC        =        1;//next is command
    final static short TN_CMD        =        2;//incoming command
    final static short TN_SUB_CMD    =        3;//incoming sub-command
    /*							CUSTOM CODES									*/
    final static short BROADCAST 		= 			 245;// custom byte for flagging the next bytes as broadcast message

}
