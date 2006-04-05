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
package net.sf.freehost3270.client;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class is the mack-daddy of the 3270 Engine, it takes
 * the data from the RWTelnet class (through the incomingData method
 * of the RWTnAction interface.) and makes sense of it.
 *
 * <p> It also implements all of the commands and orders outlined in
 * the "3270 Data Stream Programmer's Reference" (GA23-0059-07) in
 * chapters 3 & 4.
 *
 * @since 0.1
 */
public class RWTn3270StreamParser {
    private final static Logger log = Logger.getLogger(RWTn3270StreamParser.class.getName());
  
  // this logger is used to log low level parsing operations
  private final static Logger logP = Logger.getLogger("net.sf.freehost3270.Parsing");
  
    static final short[] addrTable = {
            0x40, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0x4A,
            0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5,
            0xD6, 0xD7, 0xD8, 0xD9, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F, 0x60,
            0x61, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9, 0x6A, 0x6B,
            0x6C, 0x6D, 0x6E, 0x6F, 0xF0, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6,
            0xF7, 0xF8, 0xF9, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F
        };
    private static final int[] ebc2asc = {
            0, 1, 2, 3, 156, 9, 134, 127, 151, 141, 142, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 157, 133, 8, 135, 24, 25, 146, 143, 28, 29, 30, 31, 128,
            129, 130, 131, 132, 10, 23, 27, 136, 137, 138, 139, 140, 5, 6, 7,
            144, 145, 22, 147, 148, 149, 150, 4, 152, 153, 154, 155, 20, 21, 158,
            26, 32, 160, 161, 162, 163, 164, 165, 166, 167, 168, 213, 46, 60, 40,
            43, 124, 38, 169, 170, 171, 172, 173, 174, 175, 176, 177, 33, 36, 42,
            41, 59, 126, 45, 47, 178, 179, 180, 181, 182, 183, 184, 185, 203, 44,
            37, 95, 62, 63, 186, 187, 188, 189, 190, 191, 192, 193, 194, 96, 58,
            35, 64, 39, 61, 34, 195, 97, 98, 99, 100, 101, 102, 103, 104, 105,
            196, 197, 198, 199, 200, 201, 202, 106, 107, 108, 109, 110, 111, 112,
            113, 114, 94, 204, 205, 206, 207, 208, 209, 229, 115, 116, 117, 118,
            119, 120, 121, 122, 210, 211, 212, 91, 214, 215, 216, 217, 218, 219,
            220, 221, 222, 223, 224, 225, 226, 227, 228, 93, 230, 231, 123, 65,
            66, 67, 68, 69, 70, 71, 72, 73, 232, 233, 234, 235, 236, 237, 125,
            74, 75, 76, 77, 78, 79, 80, 81, 82, 238, 239, 240, 241, 242, 243, 92,
            159, 83, 84, 85, 86, 87, 88, 89, 90, 244, 245, 246, 247, 248, 249,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 250, 251, 252, 253, 254, 255
        };
    private static final int[] asc2ebc = {
            0, 1, 2, 3, 55, 45, 46, 47, 22, 5, 37, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 60, 61, 50, 38, 24, 25, 63, 39, 28, 29, 30, 31, 64, 90, 127,
            123, 91, 108, 80, 125, 77, 93, 92, 78, 107, 96, 75, 97, 240, 241,
            242, 243, 244, 245, 246, 247, 248, 249, 122, 94, 76, 126, 110, 111,
            124, 193, 194, 195, 196, 197, 198, 199, 200, 201, 209, 210, 211, 212,
            213, 214, 215, 216, 217, 226, 227, 228, 229, 230, 231, 232, 233, 173,
            224, 189, 154, 109, 121, 129, 130, 131, 132, 133, 134, 135, 136, 137,
            145, 146, 147, 148, 149, 150, 151, 152, 153, 162, 163, 164, 165, 166,
            167, 168, 169, 192, 79, 208, 95, 7, 32, 33, 34, 35, 36, 21, 6, 23,
            40, 41, 42, 43, 44, 9, 10, 27, 48, 49, 26, 51, 52, 53, 54, 8, 56, 57,
            58, 59, 4, 20, 62, 225, 65, 66, 67, 68, 69, 70, 71, 72, 73, 81, 82,
            83, 84, 85, 86, 87, 88, 89, 98, 99, 100, 101, 102, 103, 104, 105,
            112, 113, 114, 115, 116, 117, 118, 119, 120, 128, 138, 139, 140, 141,
            142, 143, 144, 106, 155, 156, 157, 158, 159, 160, 170, 171, 172, 74,
            174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187,
            188, 161, 190, 191, 202, 203, 204, 205, 206, 207, 218, 219, 220, 221,
            222, 223, 234, 235, 236, 237, 238, 239, 250, 251, 252, 253, 254, 255
        };

    /****************************************************************/
    /*                   TN3270 Commands                            */
    /* See "Data Stream Programmer's reference p. 3.3               */
    /****************************************************************/
    /**
     * Write command p. 3.5.1
     */
    final static short CMD_W = 0x01;
    final static short CMD_W_EBCDIC = 0xF1;

    /**
     * Erase/Write command p. 3.5.2
     */
    final static short CMD_EW = 0x05;
    final static short CMD_EW_EBCDIC = 0xF5;

    /**
     * Erase/Write Alternate command p 3.5.3
     */
    final static short CMD_EWA = 0x0D;
    final static short CMD_EWA_EBCDIC = 0x7E;

    /**
     * Read Buffer Command p 3.6.1
     */
    final static short CMD_RB = 0x02;
    final static short CMD_RB_EBCDIC = 0xF2;

    /**
     * Read Modified Command p 3.6.2
     */
    final static short CMD_RM = 0x06;
    final static short CMD_RM_EBCDIC = 0xF6;

    /**
     * Read Modified All Command p 3.6.2.5
     */
    final static short CMD_RMA = 0x0E;
    final static short CMD_RMA_EBCDIC = 0x6E;

    /**
     * Erase all unprotected command p. 3.5.5
     */
    final static short CMD_EAU = 0x0F;
    final static short CMD_EAU_EBCDIC = 0x6F;

    /**
     * Write Structured Field command p. 3.5.4 (Not supported in ASCII)
     */
    final static short CMD_WSF = 0x11;
    final static short CMD_WSF_EBCDIC = 0xF3;

    /**
     * No-op
     */
    final static short CMD_NOOP = 0x03;

    /*************************************************************/
    /*                    TN3270 ORDERS                          */
    /*                p 4.3 table 4-1                            */
    /*************************************************************/
    /**
     * Start Field Order p 4.3.1
     */
    final static short ORDER_SF = 0x1D;

    /**
     * Start Field Extended p 4.3.2
     */
    final static short ORDER_SFE = 0x29;

    /**
     * Set Buffer Address p 4.3.3
     */
    final static short ORDER_SBA = 0x11;

    /**
     * Set Attribute p 4.3.4
     */
    final static short ORDER_SA = 0x28;

    /**
     * Modify Field p 4.3.5
     */
    final static short ORDER_MF = 0x2C;

