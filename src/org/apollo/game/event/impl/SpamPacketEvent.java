package org.apollo.game.event.impl;

import org.apollo.game.event.Event;

/**
 * A message sent after a short period of time containing random data.
 * 
 * @author Major
 */
public class SpamPacketEvent extends Event {

	/**
	 * Data sent by the spam packet.
	 */
	private final byte[] data;

	/**
	 * Creates a new spam packet event.
	 * 
	 * @param data The data sent.
	 */
	public SpamPacketEvent(byte[] data) {
		this.data = data;
	}

	/**
	 * Gets the data sent.
	 * 
	 * @return The data.
	 */
	public byte[] getData() {
		return data;
	}

}