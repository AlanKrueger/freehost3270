/*
 * FreeHost3270 a suite of terminal 3270 access utilities.
 * Copyright (C) 1998, 2001  Art Gillespie
 * Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *						Project Contributors.
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
import java.awt.ImageCapabilities;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.image.VolatileImage;
import java.awt.Image;
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

// TODO: Implement windowMessage.
// TODO: Keyboard handling.
// TODO: Mouse handling.
// TODO: Implement the rest of the TerminalEventListener bits.

/**
 * A SWING component that interactively renders terminal screen contents and
 * handles user to terminal interaction.
 * 
 * <p>
 * The terminal is first rendered to a buffered image and then, whan a
 * component repaint is requested this buffered image is painted on the given
 * component's graphics context. The font face may be set via setFont(), with 
 * the font scaling policy set via setFontScalePolicy(). If font scaling is 
 * enabled, the font will scale to the available size of the component, rather
 * than the component scaling to the size of the font.</p>
 * <p>This class concerns itself with rendering the data provided by a 
 * DefaultTerminalModel or appropriate subclass. The model communicates with the
 * component through normal event-dispatching mechanisms.
 *
 * @see #paintComponent(Graphics)
 * @see #setFont(Font)
 * @see #doLayout()
 * @author Bryan.Varner
 */
public class JTerminal extends JPanel implements TerminalEventListener {
	public static final int SCALE_FONT_OFF = 0;
	public static final int SCALE_FONT_SIZE = 1;
	public static final int STRETCH_FONT_VERTICALLY = 2;
	
	private KeyMap keyMap;
	private ColorMap colorMap;
	private DefaultTerminalModel term;
	
	// buffer used to draw the screen in background.
	private VolatileImage frameBuff;
	private Graphics2D gfx;
	private RenderingHints gfxHints;
	private int fontScalePolicy;
	
	private Rectangle2D charSize;
	private FontMetrics fontMetrics;
	
	
	/**
	 * Creates a new JTerminal with a DefaultTerminalModel (type 2),
	 * DefaultKeyMap, and DefaultColorMap.
	 */
	public JTerminal() {
		this(new DefaultTerminalModel());
	}
	
	/**
	 * Creates a new JTerminal with a DefaultKeyMap and DefaultColorMap.
	 * 
	 * @param model The TerminalModel to use for interaction.
	 */
	public JTerminal(DefaultTerminalModel model) {
		this(model, new DefaultKeyMap());
	}
	
	/**
	 * Creates a new JTerminal with a DefaultColorMap.
	 * 
	 * @param model The TerminalModel to use for interaction.
	 * @param km The KeyMap to use when translating keyboard events.
	 */
	public JTerminal(DefaultTerminalModel model, KeyMap km) {
		this(model, km, new DefaultColorMap());
	}
	
	/**
	 * Creates a new JTerminal 
	 * 
	 * @param model The TerminalModel to use for interaction.
	 * @param km The KeyMap to use when translating keyboard events.
	 * @param cm The ColorMap to use for selecting colors while rendering.
	 */
	public JTerminal(DefaultTerminalModel model, KeyMap km, ColorMap cm) {
		super();
		
		this.term = model;
		term.addTerminalEventListener(this);
		this.keyMap = km;
		this.colorMap = cm;
		
		Dimension minSize = new Dimension();
		minSize.setSize(4 * getDisplayCols(),
		                4 * getDisplayRows());
		setMinimumSize(minSize);
		
		// originally, JPanel does not recieve focus
		setFocusable(true);
		
		// to catch VK_TAB et al.
		setFocusTraversalKeysEnabled(false);
		
		fontScalePolicy = SCALE_FONT_OFF;
		gfxHints = new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS, 
		                              RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		setFont(Font.decode("Lucida Console"));
	}
	
	/**
	 * Gets the active TerminalModel for this JTerminal
	 */
	public DefaultTerminalModel getTerminalModel() {
		return term;
	}
	