    /**
     * Insert Cursor p 4.3.6
     */
    final static short ORDER_IC = 0x13;

    /**
     * Program Tab p 4.3.7
     */
    final static short ORDER_PT = 0x05;

    /**
     * Repeat to Address p 4.3.8
     */
    final static short ORDER_RA = 0x3C;

    /**
     * Erase Unprotected to Address p 4.3.9
     */
    final static short ORDER_EUA = 0x12;

    /**
     * Graphic Escape p 4.3.10
     */
    final static short ORDER_GE = 0x08;

    /********************************************************************/
    /*             Extended Attributes (see table 4-6 p 4.4.5)          */
    /********************************************************************/
    /**
     * 3270 Field Attributes p 4.4.6.2
     */
    final static short XA_SF = 0xC0;

    /**
     * Field Validation p 4.4.6.3
     */
    final static short XA_VALIDATION = 0xC1;

    /**
     * Field Outlining  p 4.4.6.6
     */
    final static short XA_OUTLINING = 0xC2;

    /**
     * Extended Highlighting p 4.4.6.3
     */
    final static short XA_HIGHLIGHTING = 0x41;

    /**
     * Foreground Color p 4.4.6.4
     */
    final static short XA_FGCOLOR = 0x42;

    /**
     * Character Set 4.4.6.5
     */
    final static short XA_CHARSET = 0x43;

    /**
     * Background Color p 4.4.6.4
     */
    final static short XA_BGCOLOR = 0x45;

    /**
     * Transparency p 4.4.6.7
     */
    final static short XA_TRANSPARENCY = 0x46;
    
    final static short SF_RPQ_LIST  = 0x00;
    final static short SF_READ_PART = 0x01;
    final static short SF_RP_QUERY  = 0x02;
    final static short SF_RP_QLIST  = 0x03;
    final static short SF_RPQ_EQUIV = 0x40;
    final static short SF_RPQ_ALL   = 0x80;
    
    private RW3270 rw;
    private RW3270Char[] chars;
    private char[] display;
    private RWTelnet tn;
    protected RWTnAction client;
    private Vector fields;
    private int counter;
    private short[] dataIn;
    private int dataInLen;
    private boolean lastWasCommand;
    private int bufferAddr;
    private short foreground;
    private short background;
    private short highlight;

    public RWTn3270StreamParser(RW3270 rw, RWTnAction client) {
        this.client = client;
        this.rw = rw;
        display = rw.getDisplay();
    }

    /**
     * This method takes an input buffer and executes the appropriate
     * tn3270 commands and orders.
     */
    protected synchronized void parse(short[] inBuf, int inBufLen)
        throws IOException {
        if (log.isLoggable(Level.FINEST)) {
            StringBuffer inBufStr = new StringBuffer("parsing buffer: ");

            for (int i = 0; i < inBufLen; i++) {
	      // prepending the hex digit with 0x prefix for
	      // convenience of putting dumped data into tests
                inBufStr.append("0x").append(Integer.toHexString(inBuf[i]))
		  .append(", ");
            }

            log.finest(inBufStr.toString());
        } else if (log.isLoggable(Level.FINE)) {
            log.fine("parsing buffer");
        }

        bufferAddr = rw.getCursorPosition();

        fields = rw.getFields();
        chars = rw.getDataBuffer();
        dataIn = inBuf;
        dataInLen = inBufLen;

        //is the first byte an EBCDIC cmd, if so convert it
        switch (dataIn[0]) {
        case CMD_W_EBCDIC:
            dataIn[0] = CMD_W;

            break;

        case CMD_EW_EBCDIC:
            dataIn[0] = CMD_EW;

            break;

        case CMD_EWA_EBCDIC:
            dataIn[0] = CMD_EWA;

            break;

        case CMD_EAU_EBCDIC:
            dataIn[0] = CMD_EAU;

            break;

        case CMD_WSF_EBCDIC:
            dataIn[0] = CMD_WSF;

            break;

        case CMD_RB_EBCDIC:
            dataIn[0] = CMD_RB;

            break;

        case CMD_RM_EBCDIC:
            dataIn[0] = CMD_RM;

            break;

        case CMD_RMA_EBCDIC:
            dataIn[0] = CMD_RMA;

            break;
        }

        //now let's send the commands off to their appropriate methods:
        switch (dataIn[0]) {
        case CMD_W:
        case CMD_EW:
        case CMD_EWA:
        case CMD_EAU:
            log.finest("write operation");
            lastWasCommand = true;
            writeOperation();
            buildFields();
            client.incomingData();

            break;

        case CMD_WSF:
            lastWasCommand = true;
            log.finest("WSF");
            writeStructuredField(dataIn);

            break;

        case CMD_RB:
            lastWasCommand = true;

            //System.out.println("Read Buffer...");
            readBuffer();

            break;

        case CMD_RM:
            lastWasCommand = true;

            //System.out.println("Read Modified...");
            readModified();

            break;

        case CMD_RMA:
            lastWasCommand = true;

            //System.out.println("Read Modified All...");
            readModifiedAll();

            break;

        default:
            throw new IOException("Invalid 3270 Command");
        }

        rw.resumeParentThread();
    }

    /**
     * From <i>3270 Data Stream Programmer's Reference</i>:
     * <h3>3.5 Write Operation</h3>
     * The process of sending a write type command and performing that command
     * is called a <i>write</i> operation.  Five write commands are initiated by
     * the application program and performed by the display:
     * <UL>
     * <LI>Write(W)</LI>
     * <LI>Erase/Write(EW)</LI>
     * <LI>Erase/Write Alternate(EWA)</LI>
     * <LI>Erase All Unprotected(EAU)</LI>
     * <LI>Write Structured Field(WSF)</LI>
     * </UL>
     *
     * <i>From 3.1</i>
     * The format of a write command is as follows:
     * <table border="1">
     * <tr><th>Byte 1</th><th>Byte 2</th><th>Byte 3 ... <i>n</i></th></tr>
     * <tr><td align="center">Write Command</td>
     *     <td align="center">WCC (Write Control Character)</td>
     *     <td align="center">Orders and Data</td>
     * </tr>
     * </table>
     *
     * <h3>3.4 Write Control Character (WCC) Byte</h3>
     * The following table explains the interpretation of the WCC byte
     * <table border="1">
     * <TR><TH colspan = 2>Table 3-2. Write Control Character (WCC) Bit Definitions for Displays</TH></TR>
     * <TR><TH>Bit</TH><TH>Explanation</TH></TR>
     * <TR><TD>0</TD><TD>N/A</TD></TR>
     * <TR><TD>1</TD><TD>WCC reset bit.  When set to 1, it resets partition
     * characteristics to their system-defined defaults.  When set to 0, the
     * current characteristics remain unchanged (no reset operations are performed).</TD></TR>
     * <TR><TD>2, 3 & 4</TD><TD>Printer Operations N/A</TD></TR>
     * <TR><TD>5</TD><TD>Sound alarm bit.  When set to 1, it sounds the audible alarm
     * at the end of the operation if that device has an audible alarm.</TD></TR>
     * <TR><TD>6</TD><TD>Keyboard Restore Bit. When set to 1, this bit unlocks the keyboard.
     * it also resets the AID byte.</TD></TR>
     * <TR><TD>7</TD>
     * <TD>Bit 7 resets MDT bits in the field attributes.  When set to 1, all
     * MDT bits in the device's existing character buffer are reset before any
     * data is written or orders are performed.</TD></TR></TABLE>
     *
     */
    private synchronized void writeOperation() {
        if (dataIn[0] == CMD_EAU) {
	  logP.fine("erase all unprotected");

            eraseAllUnprotected();

            return;
        }

        //now let's check the WCC for bit 0
        if ((dataIn[1] & 0x01) != 0) {
            //Bit 7 is set to 1, reset all modified bits
	  logP.fine("reset MDT");

            resetMDT();
            lastWasCommand = true;
        }

        switch (dataIn[0]) {
        case CMD_EW:
        case CMD_EWA:

            //System.err.println("Erase Write...");
            lastWasCommand = true;
            eraseWrite();

            break;

        case CMD_W:

            //System.err.println("Write...");
            lastWasCommand = true;
            write();

            break;
        }

        //check the post-operation functions in the WCC
        if ((dataIn[1] & 0x04) != 0) {
            //Bit 5 is set to 1
            beep();
        }

        if ((dataIn[1] & 0x02) != 0) {
            //Bit 2 is set to 1
            rw.unlockKeyboard();
            client.status(RWTnAction.READY);
        }
    }

