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

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 * 
 */
public abstract class FlightHandler implements Handler<Message<JsonObject>> {

	protected Long id;
	protected String action;
	private String address;

	public FlightHandler(String address, String action) {
		super();
		this.address = address;
		this.action = action;
	}

	public abstract void doHandle(Message<JsonObject> message, JsonObject contents);

	@Override
	public void handle(Message<JsonObject> message) {
		JsonObject body = message.body();
		Long senderId = body.getLong("senderId");
		if (senderId == id) {
			return;// Don't send back to ourselves
		} else {
			String messageAction = body.getString("action");
			if (action.equals(messageAction)) {
				JsonObject contents = body.getObject("contents");
				doHandle(message, contents);
			}
		}
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}
}