	/**
	 * Gets the number of virtual columns we actually can paint on the screen.
	 */
	private int getDisplayCols() {
		return term.rw.getCols() + 2;
	}
	
	/**
	 * Gets the number of virtual rows we actually can paint on the screen
	 */
	private int getDisplayRows() {
		return term.rw.getRows() + 2;
	}
	
	/**
	 * Invoked by our parent objects layout manager.
	 * Because JPanel is a container class, this is invoked after
	 * our own size has been correctly set.
	 * 
	 * We flush any existing backbuffer, and create a new backbuffer
	 * using hardware acceleration where possible.
	 */
	public void doLayout() {
		super.doLayout();
		
		Dimension currentSize = getSize();
		if (frameBuff != null) {
			frameBuff.flush();
		}
		
		try {
			// Attempt to use HW Acceleration
			ImageCapabilities imageCaps = new ImageCapabilities(true);
			frameBuff = createVolatileImage(currentSize.width, 
			                                currentSize.height, imageCaps);
		} catch (java.awt.AWTException awte) { // Fallback to SW only.
			frameBuff = createVolatileImage(currentSize.width, 
			                                currentSize.height);
		}
		gfx = frameBuff.createGraphics();
		gfx.setBackground(
				colorMap.translateColor(RW3270Field.DEFAULT_BGCOLOR));
		gfx.setRenderingHints(gfxHints);
		
		setFont(getFont());
		
		renderScreen();
	}
	
	/**
	 * Overrides setFont.
	 * Sets the font to be used for all text rendering for this component.
	 * Depending on the font scale policy that is currently set, the font may 
	 * be scaled to fit the bounds of the component, or stretched to fill it
	 * vertically.
	 * 
	 * @param font The new Font face to use.
	 */
	public void setFont(Font font) {
		if (gfx != null) {
			Dimension panelSize = getSize();
			
			Rectangle2D.Double idealCharSize = new Rectangle2D.Double(0.0, 0.0, 
					  panelSize.getWidth() / getDisplayCols(),
					  panelSize.getHeight() / getDisplayRows());
			
			// Normal font scaling.
			if ((fontScalePolicy & SCALE_FONT_SIZE) > 0) {
				// Make sure we have a font with only an identity transform
				font = font.deriveFont(12.0f).deriveFont(new AffineTransform());
				FontMetrics metrics = gfx.getFontMetrics(font);
				charSize = metrics.getStringBounds("M", gfx);
				
				float ptScale = (float)(idealCharSize.getWidth() / charSize.getWidth());
				ptScale = (float)Math.round(ptScale * font.getSize2D() * 10) / 10.0f;
				font = font.deriveFont(ptScale);
			}
			
			// Stretch the font only in the vertical (y) axis.
			if ((fontScalePolicy & STRETCH_FONT_VERTICALLY) > 0) {
				FontMetrics metrics = gfx.getFontMetrics(font);
				idealCharSize.setFrame(0, 0, idealCharSize.getWidth(), 
						idealCharSize.getHeight() - metrics.getDescent());
				charSize = metrics.getStringBounds("M", gfx);
				
				double scaley = idealCharSize.getHeight() / charSize.getHeight();
				scaley = Math.round(scaley * 100) / 100.0;
				AffineTransform scaleTransform = new AffineTransform();
				scaleTransform.setToScale(1.0, scaley);
				font = font.deriveFont(scaleTransform);
			}
			
			gfx.setFont(font);
			fontMetrics = gfx.getFontMetrics(font);
			charSize = fontMetrics.getStringBounds("M", gfx);
			
			// If fontscaling is off, then we need to set our size.
			if (fontScalePolicy == SCALE_FONT_OFF) {
				Point2D bottomRight = getLocation(getDisplayRows() + 1, 
				                                  getDisplayCols(), null);
				bottomRight.setLocation(
						bottomRight.getX() + charSize.getWidth(),
						bottomRight.getY());
				Dimension size = new Dimension();
				size.setSize(bottomRight.getX(), bottomRight.getY());
				setMinimumSize(size);
				setPreferredSize(size);
				revalidate();
			}
		}
		super.setFont(font);
	}
	