    /**
     * <h3>3.5.1 Write Command</h3>
     * The Write command writes data into specified locations of the character
     * buffer of partition 0 without erasing or modifying data in the other
     * locations.  Data is stored in sucessive buffer locations until an order
     * is encountered in the data stream that alters the buffer address, or until
     * all the data has been stored.  During the write operation, the buffer
     * address is advanced one location as each character is stored.
     */
    private synchronized void write() {
        lastWasCommand = true;

        //System.out.println(dataInLen);
        for (counter = 2; counter < dataInLen; counter++) {
            switch (dataIn[counter]) {
            case ORDER_SF:

                //System.err.println("SF: " + bufferAddr + " ");
                startField();
                lastWasCommand = true;

                break;

            case ORDER_SFE:

                //System.err.println("SFE " + bufferAddr + " ");
                startFieldExtended();
                lastWasCommand = true;

                break;

            case ORDER_SBA:

                //System.err.println("SBA " + bufferAddr + " ");
                bufferAddr = setBufferAddress();

                //System.err.println("to: " + bufferAddr);
                lastWasCommand = true;

                break;

            case ORDER_SA:

                //System.err.println("SA " + bufferAddr + " ");
                setAttribute();
                lastWasCommand = true;

                break;

            case ORDER_MF:

                //System.err.println("MF " + bufferAddr + " ");
                modifyField();
                lastWasCommand = true;

                break;

            case ORDER_IC:

                //System.err.println("IC " + bufferAddr + " ");
                insertCursor();
                lastWasCommand = true;

                break;

            case ORDER_PT:

                //System.err.println("PT " + lastWasCommand + " " + bufferAddr);
                programTab();

                //System.out.println(" " + bufferAddr);
                break;

            case ORDER_RA:

                //System.err.print("RA " + bufferAddr + " ");
                repeatToAddress();
                lastWasCommand = true;

                break;

            case ORDER_EUA:

                //System.err.print("EUA " + bufferAddr + " ");
                eraseUnprotectedToAddress();
                lastWasCommand = true;

                break;

            case ORDER_GE:

                //System.err.print("GE " + " ");
                graphicEscape();
                lastWasCommand = true;

                break;

            default:
                display[bufferAddr] = (dataIn[counter] == 0x00) ? ' '
                                                                : (char) ebc2asc[dataIn[counter]];

                RW3270Char currChar = (RW3270Char) chars[bufferAddr++];
                currChar.clear();
                currChar.setChar((char) ebc2asc[dataIn[counter]]);

                //System.out.print(currChar.getChar());
                currChar.setForeground(foreground);
                currChar.setBackground(background);
                currChar.setHighlighting(highlight);
                lastWasCommand = false;

                if (bufferAddr == chars.length) {
                    bufferAddr = 0;
                }
            }
        }
    }

    /**
     * <h3>3.5.2 Erase/Write Command</h3>
     * The EW command does the following:
     * <UL>
     * <LI>Sets the implicit partition size to the default size, if in implicit
     * state</LI>
     * <LI>Resets a Program Check Indication, if one exists.</LI>
     * <LI>Erases the character buffer by writing null characters into all buffer
     *    locations.</LI>
     * <LI>Sets all the associated character attributes and extended field attributes
     *    to their default value(X'00).</LI>
     * <LI>Erases all field validation attributes.</LI>
     * <LI>Sets the current cursor position to 0.  If directed to a partition,
     *    autoscroll is performed, if necessary, to position the window at offset
     *    (0, 0).</LI>
     * <LI>If bit 1 of the WCC is set to 1, EW does the following:  </LI>
     *     <UL>
     *        <LI>Resets the inbound reply mode to Field.</LI>
     *        <LI>Resets to implicit partition state, if currently in explicit
     *             partitioned state.  It destroys all partitions, creates implicit
     *             partition 0 with default screen size, and sets inboud PID to 0 and
     *             INOP to Read Modified.</LI>
     *    </UL>
     * <LI>Provides an acknoledgment of any outstanding read or enter if the
     *     keyboard restore bit in the WCC is set to 1.</LI>
     * <LI>Performs a write operation</LI>
     * </UL>
     */
    private synchronized void eraseWrite() {
        //set all buffer positions to 'null'
        for (int i = 0; i < chars.length; i++) {
            ((RW3270Char) chars[i]).clear();
            display[i] = ' ';
        }

        rw.setCursorPosition((short) 0);
        bufferAddr = 0;
        write();
    }

    /**
     * <h3>3.5.5 Erase All Unprotected Command</h3>
     * EAU does the following:
     * <UL>
     * <LI>Clears all the unprotected character locations of the partition
     *     to nulls and sets any character attributes affected to their default
     *     values.</LI>
     * <LI>Resets to 0 the MDT bit in the field attribute for each unprotected field</LI>
     * <LI>Unlocks the keyboard</LI>
     * <LI>Resets the AID</LI>
     * <LI>Repositions the cursor to the first character location, after
     *     the field attribute, in the first unprotected field of the
     *     partition's character buffer.</LI>
     * </UL>
     */
    private synchronized void eraseAllUnprotected() {
        for (int i = 0; i < chars.length; i++) {
            RW3270Char c = (RW3270Char) chars[i];
            RW3270Field f = c.getField();

            if ((f != null) && !f.isProtected()) {
                display[i] = ' ';
                c.clear();

                try {
                    f.setModified(false);
                } catch (Exception e) {
                    log.warning(e.getMessage());
                }
            } else if (f == null) {
                //not in a field -- unprotected by default
                c.clear();
            }

            //unlock the keyboard
            rw.unlockKeyboard();

            //move the cursor to the first unprotected field
            rw.setCursorPosition((short) 0);
            rw.setCursorPosition(rw.getNextUnprotectedField(
                    rw.getCursorPosition()));

            //TO-DO reset the AID
        }
    }

