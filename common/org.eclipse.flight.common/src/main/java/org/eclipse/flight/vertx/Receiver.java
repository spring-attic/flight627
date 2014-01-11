/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.flight.vertx;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 *
 */
public abstract class Receiver extends FlightHandler {
	/**
	 * @param id
	 * @param action
	 */
	public Receiver(String address, String action) {
		super(address, action);
	}

	public abstract void receive(JsonObject contents);

	@Override
	public void doHandle(Message<JsonObject> message, JsonObject contents) {
		receive(contents);
	}

	@Override
	public String toString() {
		return "Receive @" + getAddress() + " " + getAction();
	}
}
