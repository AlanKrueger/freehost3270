package net.sf.freehost3270.gui;

import net.sf.freehost3270.client.RW3270Field;
import java.awt.Color;

/**
 * The DefaultColorMap implements basic ColorMap functions, and reasonable
 * defaults for each color setting.
 */
public class DefaultColorMap implements ColorMap {
	public Color translateColor(int c) {
		switch (c) {
						case ColorMap.FIELD_BACKGROUND:
							return new Color(45, 45, 45);
						case ColorMap.CURSOR_FOREGROUND:
							return Color.YELLOW;
						case ColorMap.CURSOR_BACKGROUND:
							return Color.RED;
						case RW3270Field.DEFAULT_BGCOLOR:
							return Color.BLACK;
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
							return Color.CYAN;
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