    /**
     * <h3>3.5.4 Write Structured Field</h3>
     * WSF is used to send structured fields from the spplication program
     * to the display.  On the application-to-display flow [outbound], structured
     * fields can be sent only with the WSF command.
     *<P>
     * The format of a WSF data stream is as follows:
     * <TABLE border = 1>
     * <TR><TD>WSF Command</TD><TD>Structured Field</TD><TD>Structured Field...</TD></TR></TABLE>
     *</P>
     *  In our case, we're really only concerned with responding to
     *  queries, so we can inform the host of our capabilities on demand.
     *  Query replies are covered in agonizing detail in chapter 6 of the
     * <i>3270 Data Stream Programmer's Reference</i>.  Suffice it to say
     *  That we're telling the host:
     *  <LI>How big our screen is</LI>
     *  <LI>How many colors we support</LI>
     *  <LI>Do we handle outlining</LI>
     */
    private synchronized void writeStructuredField(short[] buf) {
        log.finest("Write Structured Field...");

        int cmnd;
        int length;
        int offset;
        int nleft;
        int pid;
        int sfid;
        int type;
        int buflen;
        int i;
        int n;
        buflen = buf.length;
        offset = 1;
        nleft = buflen - 1;

        while (nleft > 0) {
            if (nleft < 3) {
                return;

                //WSF too small
            }

            length = (buf[offset] << 8) + buf[offset + 1];
            sfid = buf[offset + 2];

            switch (sfid) {
            case SF_READ_PART:

                /*
                 * Read Partion - p. 5-47
                 */
                if (length < 5) {
                    return;

                    //WSF-RP too small
                }

                pid = buf[offset + 3];
                type = buf[offset + 4];
                /* Check to see if it is a Query 0x02 */
                switch( type ) {
                case SF_RP_QUERY:
                	if( pid != 0xFF ) 
                		return;        	
                	try {
                		short[] queryReply = buildQueryReply( 2, true );
                		rw.getTelnet().sendData(queryReply, queryReply.length);
                	} catch (IOException e) {
                		log.severe(e.getMessage());
                	}
                	
                	break;
                case SF_RP_QLIST:
                	if( pid != 0xFF ) 
                		return;
                	switch( buf[offset + 5] ) {
                	case SF_RPQ_LIST:
                		System.out.println( "List" );
                		return;
                	case SF_RPQ_EQUIV:
                		System.out.println( "Equivalent+List" );
                		return;
                	case SF_RPQ_ALL:
                		System.out.println( "All" );
                		try {
                    		short[] queryReply = buildQueryReply( 2, true );
                    		rw.getTelnet().sendData(queryReply, queryReply.length);
                    	} catch (IOException e) {
                    		log.severe(e.getMessage());
                    	}
                		break;
                	}
                	break;
                default:
                		return;
                }

                

                break;

            case SF_RPQ_EQUIV:

                /*
                 * Outbound 3270DS - p. 5-41
                 */
                if (length < 5) {
                    return;

                    //WSF-OBDS too small
                }

                pid = buf[offset + 3];
                cmnd = buf[offset + 4];

                if (pid != 0x00) {
                    return;

                    //WSF-OBDS invalid PID
                }

                switch (cmnd) {
                case CMD_W_EBCDIC:
                case CMD_EW_EBCDIC:
                case CMD_EWA_EBCDIC:
                case CMD_EAU_EBCDIC:
                    n = length - 4;
                    dataIn = new short[n];

                    for (i = 0; i < n; ++i) {
                        dataIn[i] = buf[i + 4];
                    }

                    writeOperation();

                    break;

                default:
                    return;

                    //WSF-OBDS unsupported
                }

                break;

            default:
                return;

                //unsupported WFS ID
            }

            offset += length;
            nleft -= length;
        }
    }

    /** <h2>3.6 Read Operations</h2>
     * The process of sending data inbound is called a <i>read operation</i>.
     * A read operation can be initiated by the following:
     * <UL>
     * <LI>The host application sending an explicit read command</LI>
     * <LI>The host application program sending a Read Partition structured
     * field specifying Read Buffer, Read Modified, or Read Modified All.</LI>
     * <LI>An operator action, for example, pressing the Enter key.</LI>
     * </UL>
     * A read operation sends an inbound data stream (from the terminal to the
     * application program) with an AID byte as the first byte of the inbound
     * data stream.  The inbound data stream usually consists of an AID followed
     * by the cursor address (2 bytes).  These 3 bytes of the read data stream
     * the AID, and cursor address are known as the <i>read heading</i>. The inbound
     * data stream format is as follows:
     * <P>
     * <TABLE border = 1>
     *    <TR><TH>Byte 1</TH><TH>Byte 2</TH><TH>Byte 3</TH><TH>Byte 4</TH></TR>
     *    <TR><TD>AID</TD><TD colspan = 2>Cursor Address (2 bytes)</TD><TD>Data</TD></TR>
     * </TABLE>
     * </P>
     * <h2>3.6.1 Read Commands</h2>
     * Three read commands can be sent by the application program:  Read Buffer
     * Read Modified, and Read Modified All.
     * </h3>3.6.1.1 Read Buffer Command</h3>
     * Operation of the Read Buffer command causes all data in the addressed
     * display buffer, from the buffer location at which reading starts through
     * the last buffer location, to be transmitted to the host.  For displays
     * the transfer of data begins from buffer address 0.
     */
    private synchronized void readBuffer() {
        int byteCount = 0;
        short[] dataOut = new short[((chars.length) * 2) + 40];

        //get the current AID
        dataOut[byteCount++] = rw.getAID();

        //convert the current cursor position to 14-bit addressing
        dataOut[byteCount++] = addrTable[(rw.getCursorPosition() >> 6) & 0x3F];
        dataOut[byteCount++] = addrTable[rw.getCursorPosition() & 0x3F];

        //iterate through the screen buffer, if a position
        //contains an FA send it instead of the character.
        for (int i = 0; i < (chars.length); i++) {
            RW3270Char currChar = (RW3270Char) chars[i];

            if (currChar.isStartField()) {
                dataOut[byteCount++] = ORDER_SF;
                dataOut[byteCount++] = currChar.getFieldAttribute();
            } else {
                dataOut[byteCount++] = (short) asc2ebc[currChar.getChar()];
            }
        }

        try {
            rw.getTelnet().sendData(dataOut, byteCount);
        } catch (IOException e) {
        }
    }

