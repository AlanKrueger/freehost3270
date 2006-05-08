/*
 * FreeHost3270 a suite of terminal 3270 access utilities.
 * Copyright (C) 1998, 2001  Art Gillespie
 * Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *                        Project Contributors.
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
 */


package net.sf.freehost3270.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import net.sf.freehost3270.client.IsProtectedException;
import net.sf.freehost3270.client.RW3270;
import net.sf.freehost3270.client.RW3270Char;
import net.sf.freehost3270.client.RW3270Field;
import net.sf.freehost3270.client.RWTnAction;


/**
 * A SWING component that interactively renders terminal screen contents and
 * handles user to terminal interaction.
 * 
 * <p>
 * The terminal is first rendered to a buffered image and then, whan a
 * component repaint is requested this buffered image is painted on the given
 * component's graphics context. In case if a default font size is changed,
 * the <code>JTerminalScreen</code> adjusts it size in order to feed the
 * grown terminal screen size.
 * </p>
 *
 * @see #paintComponent(Graphics)
 * @see #setFont(Font)
 * @since 0.1 RHPanel
 */
public class JTerminalScreen extends JPanel implements RWTnAction, KeyListener, Printable {
	
    private static final Logger log = Logger.getLogger(JTerminalScreen.class.getName());

    /**
     * The default width of the panel. Should be overriden upon panel
     * initialization or font size change.
     */
    public static final int DEFAULT_WIDTH = 800;

    /**
     * The default height of the panel.
     *
     * @see #DEFAULT_WIDTH
     */
    public static final int DEFAULT_HEIGHT = 600;
    private static final int MSG_CLOSED_REMOTE = 0;
    private static final int MSG_STRING = 1;
    private static final int MSG_BROADCAST = 2;

    /** Default size of the screen font. */
    public static final int DEFAULT_FONT_SIZE = 14;

    /** Default font used to draw terminal. */
    public static final Font DEFAULT_FONT = new Font("Monospaced", Font.PLAIN,
            DEFAULT_FONT_SIZE);

    /** Default terminal foreground color. */
    public static final Color DEFAULT_FG_COLOR = Color.CYAN;

    /** Default terminal background color. */
    public static final Color DEFAULT_BG_COLOR = Color.BLACK;

    /** Default font color used to render bold text. */
    public static final Color DEFAULT_BOLD_COLOR = Color.WHITE;

    /** Default color used to render cursor pointer on the terminal screen. */
    public static final Color DEFAULT_CURSOR_COLOR = Color.RED;

    /** Default input field background color. */
    public static final Color DEFAULT_FIELD_BG_COLOR = new Color(45, 45, 45);
    public static final int MARGIN_LEFT = 5;
    public static final int MARGIN_TOP = 6;

    // buffer used to draw the screen in background.
    private BufferedImage frameBuff;
    private Color boldColor = DEFAULT_BOLD_COLOR;
    private Color currentBGColor = DEFAULT_BG_COLOR;
    private Color currentFGColor = DEFAULT_FG_COLOR;
    private Color currentFieldBGColor = DEFAULT_FIELD_BG_COLOR;
    private Color cursorColor = DEFAULT_CURSOR_COLOR;
    private Font font = DEFAULT_FONT;

    // graphics context of the background buffer. Use this context to
    // paint the components of the screen.
    private Graphics2D frame;
    private RW3270 rw;

    /** Current status message. */
    private String statusMessage;
    private String currentPosition;
    private String strMessage;
    private boolean messageOnScreen;
    private boolean tooManyConnections;
    private int char_ascent;
    private int char_height;
    private int char_width;
    private int fontsize = DEFAULT_FONT_SIZE;
    private int messageNumber;
    private Point rectStartPoint;
    
    /**
     * Construct a new GUI session with a terminalModel of 2, and a terminalType of 3279-E.
     */
    public JTerminalScreen() {
        super();
        
        rw = new RW3270(2, this);
        
        init();
    }
    /**
     * Construct a new GUI session with the specified terminalModel and terminalType.
     * 
     * @param terminalModel
     * @param terminalType
     */
    public JTerminalScreen( int terminalModel, String terminalType ) {
    	super();
    	
    	rw = new RW3270( terminalModel, this );
    	
    	init();
    }
    
    /**
     * Construct a new GUI session with a 3279-E terminalType and the specified model 
     * where 2 <= terminalModel <= 5. 
     * @param terminalModel
     */
    public JTerminalScreen( int terminalModel ) {
    	super();
    	
    	rw = new RW3270( terminalModel, this );
    	
    	init();
    }
    
    /**
     * Construct a new GUI session with a model 2 (24x80) screen using either a
     * 3278, 3279, or 3279-E terminalType.
     * 
     * @param terminalType
     */
    public JTerminalScreen( String terminalType ) {
    	super();
    	
    	rw = new RW3270( 2, this );
    	
    	init();
    }
    