	/**
	 * Used internally to get the row that the given buffer position should
	 * be drawn to.
	 */
	private int getRow(int position) {
		return position / term.rw.getCols() + 
		       ((getDisplayRows() - term.rw.getRows()) / 2);
	}
	
	/**
	 * Used internally to get the column that the given buffer position should
	 * be drawn to.
	 */
	private int getColumn(int position) {
		return position % term.rw.getCols() + 
		       ((getDisplayCols() - term.rw.getCols()) / 2);
	}
	
	/**
	 * Translates an absolute offset position (say, the rw buffer) into a 
	 * row / column, and finally into the Point2D object.
	 * 
	 * @param p An existing Point2D object that will be set to the proper
	 *          location, saving an object allocation on the stack.
	 * @param position The position to translate.
	 * @return the Point2D location where the given character should be painted.
	 */
	private Point2D getLocation(int position, Point2D p) {
		return getLocation(getRow(position), getColumn(position), p);
	}
	
	/**
	 * Gets the 2D point location to paint a character at row, col, using the
	 * pre-allocated Point2D object p.
	 * 
	 * @param row The row to get the location for
	 * @param col The column to get the location for.
	 * @param p If not null, the Point object provided will be set and 
	 *          returned rather than allocating a new object on the stack.
	 * @return A Point2D object set to the location where drawString() may be
	 *         invoked to paint a character at the given row and column.
	 */
	private Point2D getLocation(int row, int col, Point2D p) {
		if (p == null) {
			p = new Point2D.Double();
		}
		p.setLocation(col * charSize.getWidth(), 
		              row * fontMetrics.getAscent() + 
				    (row * fontMetrics.getDescent()));
		return p;
	}
	
	/**
	 * Fills the given character location with the given color.
	 * Paints a background.
	 * @param position The absolute position in the character buffer.
	 * @param p A Point2D object to use when calculating Points so as not to
	 *          allocate another object on the stack.
	 * @param c the Color to use when filling.
	 */
	private void fillLocation(int position, Color c, Point2D p) {
		fillLocation(getRow(position), getColumn(position), 1, c, p);
	}
	
	/**
	 * Fills the remainder of a row of device columns. This does NOT clear the 
	 * display columns we use for padding.
	 * 
	 * @param position The position into the buffer to start filling
	 * @param c The color to use when filling, may be null if you want to
	 *          use the currently set foreground color.
	 * @param p A Point2D object used for location calculations. Passing in an
	 *          already allocated point will eliminate allocation on the stack,
	 *          It may be null.
	 */
	private void fillRowFromLocation(int position, Color c, Point2D p) {
		fillLocation(getRow(position), getColumn(position), 
		             term.rw.getCols() - getColumn(position) + 1, c, p);
	}
	
	/**
	 * Fills the given character location with the given color.
	 * Paints a background.
	 * 
	 * @param row The character location to fill.
	 * @param col The character location to fill.
	 * @param p A Point2D object to use when calculating Points so as not to 
	 *          allocate another object on the stack.
	 * @param c the Color to use when filling.
	 * @param span The number of character cells to span
	 */
	private void fillLocation(int row, int col, int span, Color c, Point2D p) {
		Color oldColor = gfx.getColor();
		gfx.setColor(c);
		p = getLocation(row, col, p);
		// Offset the background by half the font face descent along the y axis.
		double offset = fontMetrics.getDescent() / 2.0;
		// We round the width, as sometimes with fractional metrics
		// the background won't follow to the tail end of the font face.
		gfx.fill(new Rectangle2D.Double(Math.round(p.getX()), p.getY() + offset, 
		         Math.round(charSize.getWidth() * span), fontMetrics.getHeight()));
		gfx.setColor(oldColor);
	}
	