    /**
     * <h3>3.6.2.1 Read Modified Operation</h3>
     * During a read modified operation, if an AID other than selector pen
     * attention, cursor select key, PAkey, or Clear key is generated, all
     * fields that have been modified by keyboard, selector pen, or magnetic
     * reader activity are transferred to the application program.  A major
     * feature of the read modified operation is null suppression.  Only
     * non-null character data and corresponding character attribute data (in
     * Character mode)are transmitted.  All null character data and all extended
     * attributes for null character data are suppressed.
     * <BR><BR>
     * If a space or null selector pen AID is generated, fields are not
     * transferred to main storage during the read modified operation. Instead,
     * when a set MDTbit is found (indicating selector pen and/or keyboard
     * activity), only the read heading, the SBA order code, and the attribute
     * address +1 are transferred.
     * <BR><BR>
     * If the buffer is unformatted (contains no fields), the read data stream
     * consists of the 3-byte read heading followed by all alphanumeric data in
     * the buffer (nulls are suppressed), even when part or all of the data has
     * not been modified.  Since an unformatted buffer contains no attribute bytes
     * no SBA codes with associated addresses or address characters included in the
     * data stream, and the modification of data cannot be determined.
     * Data transfer starts at address 0 and continues to the end of the buffer.
     * At the end of the operation, the buffer address is set to 0.
     */
    protected synchronized void readModified() {
        client.status(RWTnAction.X_WAIT);
        rw.lockKeyboard();

        int byteCount = 0;
        short[] dataOut = new short[(chars.length * 2) + 40];
        dataOut[byteCount++] = rw.getAID();

        switch (rw.getAID()) {
        case RW3270.PA1:
        case RW3270.PA2:
        case RW3270.PA3:
        case RW3270.CLEAR:

            try {
                rw.getTelnet().sendData(dataOut, byteCount);
            } catch (Exception e) {
            }

            return;
        }

        //cursor position
        dataOut[byteCount++] = addrTable[(rw.getCursorPosition() >> 6) & 0x3F];
        dataOut[byteCount++] = addrTable[rw.getCursorPosition() & 0x3F];

        //are there any fields? (formatted/unformatted)
        if (fields.size() == 0) {
            for (int i = 0; i < chars.length; i++) {
                RW3270Char currChar = (RW3270Char) chars[i];

                if (currChar.getChar() != ' ') {
                    dataOut[byteCount++] = (short) asc2ebc[currChar.getChar()];
                }
            }

            try {
                rw.getTelnet().sendData(dataOut, byteCount);
            } catch (IOException e) {
            }

            bufferAddr = 0;

            return;
        }

        //get an enumeration of the current fields
        Enumeration e = fields.elements();

        //iterate through the fields, checking for modification
        while (e.hasMoreElements()) {
            RW3270Field f = (RW3270Field) e.nextElement();

            if (f.isModified()) {
                //field has been modified... get characters stored in the
                //field.
                RW3270Char[] fieldChars = f.getChars();

                //send an SBA on the beginning of this field + 1
                //(ignore the field attribute)
                dataOut[byteCount++] = ORDER_SBA;
                dataOut[byteCount++] = addrTable[((f.getBegin() + 1) >> 6) &
                    0x3F];
                dataOut[byteCount++] = addrTable[(f.getBegin() + 1) & 0x3F];

                //put the characters in the output buffer
                for (int i = 1; i < fieldChars.length; i++) {
                    if (fieldChars[i].getChar() != 0) //null suppression
                     {
                        dataOut[byteCount++] = (short) asc2ebc[fieldChars[i].getChar()];

                        //System.out.print("Hey..." + fieldChars.length + fieldChars[i].getChar());
                    }
                }
            }
        }

        try {
            //System.out.println("Sending data...");
            rw.getTelnet().sendData(dataOut, byteCount);
        } catch (IOException ioe) {
            log.warning("exception in readModified: " + ioe.getMessage());
        }
    }

    private synchronized void readModifiedAll() {
        readModified();
    }

    private synchronized void resetMDT() {
        Enumeration e = fields.elements();

        while (e.hasMoreElements()) {
            try {
                ((RW3270Field) e.nextElement()).setModified(false);
            } catch (IsProtectedException ipe) {
                //the field is protected, how can it be modified? Move on.
                log.finest("the field is protected. pass it");
            }
        }
    }

    private void beep() {
        //TODO: Add beep code here... use a callback interface to the consumer.
        //System.out.println("Beep..");
        log.fine("beep");
    }

    /**
     * <h3>4.3.1 Start Field(SF)</h3>
     * The SF order indicates the start of a field.
     * <h3>Table 4-4 - Bit Definitions for 3270 Field Attributes</h3>
     * <TABLE border = 1>
     * <TR><TH>Bit</TH><TH>Description</th></tr>
     * <TR><TD>0, 1</td><td>N/A</td></tr>
     * <TR><TD>2</TD><TD>0 - Field is Unprotected<BR>
     *                   1 - Field is Protected</TD></TR>
     * <TR><TD>3</TD><TD>0 - Alphanumeric<BR>
     *                   1 - Numeric</TD></TR>
     * <TR><TD>4, 5</TD><TD>00 - Display/not selector pen detectable<BR>
     *                      01 - Display/selector pen detectable<BR>
     *                      10 - Intensified display/selector pen detectable(BOLD)<BR>
     *                      11 - Nondisplay, nondetectable (PASSWORDS, etc.)</TD></TR>
     * <TR><TD>6</TD><TD>Reserved. Must Always be 0</TD></TR>
     * <TR><TD>7</TD><TD>MDT identifies modified fields during Read Modified Command
     *                   operations.<BR>
     *                   0 - Field has not been modified.<BR>
     *                   1 - Field has been modified by the operator.<BR></TD></TR>
     *
     */
    private synchronized void startField() {
        ((RW3270Char) chars[bufferAddr]).clear();

        //increment the buffer address,
        //and clear the existing character
        ((RW3270Char) chars[bufferAddr]).setStartField();
        ((RW3270Char) chars[bufferAddr]).setFieldAttribute(dataIn[++counter]);
        display[bufferAddr] = ' ';

        if (++bufferAddr == chars.length) {
            bufferAddr = 0;
        }
    }

