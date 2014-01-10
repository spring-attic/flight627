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

package org.eclipse.flight.resources.vertx;

import org.eclipse.flight.resources.FlightObject;
import org.eclipse.flight.resources.JsonWrapper;
import org.eclipse.flight.resources.ResponseMessage;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 * 
 */
public abstract class Responder extends FlightHandler {

	public Responder(String address, String action) {
		super(address, action);
	}

	public abstract Object respond(JsonObject request);

	@Override
	public void doHandle(Message<JsonObject> message, JsonObject contents) {
		Object response = respond(contents);
		if (response instanceof JsonObject) {
			JsonObject json = (JsonObject) response;
			message.reply(new ResponseMessage(id, action, new JsonWrapper(json)).toJson());
		} else if (response instanceof FlightObject) {
			message.reply(new ResponseMessage(id, action, (FlightObject) response)
					.toJson());
		} else {
			message.fail(404, "Couldn't respond to: " + contents);
		}
	}
	
	@Override
	public String toString() {
		return "Response @" + getAddress() + " " + getAction();
	}
}