    private void init() {
        frameBuff = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        frame = frameBuff.createGraphics();
        messageOnScreen = false;
        rectStartPoint = new Point();
        setBackground(currentBGColor);
        setFont(DEFAULT_FONT);
        addKeyListener(this);
        MouseInputAdapter mouseAdapter = (new MouseInputAdapter() {
        	public void mouseClicked(MouseEvent e) {
        		JTerminalScreen.this.mouseClicked(e);
        	}
        	public void mousePressed(MouseEvent e) {
        		JTerminalScreen.this.mousePressed(e);
        	}
        	    
        	public void mouseReleased(MouseEvent e) {
        		JTerminalScreen.this.mouseReleased(e);
        	}
        	
        	public void mouseDragged(MouseEvent e) {
        		JTerminalScreen.this.mouseDragged(e);
        	}
        });
        addMouseListener( mouseAdapter );
        addMouseMotionListener( mouseAdapter );

        // originally, JPanel does not recieve focus
        setFocusable(true);

        // to catch VK_TAB et al.
        setFocusTraversalKeysEnabled(false);
        setVisible(true);
        requestFocus();
    }

    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    public void broadcastMessage(String msg) {
        log.fine("broadcast message: " + msg);
        strMessage = msg;

        if (msg.indexOf("<too many connections>") != -1) {
            strMessage = msg.substring(23);
            tooManyConnections = true;
        }

        setWindowMessage(MSG_BROADCAST);
    }

    /**
     * Opens a connection to the destination terminal server host.
     *
     * @param host
     * @param port
     * @param host3270
     * @param port3270
     * @param encryption DOCUMENT ME!
     */
    public void connect(String host, int port, String host3270, int port3270,
        boolean encryption) {
        if (host == null) {
            log.warning("called connect with null host");

            return;
        }

        messageOnScreen = false;
        log.info("connecting " + host + ":" + port + "; " + host3270 + ":" +
            port3270);
        setStatus("Connecting");
        rw.connect(host, port, host3270, port3270, encryption);
        log.info("connecting complete");
    }

    public void connect(String host, int port)
        throws IOException, UnknownHostException {
        if (host == null) {
            log.warning("called connect with null host");

            return;
        }

        messageOnScreen = false;
        log.info("connecting " + host + ":" + port);
        setStatus("Connecting");
        rw.connect(host, port);
        log.info("connecting complete");
    }
    
    public void connect(String host, int port, boolean encryption)
    	throws IOException, UnknownHostException {
    	rw.setEncryption( encryption );
    	this.connect( host, port );
    }

    public void cursorMove(int oldPos, int newPos) {
        log.finest("moving cursor: " + oldPos + "->" + newPos);

        //paintCursor(oldPos);
        paintChar(rw.getChar(oldPos));
        paintChar(rw.getChar(newPos));
        paintCursor(newPos);
        repaint();
    }

    public void disconnect() {
        log.info("disconnecting");
        rw.disconnect();
        setStatus("Disconnected");
    }

    public RW3270 getRW3270() {
        return rw;
    }

    public void incomingData() {
        log.info("incoming data");
        renderScreen();
        repaint();
        log.info("finished processing incoming data");
    }
    
    public void PA( int paKey ) {
    	rw.PA( paKey );
    }
    
    public void enter() {
    	rw.enter();
    }
    
    public void clear() {
    	rw.clear();
    }
    
    /**
     * Processess non-character keyboard events. All control, meta, escape
     * arrow key typed events are processed here.
     *
     * @param evt DOCUMENT ME!
     */
    public void keyPressed(KeyEvent evt) {
        log.fine(evt.toString());

        synchronized (rw) {
            if (messageOnScreen) {
                if ((evt.getKeyCode() == KeyEvent.VK_ESCAPE) &&
                        !tooManyConnections) {
                    // if a message is displayed on the screen, and it is
                    // not "too many connections" clear the screen and
                    // proceed
                    messageOnScreen = false;
                    setWindowMessage(-1);
                    repaint();
                }
            } else {
                switch (evt.getKeyCode()) {
                case KeyEvent.VK_ESCAPE: {
                    rw.clear();

                    break;
                }

                case KeyEvent.VK_ENTER: {
                    rw.enter();

                    break;
                }

                case KeyEvent.VK_DELETE: {
                	if (evt.isControlDown()) {
                		/*
                		try {
                			rw.deleteField();
                			renderScreen();
                			repaint();
                		} catch (IsProtectedException e) {
	                        log.warning(e.getMessage());
	                    }
	                    */
                	} else {
	                    try {
	                        rw.delete();
	                    } catch (IsProtectedException e) {
	                        log.warning(e.getMessage());
	                    }
                	}
                    break;
                }

                case KeyEvent.VK_TAB: {
                    if (evt.isShiftDown()) {
                        //Shift-Tab is a standard Win32 shortcut for backtab
                        rw.backTab();
                    } else {
                        rw.tab();
                    }

                    evt.consume();

                    break;
                }

                case KeyEvent.VK_BACK_SPACE: {
                    try {
                        rw.backspace();
                    } catch (IsProtectedException e) {
                        log.warning(e.getMessage());
                    }

                    break;
                }

                case KeyEvent.VK_LEFT: {
                    rw.left();

                    break;
                }

                case KeyEvent.VK_RIGHT:
                    rw.right();

                    break;

                case KeyEvent.VK_UP:
                    rw.up();

                    break;

                case KeyEvent.VK_DOWN:
                    rw.down();

                    break;

                case KeyEvent.VK_PAGE_UP:
                    rw.PA(RW3270.PA1);

                    break;

                case KeyEvent.VK_PAGE_DOWN:
                    rw.PA(RW3270.PA2);

                    break;

                case KeyEvent.VK_HOME:
                    rw.home();

                    break;

                // function keys (F1, F2, and the like).
                case KeyEvent.VK_F1:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF13);
                    } else {
                        if (evt.isMetaDown()) {
                            rw.PA(RW3270.PA1);
                        } else {
                            rw.PF(RW3270.PF1);
                        }
                    }

                    return;

                case KeyEvent.VK_F2:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF14);
                    } else if (evt.isMetaDown()) {
                        rw.PA(RW3270.PA2);
                    } else {
                        rw.PF(RW3270.PF2);
                    }

