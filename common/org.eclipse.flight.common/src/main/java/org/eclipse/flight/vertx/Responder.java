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

import org.eclipse.flight.messages.ResponseMessage;
import org.eclipse.flight.objects.FlightObject;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 * 
 */
public abstract class Responder extends FlightHandler {

	private boolean thin;

	public Responder(String address, String action, boolean thin) {
		super(address, action);
		this.thin = thin;
	}

	public Responder(String address, String action) {
		super(address, action);
	}

	public abstract FlightObject respond(FlightObject request);

	/**
	 * An opportunity to further advise the response.
	 * 
	 * @param message
	 * @param json
	 */
	public void modifyJsonResponse(FlightObject request, JsonObject json) {
	}

	@Override
	public void doHandle(Message<JsonObject> message, JsonObject contents) {
		FlightObject requestObject = null;
		JsonObject objectContents = message.body().getObject("contents");
		if (objectContents != null) {
			requestObject = FlightObject.createFromJson(objectContents);
		}
		
		logger.debug("Request: " + requestObject);
		FlightObject response = respond(requestObject);
		if (response != null) {
			JsonObject json = new ResponseMessage(id, action, (FlightObject) response)
					.toJson(thin);
			modifyJsonResponse(requestObject, json);
			message.reply(json);
			logger.debug("Response: " + response);
		} else {
			message.fail(404, "Couldn't respond to: " + contents);
			logger.debug("[Failed]");
		}
	}

	@Override
	public String toString() {
		return "Response @" + getAddress() + " " + getAction();
	}
}