    /**
     * <h3>4.3.2 Start Field Extended (SFE)</h3>
     * The SFE order is also used to indicate the start of a field.  However,
     * the SFE control sequence contains information on the field's properties that
     * are described in the extended field attribute.  The SFE order has the
     * following format:
     * <TABLE border=1>
     * <TR><TD>0x29</TD><TD>Number of Attribute Type-Value pairs</TD><TD>Attribute Type</TD><TD>Attribute Value</TD></TR>
     * </TABLE>
     */
    private synchronized void startFieldExtended() {
        counter++;
        ((RW3270Char) chars[bufferAddr]).clear();
        display[bufferAddr] = ' ';

        int pairs = dataIn[counter]; //get the number of attribute type pairs

        if (!((RW3270Char) chars[bufferAddr]).isStartField()) {
            // Hard-learned lesson:  if no StartField is specified,
            // but extended attributes have been defined, you must
            // define a default start field.
            ((RW3270Char) chars[bufferAddr]).clear();
            ((RW3270Char) chars[bufferAddr]).setStartField();
            ((RW3270Char) chars[bufferAddr]).setFieldAttribute((short) 0x00);
        }

        for (int i = 0; i < pairs; i++) {
            //System.out.println("SFE: " + Integer.toHexString(dataIn[++counter]));
            switch (dataIn[++counter]) {
            // get the next value from dataIn
            // which will tell us what kind of attribute
            // it is
            case XA_SF: // same as SF command above
                ((RW3270Char) chars[bufferAddr]).setStartField();
                ((RW3270Char) chars[bufferAddr]).setFieldAttribute(dataIn[++counter]);

                break;

            case XA_VALIDATION:
                ((RW3270Char) chars[bufferAddr]).setValidation(dataIn[++counter]);

                break;

            case XA_OUTLINING:
                ((RW3270Char) chars[bufferAddr]).setOutlining(dataIn[++counter]);

                break;

            case XA_HIGHLIGHTING:
                ((RW3270Char) chars[bufferAddr]).setHighlighting(dataIn[++counter]);

                break;

            case XA_FGCOLOR:
                ((RW3270Char) chars[bufferAddr]).setForeground(dataIn[++counter]);

                break;

            case XA_CHARSET:

                //not supported - nightmare
                counter++;

                break;

            case XA_BGCOLOR:
                ((RW3270Char) chars[bufferAddr]).setBackground(dataIn[++counter]);

                break;

            case XA_TRANSPARENCY:

                //not supported - What does it do?
                counter++;

                break;
            }
        }

        bufferAddr++;
    }

    /**
     * <h3>4.3.3 Set Buffer Address</h3>
     * The Set Buffer Address function converts a two-byte segment into
     * an integer corresponding to the buffer address.  If the first 2 bits
     * of the first byte are 00, it signals that a 14-bit binary address follos
     * (the remaining 6 bits of byte 1 and 8 bits of byte 2).  This is easily
     * arrived at by shifting the first byte 8 positions left and adding the second
     * byte.  If the first two bits of the first byte contain any other bit
     * pattern (01, 10, 11), the two bytes comprise a 12-bit coded address which
     * can be arrived at by <code>((counter1 & 0x3F) << 6) + (counter2 & 0x3F)</code>
     */
    private synchronized int setBufferAddress() {
        int counter1 = dataIn[++counter];
        int counter2 = dataIn[++counter];

        if ((counter1 & 0xC0) == 0x00) {
            return ((counter1 & 0x3F) << 8) + counter2;
        } else {
            return ((counter1 & 0x3F) << 6) + (counter2 & 0x3F);
        }
    }

    /**
     * <h3>4.3.4 Set Attribute(SA)</h3>
     * The SA order is used to specify a character's attribute type and its value
     * so that subsequently interpreted characters in the data stream apply
     * the character properties defined by the type-value pair.  The format
     * of the SA control sequence is as follows:
     * <TABLE border=1>
     * <TR>
     *    <TD>0x28</TD><TD>Attribute Type</TD><TD>Attribute Value</TD>
     * </TR>
     * </TABLE>
     */
    private synchronized void setAttribute() {
        int att = dataIn[++counter];

        switch (att) {
        case 0:
            foreground = 247;
            background = 240;
            highlight = 240;

            return;

        case XA_HIGHLIGHTING:
            highlight = dataIn[++counter];

            return;

        case XA_FGCOLOR:
            foreground = dataIn[++counter];

            return;

        case XA_BGCOLOR:
            background = dataIn[++counter];

            return;

        default:
            return;
        }
    }

    /**
     * <h3>4.3.5 Modify Field (MF)</h3>
     * The MF order begins a sequence that updates field and extended field
     * attributes at the current buffer address.  After the attributes have
     * been updated, the current buffer address is incremented by one.
     * <P>The MF control sequence has the following format:
     * </P>
     * <TABLE border=1>
     * <TR>
     *    <TD>0x2C</TD><TD>Number of Attribute Type/Value Pairs</TD>
     *    <TD>Attribute Type</TD><TD>Attribute Value</TD>
     * </TR>
     * </TABLE>
     * Gotchas:<BR>
     *
     * <UL>
     * <li><b>Attribute types not specified remain unchanged</b></li>
     * <li><b>If the current buffer address is not a field attribute, the MF
     * order should be rejected</b></li>
     * </ul>
     */
    private synchronized void modifyField() {
        // reject if not a FA
        RW3270Char currChar = (RW3270Char) chars[bufferAddr];

        //System.out.println(" " + currChar.isStartField());
        if (!currChar.isStartField()) {
            return;
        }

        int pairs = dataIn[++counter];

        for (int i = 0; i < pairs; i++) {
            //System.out.println("Attribute to modify: " + Integer.toHexString(dataIn[++counter]));
            switch (dataIn[++counter]) {
            case ORDER_SFE:
            case ORDER_SF:
            case XA_SF:
                currChar.setFieldAttribute(dataIn[++counter]);

                break;

            case XA_VALIDATION:
                currChar.setValidation(dataIn[++counter]);

                break;

            case XA_HIGHLIGHTING:
                currChar.setHighlighting(dataIn[++counter]);

                break;

            case XA_FGCOLOR:
                currChar.setForeground(dataIn[++counter]);

                break;

            case XA_BGCOLOR:
                currChar.setBackground(dataIn[++counter]);

                break;

            case XA_OUTLINING:
                currChar.setOutlining(dataIn[++counter]);

                break;

            default:
                counter++;
            }
        }

        bufferAddr++;
    }

    /**
     * <h3>4.3.6 Insert Cursor (IC)</h3>
     * The IC order repositions the cursor to the locations specified by the
     * current buffer address.  Execution of this order does not change the
     * current buffer address.
     */
    private synchronized void insertCursor() {
        rw.setCursorPosition((short) bufferAddr);
    }

    /**
     * <h3>4.3.7 Program Tab (PT)</h3>
     * The PT order advances the current buffer address to the address of
     * the first character position of the next unprotected field.  If PT
     * is issued when the current buffer address is the location of a field
     * attribute of an unprotected field, the buffer advances to the next
     * location of that field (one location).  In addition, if PT does not
     * immediately follow a command order, or order sequence (such as after
     * the WCC, IC, and RA respectively), nulls are inserted in the buffer
     * from the current buffer address to the end of the field, regardless of
     * the value of bit 2 (protected/unprotected) of the field attribute for
     * the field.  When PT immediately follows a command, order, or order
     * sequence, the buffer is not modified.
     */
    private synchronized void programTab() {
        log.finest("Program Tab...");

        int newAddr;
        int oldAddr = bufferAddr;
        newAddr = rw.getNextUnprotectedField(bufferAddr);

        //System.out.println("next unprotected: " + newAddr);
        if (newAddr <= bufferAddr) {
            bufferAddr = 0;
        } else {
            bufferAddr = newAddr;
        }

        if (!lastWasCommand) {
            //if bufferAddr = 0, there's no more FAs so
            //clear from here.
            //if(!chars[oldAddr].getField().isProtected())
            //   newAddr = oldAddr;
            //else
            RW3270Char currChar = null;

            while ((oldAddr < chars.length) &&
                    !(currChar = chars[oldAddr]).isStartField()) {
                currChar.clear();
                oldAddr++;
            }

            //System.out.println("Buffer Address.." + bufferAddr);
            /*
               newAddr = (bufferAddr == 0)?oldAddr:bufferAddr;
               System.out.println("Get field..." + newAddr);
               int end = 0;
               try
               {
                  end = chars[oldAddr].getField().getEnd();
               }
               catch(NullPointerException e)
               {
                  e.printStackTrace();
               }
               System.out.println("Clearing: " + oldAddr + " to " + end);
               for(int c = oldAddr; c <= end; c++)
               {
                  try
                  {
                     chars[c].clear();
                     display[c] = ' ';
                  }
                  catch(ArrayIndexOutOfBoundsException e){}
               } */
        }
    }