	/**
	 * Draws the given string at the given character buffer position.
	 * 
	 * @param position Absolute position into the character buffer.
	 * @param s The String to paint.
	 * @param c The Color to use when painting.
	 * @param p A Point2D object to reuse. Can be null.
	 */
	private void drawString(int position, String s, Color c, Point2D p) {
		drawString(getRow(position), getColumn(position), s, c, p);
	}
	
	/**
	 * Draws the given string at the given row and column.
	 * 
	 * @param row The row to paint in.
	 * @param col The column to paint in.
	 * @param s The String to paint.
	 * @param c The Color to use when painting.
	 * @param p A Point2D object to reuse. Can be null.
	 */
	private void drawString(int row, int col, String s, Color c, Point2D p) {
		Color oldColor = null;
		if (c != null) {
			oldColor = gfx.getColor();
			gfx.setColor(c);
		}
		
		// GetLocation get's the top left.
		// for characters we need the bottom - or the top of the next line.
		p = getLocation(row + 1, col, p);
		gfx.drawString(s, (float)p.getX(), (float)p.getY());
		
		if (oldColor != null) {
			gfx.setColor(oldColor);
		}
	}
	
	/**
	 * Draws an underline starting at the given buffer offset, length 
	 * characters long, in the given color. If the color is not given, the 
	 * currently set Color on the graphics object will be used.
	 * @param row The starting row.
	 * @param col The starting col
	 * @param length The length of the line, in characters.
	 * @param c The color to use as the foreground. May be NULL, in which case
	 * the current color on the graphics object will be used.
	 */
	private void drawUnderline(int position, int length, Color c) {
		drawUnderline(getRow(position), getColumn(position), length, c);
	}
	
	/**
	 * Draws an underline from the given row and col for the length specified.
	 * @param row The starting row.
	 * @param col The starting col
	 * @param length The length of the line, in characters.
	 * @param c The color to use as the foreground. May be NULL, in which case
	 * the current color on the graphics object will be used.
	 */
	private void drawUnderline(int row, int col, int length, Color c) {
		Color oldColor = null;
		if (c != null) {
			oldColor = gfx.getColor();
			gfx.setColor(c);
		}
		
		double offset = fontMetrics.getDescent() / 2.0;
		// Calculate point a and b, construct the Line, and draw.
		Point2D a = getLocation(row, col, null);
		a.setLocation(Math.round(a.getX()), 
		              a.getY() + fontMetrics.getHeight() + offset);
		Point2D b = getLocation(row, col + length, null);
		b.setLocation(Math.round(b.getX()), 
			         b.getY() + fontMetrics.getHeight() + offset);
		Line2D underline = new Line2D.Double(a, b);
		gfx.draw(underline);
		
		if (oldColor != null) {
			gfx.setColor(oldColor);
		}
	}
	
	/**
	 * Draws a line for the status info.
	 */
	private void drawStatusLine(Color c) {
		Color oldColor = null;
		if (c != null) {
			oldColor = gfx.getColor();
			gfx.setColor(c);
		}
		
		Point2D a = getLocation(term.rw.getRows() + 
					((getDisplayRows() - term.rw.getRows()) / 2), 0, null);
		Point2D b = getLocation(term.rw.getRows() + 
					((getDisplayRows() - term.rw.getRows()) / 2), 
					getDisplayCols(), null);
		a.setLocation(a.getX(), a.getY() + 1.0);
		b.setLocation(b.getX(), b.getY() + 1.0);
		Line2D statusLine = new Line2D.Double(a, b);
		gfx.draw(statusLine);
		
		if (oldColor != null) {
			gfx.setColor(oldColor);
		}
	}
	
	/**
	 * Sets the KeyMap
	 */
	public void setKeyMap(KeyMap km) {
		this.keyMap = km;
	}
	
	/**
	 * Retrieves the current KeyMap
	 */
	public KeyMap getKeyMap() {
		return this.keyMap;
	}
	
	/**
	 * Sets the ColorMap.
	 */
	public void setColorMap(ColorMap cm) {
		this.colorMap = cm;
		renderScreen();
		repaint();
	}
	
