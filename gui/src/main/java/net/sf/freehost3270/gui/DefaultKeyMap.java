package net.sf.freehost3270.gui;

import java.awt.Component;
import java.awt.event.KeyEvent;

/**
 * By default, all KeyEvents are transformed into an KeyEvent array, and passed
 * along to be processed. No remapping is accomplished by the DefaultKeyMap.
 */
public class DefaultKeyMap implements KeyMap {
	/**
	 * Creates a default, non-remappable KeyMap.
	 */
	public DefaultKeyMap() {
	}
	
	public KeyEvent[] translate(KeyEvent ke) {
		KeyEvent events[] = {ke};
		return events;
	}
}