    /**
     * <h3>4.3.8 Repeat to Address (RA)</h3>
     * The RA order stores a specified character in all character buffer
     * locations, starting at the current bufer address and ending at
     * (but not including) the specified stop address.
     */
    private synchronized void repeatToAddress() {
        //counter++;
        int address = setBufferAddress();
        int charIn = dataIn[++counter];
        char c = (char) ebc2asc[charIn];

        while (bufferAddr != address) {
            RW3270Char currChar = (RW3270Char) chars[bufferAddr];
            currChar.clear();
            currChar.setForeground(foreground);
            currChar.setBackground(background);
            currChar.setHighlighting(highlight);
            currChar.setChar(c);
            display[bufferAddr] = c;

            if (++bufferAddr > (chars.length - 1)) {
                bufferAddr = 0;
            }
        }
    }

    /**
     * <h3>4.3.9 Erase Unprotected to Address (EUA)</h3>
     * The EUA Order stores nulls in all unprotected character locations,
     * starting at the current buffer address and ending at, but not
     * including, the specified stop address.
     */
    private synchronized void eraseUnprotectedToAddress() {
        //counter++;

        int address = setBufferAddress();

        if (address == bufferAddr) {
            eraseAllUnprotected();
        }

        while (bufferAddr < address) {
            RW3270Char currChar = (RW3270Char) chars[bufferAddr];
            RW3270Field f = currChar.getField();

            if (!f.isProtected()) {
                currChar.setChar((char) 0);
                display[currChar.getPosition()] = ' ';

                if (currChar.isStartField()) {
                    currChar.isModified(false);
                }
            }

            if (++bufferAddr > (chars.length - 1)) {
                bufferAddr = 0;
            }
        }
    }

    private synchronized void graphicEscape() {
        //not supported
        counter++;
    }