                    return;

                case KeyEvent.VK_F3:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF15);
                    } else if (evt.isMetaDown()) {
                        rw.PA(RW3270.PA3);
                    } else {
                        rw.PF(RW3270.PF3);
                    }

                    return;

                case KeyEvent.VK_F4:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF16);
                    } else {
                        rw.PF(RW3270.PF4);
                    }

                    return;

                case KeyEvent.VK_F5:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF17);
                    } else {
                        rw.PF(RW3270.PF5);
                    }

                    return;

                case KeyEvent.VK_F6:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF18);
                    } else {
                        rw.PF(RW3270.PF6);
                    }

                    return;

                case KeyEvent.VK_F7:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF19);
                    } else {
                        rw.PF(RW3270.PF7);
                    }

                    return;

                case KeyEvent.VK_F8:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF20);
                    } else {
                        rw.PF(RW3270.PF8);
                    }

                    return;

                case KeyEvent.VK_F9:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF21);
                    } else {
                        rw.PF(RW3270.PF9);
                    }

                    return;

                case KeyEvent.VK_F10:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF22);
                    } else {
                        rw.PF(RW3270.PF10);
                    }

                    return;

                case KeyEvent.VK_F11:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF23);
                    } else {
                        rw.PF(RW3270.PF11);
                    }

                    return;

                case KeyEvent.VK_F12:

                    if (evt.isShiftDown()) {
                        rw.PF(RW3270.PF24);
                    } else {
                        rw.PF(RW3270.PF12);
                    }

                    break;

                case KeyEvent.VK_F: {
                    if (evt.isControlDown()) {
                        rw.tab();
                    } else if (evt.isMetaDown() || evt.isAltDown()) {
                        rw.backTab();
                    }

                    break;
                }

                case KeyEvent.VK_C: {
                    if (evt.isControlDown()) {
                        rw.clear();
                    }

                    break;
                }

                case KeyEvent.VK_E: {
                    if (evt.isControlDown()) {
                        try {
                            rw.delete();
                        } catch (IsProtectedException e) {
                            log.warning(e.getMessage());
                        }
                    }

                    break;
                }

                case KeyEvent.VK_M: {
                    if (evt.isControlDown()) {
                        rw.keyFieldMark();
                    }

                    break;
                }

                case KeyEvent.VK_N: {
                    if (evt.isControlDown()) {
                        rw.keyNewLine();
                    }

                    break;
                }

                case KeyEvent.VK_R: {
                    if (evt.isControlDown()) {
                        rw.reset();
                    }

                    break;
                }

                case KeyEvent.VK_S: {
                    if (evt.isControlDown()) {
                        rw.sysreq();
                    }

                    break;
                }

                case KeyEvent.VK_T: {
                    if (evt.isControlDown()) {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                            .focusNextComponent(this);
                    } else if (evt.isMetaDown() || evt.isAltDown()) {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                            .focusPreviousComponent(this);
                    }
                }
                }
            }
        }
    }

    /**
     * To satisfy interface declarations.
     *
     * @param evt DOCUMENT ME!
     */
    public void keyReleased(KeyEvent evt) {
        log.fine(evt.toString());
    }

    public void keyTyped(KeyEvent evt) {
        log.fine(evt.toString());

        if (messageOnScreen) {
            if (messageNumber != MSG_BROADCAST) {
                return;
            }
        }

        synchronized (rw) {
            char typedChar = evt.getKeyChar();

            if ((typedChar != KeyEvent.CHAR_UNDEFINED) &&
                    (!evt.isControlDown()) && (!evt.isAltDown()) &&
                    (!evt.isMetaDown()) && (!(typedChar == KeyEvent.VK_TAB)) &&
                    (!(typedChar == KeyEvent.VK_BACK_SPACE)) &&
                    (!(typedChar == KeyEvent.VK_DELETE))) {
                // the typed key generated some character
                try {
                    // First, we'll see if the field is numeric, and if so, if the input
                    // is numeric:
                    RW3270Field field = rw.getField();

                    if (field == null) {
                        return;
                    }

                    if (field.isNumeric() && (!isAlnum(typedChar))) {
                        return;
                    }

                    rw.type(typedChar);

                    if (rw.getChar(rw.getCursorPosition()).getField()
                              .isProtected() ||
                            rw.getChar(rw.getCursorPosition()).isStartField()) {
                        int oldpos = rw.getCursorPosition();
                        rw.setCursorPosition(rw.getNextUnprotectedField(
                                rw.getCursorPosition()));
                        cursorMove(oldpos, rw.getCursorPosition());
                    }

                    paintField(field, false);
                    paintCursor(rw.getCursorPosition());
                } catch (IsProtectedException e) {
                    log.warning(e.getMessage());
                }
            } else {
                log.fine("typed char is undefined");
            }
        }

        return;
    }

    /**
     * Dispatches mouse event. In case if <code>BUTTON1</code> is clicked,
     * moves terminal cursor to the character, on which the mouse event
     * occured.
     *
     * @param e DOCUMENT ME!
     */
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            log.finest("mouse clicked at: (" + e.getX() + ", " + e.getY() +
                ")");

            int oldpos = rw.getCursorPosition();
            double dx = e.getX() - MARGIN_LEFT;
            double dy = e.getY() - MARGIN_TOP;

            if ((dx >= 0) && (dy >= 0)) {
                int newpos = (((int) Math.floor(dx / char_width)) +
                    ((int) Math.floor(dy / char_height) * 80)) - 1;

                if (newpos >= 0 && newpos < ( rw.getCols() * rw.getRows() )  ) {
                    rw.setCursorPosition((short) (newpos));
                    cursorMove(oldpos, newpos);
                }
            }
        }

        requestFocus();
    }
    
    public void mousePressed(MouseEvent e) {
    	/* Save the initial point for the rectangle */
    	rectStartPoint.x = e.getX();
    	rectStartPoint.y = e.getY();
    	
    }
    
    public void mouseReleased(MouseEvent e) {
    	repaint();
    }
    
    public void mouseDragged(MouseEvent e) {
    	Rectangle rect = null;
    	
    	int eventY = e.getY();
    	int eventX = e.getX();
    	renderScreen();
    	
    	frame.setColor( Color.WHITE );
    	
    	/* Quadrant IV */
    	if( (rectStartPoint.x < eventX) && (rectStartPoint.y < eventY ) ) {
    		rect = new Rectangle( rectStartPoint.x, rectStartPoint.y, 
    				eventX-rectStartPoint.x, eventY - rectStartPoint.y ); 		
    	}
    	/* Quadrant I */
    	else if( rectStartPoint.x < eventX ) {
    		rect = new Rectangle( rectStartPoint.x, eventY, 
    				eventX-rectStartPoint.x,  rectStartPoint.y - eventY);
    		
    	} else if( rectStartPoint.y < eventY ) {
    		/* Quadrant III */
    		rect = new Rectangle( eventX, rectStartPoint.y, 
    				rectStartPoint.x - eventX, eventY - rectStartPoint.y );
    		
    	} else {
    		/* Quadrant II */
    		rect = new Rectangle( eventX, eventY, 
    				rectStartPoint.x-eventX, rectStartPoint.y-eventY );
    		
    	}
    	frame.draw( rect );
    	repaint();
    }
    
    public void paintCursor(int pos) {
    	setCurrentPosition( pos );
        //System.out.println(rw.getCols() + " = " + rw.getCols());
        frame.setFont(font);
        frame.setColor(cursorColor);
        frame.fillRect(((pos % rw.getCols()) * char_width) + (char_width + 5),
            ((pos / rw.getCols()) * char_height) + 7, char_width,
            char_height - 2);
        frame.setColor(Color.black);

        byte[] c = { (byte) rw.getChar(pos).getDisplayChar() };
        frame.drawBytes(c, 0, 1,
            ((pos % rw.getCols()) * char_width) + (char_width + 5),
            ((pos / rw.getCols()) * char_height) + char_ascent + 5);
    }

    public void paintStatus() {
        frame.setColor(Color.red);
        
        frame.drawLine(char_width + 5,
            ((rw.getRows()) * char_height) + char_ascent,
            (rw.getCols() * char_width) + char_width + 5,
            ((rw.getRows()) * char_height) + char_ascent);
        
        frame.setColor(currentBGColor);
        
        frame.fillRect(char_width + 5, (rw.getRows() + 1) * char_height,
            (rw.getCols()) * char_width, char_height);
        
        frame.setColor(currentFGColor);

        if (statusMessage != null) {
            frame.drawString(statusMessage, char_width + 5,
                ((rw.getRows()) * char_height) + char_height + char_ascent);
        }
        
        if( currentPosition != null ) {
        	frame.drawString( currentPosition, (rw.getCols() - 6) * char_width,
        			((rw.getRows()) * char_height) + char_height + char_ascent);
        }
    }

    /**
     * Paints a message on the screen.
     */
    public void paintWindowMessage() {
        String message = null;
        messageOnScreen = true;

        // frame.setFont(font);
        frame.setColor(currentBGColor);
        frame.fillRect(3, 3, getSize().width - 6, getSize().height - 6);

        switch (messageNumber) {
        case MSG_CLOSED_REMOTE: {
            message = "Your connection to the FreeHost 3270 Session Server was lost or could not be established. " +
                "Please try your session again, and contact your system administrator if the problem persists.";

            break;
        }

        case MSG_STRING:
        case MSG_BROADCAST:
            message = strMessage;

            break;
        }

        frame.setColor(Color.red);
        frame.draw3DRect(5 + (char_width * 20), char_height * 2,
            char_width * 40, char_width * 40, true);
        frame.setColor(Color.white);
        frame.setFont(new Font("Helvetica", Font.PLAIN, fontsize));

        // the next few lines of code handle broadcast messages of
        // varying length and therefore had to be able to auto-wrap on
        // whitespace
        if (message.length() <= 40) {
            frame.drawString(message, char_width * 22, char_height * 3);
        } else {
            int lineNo = 0;

            for (int i = 0; i < message.length(); i++) {
                if ((message.length() - i) <= 45) {
                    frame.drawString(message.substring(i, message.length()),
                        char_width * 22, char_height * (3 + lineNo));

                    break;
                } else {
                    String line = message.substring(i, i + 45);
                    int lastSpace = line.lastIndexOf(' ');
                    frame.drawString(message.substring(i, i + lastSpace),
                        char_width * 22, char_height * (3 + lineNo));
                    i = i + lastSpace;
                    lineNo++;
                }
            }
        }

        if ((messageNumber == MSG_BROADCAST) && (tooManyConnections == false)) {
            frame.setFont(new Font("Helvetica", Font.BOLD, fontsize));
            frame.drawString("Message From Your System Administrator:",
                char_width * 22, (char_height * 2) - 5);
            frame.setFont(new Font("Helvetica", Font.PLAIN, fontsize - 2));
            frame.drawString("Press <ESC> to return to your session.",
                char_width * 22, char_height * 19);
        }
    }
    
    /**
     * Renders a terminal screen on the buffered image graphics context.
     */
    public void renderScreen() {
        //start the blink thread
        //if (t != null) {
        //      t.interrupt();
        //      t = null;
        //}
        //t = new Thread(this);
        //t.start();
        frame.setColor(currentBGColor);
        frame.fillRect(0, 0, char_width * 85, (char_height * 25) - 4);

        try {
            frame.setFont(font);

            Color bgcolor = translateColor(RW3270Field.DEFAULT_BGCOLOR);
            Color fgcolor = translateColor(RW3270Field.DEFAULT_FGCOLOR);

            // if the screen is unformatted, paint the entire
            // data buffer:
            log.finest("field count: " + rw.getFields().size());

            if (rw.getFields().size() < 1) {
                char[] c = rw.getDisplay();
                byte[] ca = new byte[c.length];

                for (int i = 0; i < c.length; i++) {
                    ca[i] = (byte) c[i];
                }

                paintData(ca, c.length, 0, bgcolor, fgcolor, false, false);
            } else {
                //formatted... check for fields:
                Enumeration fields = rw.getFields().elements();

                while (fields.hasMoreElements()) {
                    RW3270Field field = (RW3270Field) fields.nextElement();
                    paintField(field, false);
                }
            }

            paintStatus();
            paintCursor(rw.getCursorPosition());
        } catch (NullPointerException e) {
            e.printStackTrace();
            log.severe("exception in RHPanel.paintComponent: " +
                e.getMessage());
        }
    }

    /*
       public void paint(Graphics g) {
           if (g == null) {
               return;
           }
           //g.setColor(currentBGColor);
           //g.fillRect(0, 0, size().width, size().height);
           if (messageOnScreen) {
               paintWindowMessage();
               return;
           }
           repaint();
           status(RWTnAction.READY);
       }
     */
    public void run() {
        // blinked is a toggle.  When true,
        // the affected text is 'off'...
        boolean blinked = false;

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
                log.fine(e.getMessage());
            }

            Enumeration e = rw.getFields().elements();

            while (e.hasMoreElements()) {
                RW3270Field field = (RW3270Field) e.nextElement();

                if (field.getHighlighting() == RW3270Char.HL_BLINK) {
                    Graphics graphics = getGraphics();

                    if (graphics == null) {
                        log.warning("getGraphics returned null, in run");
                    }

                    paintField(field, blinked);
                }
            }

            paintCursor(rw.getCursorPosition());
            blinked = !blinked;
            log.finest("blink!");
        }
    }

    public void setBackgroundColor(Color c) {
        currentBGColor = c;
        setBackground(c);
        renderScreen();
        repaint();
    }

    public void setBoldColor(Color c) {
        boldColor = c;
        renderScreen();
        repaint();
    }

    /**
     * Sets the font used to draw the screen. Based on the given font metrics,
     * changes screen size in case if the background rendering buffer is not
     * null.
     *
     * @param newFont the new font to use for rendering terminal screen.
     */
    public void setFont(Font newFont) {
        super.setFont(newFont);
        log.finest("new font: " + newFont);
        font = newFont;
        fontsize = font.getSize();

        if (frame != null) {
            FontRenderContext context = frame.getFontRenderContext();
            Rectangle2D bound = font.getStringBounds("M", context);
            char_width = (int) Math.round(bound.getWidth());
            char_height = (int) Math.round(bound.getHeight());
            char_ascent = Math.round(font.getLineMetrics("M", context)
                                         .getAscent());

            //int width = x * 85;
            int width = char_width * 85;
            int height = (char_height * 27);
            setSize(width, height);
            setPreferredSize(new Dimension(width, height));
            frameBuff = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            frame = frameBuff.createGraphics();

            if (log.isLoggable(Level.FINE)) {
                log.fine("font metrics: " + char_width + " ; " + char_height);
            }

            renderScreen();
            repaint();
        }
    }

    public void setForegroundColor(Color c) {
        currentFGColor = c;
        renderScreen();
        repaint();
    }

    /**
     * Sets current status message and calls components <code>repaint</code>
     * method to make the new status message visible.
     *
     * @param statusMessage DOCUMENT ME!
     */
    public void setStatus(String statusMessage) {
        this.statusMessage = statusMessage;
        paintStatus();
        repaint();
    }
    public void setCurrentPosition( int newPos ) {
    	/* this is integer, not floating division */
    	int row = newPos / rw.getCols();
    	int col = newPos - ( row * rw.getCols() );
    	
    	NumberFormat rowFormat = NumberFormat.getInstance();
    	rowFormat.setMinimumIntegerDigits( 3 );
    	
    	String rowString = rowFormat.format( row + 1 );
    	String colString = rowFormat.format( col + 1 );
    	
    	this.currentPosition = rowString + "/" + colString;
    	
    	paintStatus();
    	repaint();
    }
    public void setWindowMessage(int msg) {
        messageNumber = msg;
        paintWindowMessage();
        repaint();
    }

    public void setWindowMessage(String msg) {
        strMessage = msg;
        paintWindowMessage();
        repaint();
    }

    public void status(int status) {
        switch (status) {
        case RWTnAction.X_WAIT: {
            setStatus("X-WAIT");

            break;
        }

        case RWTnAction.READY: {
            setStatus("Ready");

            break;
        }

        case RWTnAction.DISCONNECTED_BY_REMOTE_HOST: {
            setWindowMessage(MSG_CLOSED_REMOTE);

            break;
        }

        default:
            log.warning("status with id: " + status +
                " is not supported by JTerminalScreen");
        }
    }

    /**
     * Paints a char on the terminal panel.
     *
     * @param c DOCUMENT ME!
     */
    protected void paintChar(RW3270Char c) {
        //Graphics g = getGraphics();
        //if (g == null) {
        //  log.warning("getGraphics returned null, in paintChar");
        //}
        RW3270Field field = c.getField();
        int pos = c.getPosition();

        if (c.isStartField()) {
            frame.setColor(currentBGColor);
            frame.fillRect(((pos % rw.getCols()) * char_width) +
                (char_width + 5), ((pos / rw.getCols()) * char_height) + 7,
                char_width, char_height - 2);

            return;
        }

        byte[] ca = new byte[1];
        ca[0] = (byte) c.getDisplayChar();

        Color bgcolor = translateColor(field.getBackgroundColor());
        Color fgcolor = translateColor(field.getForegroundColor());
        boolean underscore = false;
        boolean hidden = false;

        switch (field.getHighlighting()) {
        case RW3270Char.HL_REVERSE: {
            fgcolor = bgcolor;
            bgcolor = translateColor(field.getForegroundColor());

            break;
        }

        case RW3270Char.HL_UNDERSCORE: {
            underscore = true;

            break;
        }
        }

        /*if(f.isHidden())
           hidden = true;*/
        if (field.isBold()) {
            if (fgcolor == currentFGColor) {
                fgcolor = boldColor;
            }
        }

        if (!field.isProtected()) {
            bgcolor = currentFieldBGColor;
        }

        frame.setFont(font);

        //System.out.println("Print data..." + ca[0] + " pos: " + pos + " bgcolor: " + bgcolor);
        //we have to draw the background
        frame.setColor(bgcolor);
        frame.fillRect(((pos % rw.getCols()) * char_width) + (char_width + 5),
            ((pos / rw.getCols()) * char_height) + 7, char_width,
            char_height - 2);

        paintData(ca, 1, pos, bgcolor, fgcolor, underscore, hidden);
    }

    /**
     * Paints backgoround buffered image on the given graphics context.
     *
     * @param g DOCUMENT ME!
     */
    protected void paintComponent(Graphics g) {
        g.drawImage(frameBuff, 0, 0, this);
    }
    
    public int print( Graphics graphics, PageFormat pageFormat, 
    		int pageIndex ) throws PrinterException {
    	/*
    	 * we can only print the current 3270 terminal screen image. There's no such thing as
    	 * multiple pages, and we're not implementing a client side terminal printer.
    	 */
    	if (pageIndex > 0) {
    		return Printable.NO_SUCH_PAGE;
    	}
    	
    	Graphics2D graphics2d = (Graphics2D)graphics;
    	graphics2d.translate( pageFormat.getImageableX(), pageFormat.getImageableY() );
    	graphics2d.setFont(  new Font( "Monospaced", Font.PLAIN, 10 ) );
    	
       	FontMetrics fontMetrics = graphics2d.getFontMetrics();
   	
    	char[] c = rw.getDisplay();
    	
    	StringBuffer textBuffer = new StringBuffer( rw.getCols() );
    	/*
    	 * center the display on the page.
    	 */
    	float yPos = (float) ( pageFormat.getImageableHeight() / 2 ) 
    		- ( (rw.getRows() * fontMetrics.getHeight() ) / 2 );
    	
    	/*
    	 * we're using a fixed width font, so we can get the width
    	 * of any character. It should equal the width of every
    	 * other character.
    	 */
    	float xPos = (float) (pageFormat.getImageableWidth() / 2) 
    		- ( (rw.getCols() * fontMetrics.stringWidth( " " ) ) / 2 ) + (72/10);
    	
    	for( int i = 1; i < c.length; i++ ) {
    		if( i % rw.getCols() == 0 ) {		
    			textBuffer.append( c[i] );
    			graphics2d.drawString( textBuffer.toString(), xPos, yPos);
    			
    			textBuffer.delete( 0, textBuffer.length() );
    			yPos += fontMetrics.getHeight();
    		} else {
    			textBuffer.append( c[i] );
    		}
    	}
    	
    	yPos += fontMetrics.getHeight();
    	graphics2d.drawString( textBuffer.toString(), xPos, yPos);
    	
    	return Printable.PAGE_EXISTS;
    }

    private boolean isAlnum(char c) {
        return ((c == '0') || (c == '1') || (c == '2') || (c == '3') ||
        (c == '4') || (c == '5') || (c == '6') || (c == '7') || (c == '8') ||
        (c == '9'));
    }

    /**
     * This utility/reuse method takes an array of chars, the starting
     * position, and a graphics object to paint a field, screen, character,
     * etc.
     *
     * @param c array of characters.
     * @param bufLen length of c.
     * @param startPos the starting position, relative to the databuffer.
     * @param bgcolor This component's graphics context.
     * @param fgcolor DOCUMENT ME!
     * @param under DOCUMENT ME!
     * @param hidden DOCUMENT ME!
     */
    private synchronized void paintData(byte[] c, int bufLen, int startPos,
        Color bgcolor, Color fgcolor, boolean under, boolean hidden) {
        log.finest("painting data");

        //number of columns in this screen
        //startPos = ((startPos + 1)%cols == 0)?startPos + 1:startPos;
        //a counter for keeping our place relative to the
        //character buffer passed to this method
        int counter = 0;

        //the first row this field will appear on
        int firstRow = startPos / rw.getCols();

        //the starting position, relative to the first row this field
        //appears on
        int firstRowStart = startPos % rw.getCols();

        //the number of characters that will appear on the first row
        int firstRowLen = rw.getCols() - firstRowStart;
        firstRowLen = (firstRowLen > bufLen) ? bufLen : firstRowLen;

        //the number of rows (not including first
        //and last) that this field will take
        int loops = (bufLen - firstRowLen) / rw.getCols();

        //System.out.println("rw.getCols(): " + cols + "Start pos: " + startPos + " First row: " + firstRow + " " + "First Row Start: " + firstRowStart + " FirstRowLen " + firstRowLen + " loops " + loops);
        //the number of characters that will appear
        //in the last row of this field.
        int lastRowLen = (bufLen - firstRowLen) % rw.getCols();
        firstRow = (firstRow == rw.getRows()) ? 0 : firstRow;

        // now let's draw the first row:
        frame.setColor(bgcolor);
        frame.fillRect(((firstRowStart + 1) * char_width) + (char_width + 5),
            (firstRow * char_height) + 7, (firstRowLen - 1) * char_width,
            char_height - 2);
        frame.setColor(fgcolor);

        // draw the underline, if appropriate
        if (under) {
            frame.drawLine(((firstRowStart + 1) * char_width) +
                (char_width + 5), (firstRow * char_height) + 5 + char_height,
                ((firstRowLen + firstRowStart) * char_width) +
                (char_width + 5), (firstRow * char_height) + 5 + char_height);
        }

        /*if(hidden)
           g.setColor(bgcolor);*/
        frame.drawBytes(c, counter, firstRowLen,
            ((firstRowStart) * char_width) + (char_width + 5),
            (firstRow * char_height) + char_ascent + 5);
        counter += firstRowLen;

        //System.out.println(counter);
        // iterate through the 'full' rows
        for (int i = 0; i < loops; i++) {
            firstRow++;
            firstRow = (firstRow == rw.getRows()) ? 0 : firstRow;
            frame.setColor(bgcolor);
            frame.fillRect(char_width + 5, (firstRow * char_height) + 7,
                rw.getCols() * char_width, char_height + 2);
            frame.setColor(fgcolor);

            if (under) {
                frame.drawLine(char_width + 5,
                    (firstRow * char_height) + 5 + char_height,
                    (rw.getCols() * char_width) + 5,
                    (firstRow * char_height) + 5 + char_height);
            }

            /*if(hidden)
               g.setColor(bgcolor);*/
            frame.drawBytes(c, counter, rw.getCols(), char_width + 5,
                (firstRow * char_height) + char_ascent + 5);

            //System.out.println(x + " " + y);
            counter += rw.getCols();

            //System.out.println(counter);
        }

        //if we're done, return
        if (counter >= (bufLen - 1)) {
            return;
        }

        //increment the row
        firstRow++;
        firstRow = (firstRow == rw.getRows()) ? 0 : firstRow;

        //now draw the last, partial row
        frame.setColor(bgcolor);
        frame.fillRect(char_width + 5, (firstRow * char_height) + 7,
            (lastRowLen) * char_width, char_height + 2);
        frame.setColor(fgcolor);

        if (under) {
            frame.drawLine(char_width + 5,
                (firstRow * char_height) + 5 + char_height,
                ((lastRowLen) * char_width) + 5,
                (firstRow * char_height) + 5 + char_height);
        }

        /*if(hidden)
           g.setColor(bgcolor);*/
        frame.drawBytes(c, counter, lastRowLen, char_width + 5,
            (firstRow * char_height) + char_ascent + 5);
        log.finest("drawing data complete");
    }

    /**
     * Paints a field on a terminal panel.
     *
     * @param field The current graphics context
     * @param blink Is this a blink off iteration?
     */
    private synchronized void paintField(RW3270Field field, boolean blink) {
        log.finest("paintField");

        /*for(int i = 0; i < ca.length; i++)
           {
              ca[i] = c[i].getDisplayChar();
           }*/
        char[] chars = field.getDisplayChars();

        Color fgcolor = translateColor(field.getForegroundColor());
        Color bgcolor = translateColor(field.getBackgroundColor());
        boolean underscore = false;
        boolean hidden = false;

        if (blink) {
            fgcolor = bgcolor;
        } else {
            switch (field.getHighlighting()) {
            case RW3270Char.HL_REVERSE:
                fgcolor = bgcolor;
                bgcolor = translateColor(field.getForegroundColor());

                break;

            case RW3270Char.HL_UNDERSCORE:
                underscore = true;
            }
        }

        /*if(f.isHidden())
           hidden = true;*/
        if (field.isBold()) {
            if (fgcolor == currentFGColor) {
                fgcolor = boldColor;
            }
        } else {
            frame.setFont(font);
        }

        if (!field.isProtected()) {
            bgcolor = currentFieldBGColor;
        }

        byte[] byteDisplayChars = new byte[chars.length];

        for (int i = 0; i < chars.length; i++) {
            byteDisplayChars[i] = (byte) chars[i];
        }

        // we have to overwrite the FieldAttribute ourselves
        //g.setColor(currentBGColor);
        paintData(byteDisplayChars, byteDisplayChars.length, field.getBegin(),
            bgcolor, fgcolor, underscore, hidden);

        //         if (!field.isProtected()) {
        //             // The field is not protected - editable, we wish to
        //             // highlight the text input part
        //             frame.setColor(currentFieldBGColor);
    }

    /**
     * This translates the int colors stored in a Field Attribute into a java
     * Color object
     *
     * @param c DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private Color translateColor(int c) {
        switch (c) {
        case RW3270Field.DEFAULT_BGCOLOR:
            return currentBGColor;

        case RW3270Field.BLUE:
            return Color.BLUE;

        case RW3270Field.RED:
            return Color.RED;

        case RW3270Field.PINK:
            return Color.PINK;

        case RW3270Field.GREEN:
            return Color.GREEN;

        case RW3270Field.TURQUOISE:
            return Color.CYAN;

        case RW3270Field.YELLOW:
            return Color.YELLOW;

        case RW3270Field.DEFAULT_FGCOLOR:
            return currentFGColor;

        case RW3270Field.BLACK:
            return Color.BLACK;

        case RW3270Field.DEEP_BLUE:
            return Color.BLUE;

        case RW3270Field.ORANGE:
            return Color.ORANGE;

        case RW3270Field.PURPLE:
            return Color.BLUE;

        case RW3270Field.PALE_GREEN:
            return Color.GREEN;

        case RW3270Field.PALE_TURQUOISE:
            return Color.CYAN;

        case RW3270Field.GREY:
            return new Color(180, 180, 180);

        case RW3270Field.WHITE:
            return Color.WHITE;
        }

        return Color.BLACK;
    }
}