	/**
	 * Gets the current ColorMap
	 */
	public ColorMap getColorMap() {
		return colorMap;
	}
	
	/**
	 * Gets the font scaling policy in use.
	 */
	public int getFontScalePolicy() {
		return fontScalePolicy;
	}
	
	/**
	 * Sets the font scaling policy.
	 */
	public void setFontScalePolicy(int policy) {
		this.fontScalePolicy = policy;
		setFont(getFont());
	}
	
	/**
	 * Sets the font text antialiasing policy.
	 */
	public void setTextAntialiasing(boolean b) {
		if (b) {
			gfxHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
			             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		} else {
			gfxHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
			             RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		}
		gfx.setRenderingHints(gfxHints);
	}
	
	/**
	 * Overrides the painting of our component's interior bounds.
	 * We'll simply blit the backbuffer.
	 * @param g Graphics object pre-inset.
	 */
	protected void paintComponent(Graphics g) {
		g.drawImage(frameBuff, 0, 0, this);
	}
	
	/**
	 * Handles the drawing of the screen.
	 */
	protected void renderScreen() {
		Point2D p = getLocation(0, 0, null);
		Dimension size = getSize();
		gfx.clearRect(0, 0, size.width, size.height);
		gfx.setColor(colorMap.translateColor(RW3270Field.DEFAULT_FGCOLOR));
		if (term.rw.getFields().size() < 1) {
			// Buffer empty. Render empty buffer.
			char[] c = term.rw.getDisplay();
			
			for (int i = 0; i < c.length; i++) {
				drawString(i, "" + c[i], null, p);
			}
		} else {
			Enumeration fields = term.rw.getFields().elements();
			
			while (fields.hasMoreElements()) {
				RW3270Field field = (RW3270Field)fields.nextElement();
				
				char[] chars = field.getDisplayChars();
				FieldColors fc = getFieldColors(field, null);
				boolean underscore = 
					(field.getHighlighting() == RW3270Char.HL_UNDERSCORE);
				
				if (! fc.background.equals(gfx.getBackground())) {
					fillLocation(getRow(field.getBegin()), 
					             getColumn(field.getBegin()),
					             chars.length, fc.background, p);
				}
				
				drawString(field.getBegin(), new String(chars), 
				           fc.foreground, p);
				
				if (underscore) {
					drawUnderline(field.getBegin(), chars.length, fc.foreground);
				}
			}
		}
		paintStatus();
		paintCursor(term.rw.getCursorPosition());
	}
	
	/** 
	 * Utililty method to determine the field colors for a given RW3270 field.
	 * This is reused by renderScreen and the event receiver when dealing with 
	 * cursor changes.
	 */
	private FieldColors getFieldColors(RW3270Field field, FieldColors fc) {
		if (fc == null) {
			fc = new FieldColors(Color.BLACK, Color.BLACK);
		}
		
		fc.foreground = colorMap.translateColor(
					field.getForegroundColor());
		fc.background = colorMap.translateColor(
					field.getBackgroundColor());
		
		if (field.getHighlighting() == RW3270Char.HL_REVERSE) {
			fc.reverse();
		}
		
		if (field.isBold() && fc.foreground.equals(gfx.getColor())) {
			fc.foreground = colorMap.translateColor(ColorMap.BOLD);
		}
		
		if (!field.isProtected()) {
			fc.background = colorMap.translateColor(
					ColorMap.FIELD_BACKGROUND);
		}
		return fc;
	}
	
	/**
	 * Clears and paints the status line based on the current state of the 
	 * associated TerminalModel.
	 */
	private void paintStatus() {
		Point2D p = getLocation(0, 0, null);
		// Clear the whole line
		fillLocation(term.rw.getRows() + 
		             ((getDisplayRows() - term.rw.getRows()) / 2), 
				   0, getDisplayCols(), gfx.getBackground(), p);
		
		drawStatusLine(null);
		
		drawString(term.rw.getRows() + 
			      ((getDisplayRows() - term.rw.getRows()) / 2),
				 0, term.getStatus(), null, p);
		
		drawString(term.rw.getRows() + 
		           ((getDisplayRows() - term.rw.getRows()) / 2), 
				 term.rw.getCols() - 6, term.getCurrentPosition(), null, p);
	}
	