    /**
     * This is a utility method that builds the field vector after data comes in by reading
     * the data buffer and creating a Field Object for each Start Field character.
     *
     * <p> The Field objects merely 'point' the the corresponding
     * Start Field character for conceptual ease for end-programmers.
     * No data is 'contained' in a field object
     */
    private synchronized void buildFields() {
        fields.removeAllElements();

        RW3270Field lastField = null;


        for (int i = 0; i < chars.length; i++) {
            RW3270Char currChar = chars[i];

            if (currChar.isStartField()) {
                if (lastField != null) {
                    lastField.setEnd(i - 1);
                }

                //since it's a Start Field FA, create a new field
                RW3270Field currField = new RW3270Field(currChar, rw);

                //set it's begin point as the current counter position
                currField.setBegin(i);

                //add it to the fields vector
                fields.addElement(currField);

                //move it to the last field variable, so we can set its
                //end point.
                lastField = currField;
            }

            currChar.setField(lastField);
        }

        // now we have to find the end point for the last field.  We
        // can't just set it to the last address in the buffer,
        // because fields that aren't terminated wrap to the beginning
        // of the buffer.
        if (fields.size() > 0) {
            RW3270Field firstField = (RW3270Field) fields.elementAt(0);
            lastField.setEnd((firstField.getBegin() == 0) ? chars.length
                                                          : (firstField.getBegin() -
                1));

            for (int c = 0; c < firstField.getBegin(); c++) {
                chars[c].setField(lastField);
            }
        }
    }
    /**
     * This method builds the reply packet to send to the host, it contains our capabilities
     * as a 3270 host.
     *
     */
    private synchronized short[] buildQueryReply( int model, boolean summary ) {
    	/*
    	 * We have several capabilities which we need to report
    	 * 1. Color
    	 * 2. Highlighting
    	 * 3. Partition
    	 */
    	final short HIGHLIGHT_DEFAULT 		= 0x00;
    	final short HIGHLIGHT_NORMAL  		= 0xF0;
    	final short HIGHLIGHT_BLINK			= 0xF1;
    	final short HIGHLIGHT_REVERSE		= 0xF2;
    	final short HIGHLIGHT_UNDERSCORE	= 0xF4;
    	final short HIGHLIGHT_INTENSIFY		= 0xF8;
    	
    	/* Colors: Listed in 6.13.3 */
    	final short COLOR_NEUTRAL1			= 0x00;
    	final short COLOR_BLUE				= 0xF1;
    	final short COLOR_RED				= 0xF2;
    	final short COLOR_PINK				= 0xF3;
    	final short COLOR_GREEN				= 0xF4;
    	final short COLOR_TURQUOISE			= 0xF5;
    	final short COLOR_YELLOW			= 0xF6;
    	final short COLOR_NEUTRAL2			= 0xF7;
    	final short COLOR_BLACK				= 0xF8;
    	final short COLOR_DEEP_BLUE			= 0xF9;
    	final short COLOR_ORANGE			= 0xFA;
    	final short COLOR_PURPLE			= 0xFB;
    	final short COLOR_PALE_GREEN		= 0xFC;
    	final short COLOR_PALE_TURQUOISE	= 0xFD;
    	final short COLOR_GREY				= 0xFE;
    	final short COLOR_WHITE				= 0xFF;
    	
    	final short QUERY_REPLY				= 0x81;
    	final short SUMMARY_QUERY_REPLY		= 0x80;
    	final short COLOR_QUERY_REPLY 		= 0x86;
    	final short HIGHLIGHT_QUERY_REPLY	= 0x87;
    	final short IMP_PART_QUERY_REPLY	= 0xA6;
    	
    	/* Highlighting */
    	short[] highlightReply = new short[15];
    	/* Bytes 0-1 Length of the Structured Field */
    	highlightReply[0]  = (short)0x00;
    	highlightReply[1]  = (short)0x0F;
    	/* Byte 2 Query Reply */
    	highlightReply[2]  = QUERY_REPLY;
    	/* Byte 3 Highlighting */
    	highlightReply[3]  = HIGHLIGHT_QUERY_REPLY;
    	/* Byte 4 Number of attribute-value/action pairs */
    	highlightReply[4]  = (short)0x50; 
    	/* Part 1: Data stream attribute value accepted  */
    	/* Part 2: Data stream action */
    	/* Pair 1 */
        highlightReply[5]  = HIGHLIGHT_DEFAULT;
        highlightReply[6]  = HIGHLIGHT_NORMAL;
        /* Pair 2 */
        highlightReply[7]  = HIGHLIGHT_BLINK;
        highlightReply[8]  = HIGHLIGHT_BLINK;
        /* Pair 3 */
        highlightReply[9]  = HIGHLIGHT_REVERSE;
        highlightReply[10] = HIGHLIGHT_REVERSE;
        /* Pair 4 */
        highlightReply[11] = HIGHLIGHT_UNDERSCORE;
        highlightReply[12] = HIGHLIGHT_UNDERSCORE;
        /* Pair 5 */
        highlightReply[13] = HIGHLIGHT_INTENSIFY;
        highlightReply[14] = HIGHLIGHT_INTENSIFY;
        
    	/* Color */
        short[] colorReply = new short[40];
        
        /* Bytes 0-1 Length of the Structured Field */
        colorReply[0]  = (short)0x00;
        colorReply[1]  = (short)0x26;
        
        colorReply[2]  = QUERY_REPLY;
        colorReply[3]  = COLOR_QUERY_REPLY;
        
        colorReply[4]  = (short)0x00;
        /* Number of Pairs */
        colorReply[5]  = (short)0x10;
        
        /* Pair 1 */
        colorReply[6]  = COLOR_NEUTRAL1;
        colorReply[7]  = COLOR_WHITE;
        /* Pair 2 */
        colorReply[8]  = COLOR_BLUE;
        colorReply[9]  = COLOR_BLUE;
        /* Pair 3 */
        colorReply[10] = COLOR_RED;
        colorReply[11] = COLOR_RED;
        /* Pair 4 */
        colorReply[12] = COLOR_PINK;
        colorReply[13] = COLOR_PINK;
        /* Pair 5 */
        colorReply[14] = COLOR_GREEN;
        colorReply[15] = COLOR_GREEN;
        /* Pair 6 */
        colorReply[16] = COLOR_TURQUOISE;
        colorReply[17] = COLOR_TURQUOISE;
        /* Pair 7 */
        colorReply[18] = COLOR_YELLOW;
        colorReply[19] = COLOR_YELLOW;
        /* Pair 8 */
        colorReply[20] = COLOR_NEUTRAL2;
        colorReply[21] = COLOR_NEUTRAL2;
        /* Pair 9 */
        colorReply[22] = COLOR_BLACK;
        colorReply[23] = COLOR_BLACK;
        /* Pair 10 */
        colorReply[24] = COLOR_DEEP_BLUE;
        colorReply[25] = COLOR_DEEP_BLUE;
        /* Pair 11 */
        colorReply[26] = COLOR_ORANGE;
        colorReply[27] = COLOR_ORANGE;
        /* Pair 12 */
        colorReply[28] = COLOR_PURPLE;
        colorReply[29] = COLOR_PURPLE;
        /* Pair 13 */
        colorReply[30] = COLOR_PALE_GREEN;
        colorReply[31] = COLOR_PALE_GREEN;
        /* Pair 14 */
        colorReply[32] = COLOR_PALE_TURQUOISE;
        colorReply[33] = COLOR_PALE_TURQUOISE;
        /* Pair 15 */
        colorReply[34] = COLOR_GREY;
        colorReply[35] = COLOR_GREY;
        /* Pair 16 */
        colorReply[36] = COLOR_WHITE;
        colorReply[37] = COLOR_WHITE;
        /* Pair 17 */
        colorReply[38] = COLOR_GREY;
        colorReply[39] = COLOR_GREY;
        
        /* Implicit Partition. See 6.31.2 */        
        short[] partitionReply = new short[17];
        
        partitionReply[0]  = (short)QUERY_REPLY;
        /* Bytes 1-2 Length */
        partitionReply[1]  = (short)0x00;
        partitionReply[2]  = (short)0x11;
        /* Byte 3 QCODE Identifier */
        partitionReply[3]  = (short)IMP_PART_QUERY_REPLY;
        /* Bytes 4-5 Reserved */
        partitionReply[4]  = (short)0x00;
        partitionReply[5]  = (short)0x00;
        /* 6.31.3 Implicit Partition Sizes for Display Devices Self-Defining Parameter */
        partitionReply[6]  = (short)0x0B;
        partitionReply[7]  = (short)0x01;
        partitionReply[8]  = (short)0x00;
        /* Bytes 9-10   Width of the Implicit Partition default screen size (in character cells) */
        partitionReply[9]  = (short)0x00;
        partitionReply[10] = (short)0x50;
        /* Bytes 11-12  Height of the Implicit Partition default screen size */
        partitionReply[11] = (short)0x00;
        partitionReply[12] = (short)0x18;
        /* FIXME The alternate size should be the dimensions of the terminal model selected */
        /* Bytes 13-14  Width of the Implicit Partition alternate screen size */
        partitionReply[13] = (short)0x00;
        partitionReply[14] = (short)0x50;
        /* Bytes 15-16  Height of the Implicit Partition alternate screen size */
        partitionReply[15] = (short)0x00;
        partitionReply[16] = (short)0x18;
        
        
        /* Summary */
        short[] summaryReply = new short[8];
        summaryReply[0]  = (short)0x00;
        summaryReply[1]  = (short)0x08;
        /* Byte 2 Query Reply */
        summaryReply[2]  = QUERY_REPLY;
        /* Byte 3 Summary Query Reply */
        summaryReply[3]  = SUMMARY_QUERY_REPLY;
        /* These are our capabilities...
         * Kind of silly to indicate we're capable of a summary reply
         * in a summary reply...that's how it works though.
         */
        summaryReply[4]  = SUMMARY_QUERY_REPLY;
        summaryReply[5]  = COLOR_QUERY_REPLY;
        summaryReply[6]  = HIGHLIGHT_QUERY_REPLY;
        summaryReply[7]  = IMP_PART_QUERY_REPLY;
        
        /* Assembly of the Reply Packet */
        
        /* Create a buffer the length of each of the member pieces, plus the header and footer */
        int qReplyLength = 1 + summaryReply.length + highlightReply.length 
        					 + colorReply.length   + partitionReply.length;
        
        /* Initialize the queryReply packet buffer */
        short[] queryReply = new short[qReplyLength];
        
        
        queryReply[0] = 0x88;
        int bufPos = 1;
        
        /* Add the summary Capability */
        for( int i = 0; i < summaryReply.length; i++ ) {
        	queryReply[bufPos] = summaryReply[i];
        	bufPos++;
        }
        
        /* Add the Color Capability */
        for( int i = 0; i < colorReply.length; i++ ) {
        	queryReply[bufPos] = colorReply[i];
        	bufPos++;
        }

        /* Add the Highlight Capability */
        for( int i = 0; i < highlightReply.length; i++ ) {
        	queryReply[bufPos] = highlightReply[i];
        	bufPos++;
        }
        
        /* Add the Partition Capability */
        for( int i = 0; i < partitionReply.length; i++ ) {
        	queryReply[bufPos] = partitionReply[i];
        	bufPos++;
        }
        
        
        /*for(int i = 0; i < queryReply.length; i++ ) {
        	String myStr = Long.toHexString(new Short( queryReply[i] ).longValue());
        	System.out.print( myStr + " " );
        }*/
    	return queryReply;
    }
}