	/**
	 * Paints the cursor at the given location, forces a repaint of the 
	 * status line as well.
	 * 
	 * @param position The position the cursor is at.
	 */
	private void paintCursor(int position) {
		term.setCursor(position);
		
		Point2D p = getLocation(0, 0, null);
		fillLocation(position, 
		             colorMap.translateColor(ColorMap.CURSOR_BACKGROUND), p);
		drawString(position, "" + term.rw.getChar(position).getDisplayChar(), 
		           colorMap.translateColor(ColorMap.CURSOR_FOREGROUND), p);
		paintStatus();
	}
	
	
	/**
	 * Handles incomming terminalEvents from the DefaultTerminalModel.
	 * 
	 * @param te The TerminalEvent to handle
	 */
	public void terminalEventReceived(TerminalEvent te) {
		switch (te.getID()) {
			case TerminalEvent.BEEP: {
				Toolkit.getDefaultToolkit().beep();
				break;
			}
			case TerminalEvent.BROADCAST_MESSAGE: {
				break;
			}
			case TerminalEvent.CURSOR_MOVE: {
				// TODO: Verify that this works.
				// Repaint the old position
				int position = te.getOldCursorPosition();
				Point2D p = getLocation(position, null);
				
				RW3270Char c = term.rw.getChar(position);
				RW3270Field field = c.getField();
				
				FieldColors fc = new FieldColors();
				boolean underscore = false;
				if (field != null) {
					fc = getFieldColors(field, fc);
					underscore = (field.getHighlighting() ==
					              RW3270Char.HL_UNDERSCORE);
				}
				fillLocation(position, fc.background, p);
				drawString(position, "" + c.getDisplayChar(), 
				           fc.foreground, p);
				
				if (underscore) {
					drawUnderline(position, 1, fc.foreground);
				}
				
				// Paint the new cursor location
				paintCursor(te.getNewCursorPosition());
				repaint();
				break;
			}
			case TerminalEvent.DATA_RECEIVED: {
				renderScreen();
				repaint();
				break;
			}
			case TerminalEvent.STATUS_CHANGED: {
				paintStatus();
				repaint();
				break;
			}
			default:
				System.out.println(te.toString());
		}
	}
	
	/**
	 * Internal class to make it a bit easier to fiddle with colors.
	 */
	private class FieldColors {
		Color foreground;
		Color background;
		
		public FieldColors() {
			this(colorMap.translateColor(RW3270Field.DEFAULT_FGCOLOR),
				colorMap.translateColor(RW3270Field.DEFAULT_BGCOLOR));
		}
		
		public FieldColors(Color fore, Color back) {
			foreground = fore;
			background = back;
		}
		
		private void reverse() {
			Color swap = foreground;
			foreground = background;
			background = swap;
		}
	}
	
	/*
	 * Private method that was used to test the graphics functions during
	 * development
	private void testGfxWrappers() {
		gfx.setColor(colorMap.translateColor(RW3270Field.DEFAULT_FGCOLOR));
		fillLocation(2, 1, 10, colorMap.translateColor(ColorMap.FIELD_BACKGROUND), null);
		Point2D p = getLocation(0, 0, null);
		for (int r = 0; r < getDisplayRows(); r++) {
			for (int c = 0; c < getDisplayCols(); c++) {
				drawString(r, c, "" + (c % 10), null, p);
			}
		}
		drawUnderline(85, 10, null);
		fillLocation(0, Color.WHITE, p);
		fillLocation(80 * 23 + 79, Color.YELLOW, p);
		drawString(80 * 23 + 79, "R", Color.RED, p);
	}
	*/
}
