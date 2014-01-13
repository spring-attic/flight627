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

package org.eclipse.flight.messages;

import org.eclipse.flight.objects.FlightObject;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 * 
 */
public abstract class FlightMessage extends FlightObject {

	String action;

	FlightObject object;

	private Long senderId;

	public FlightMessage(Long senderId, String action, FlightObject object) {
		this.senderId = senderId;
		this.action = action;
		this.object = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.flight.resources.MessageObject#fromJson(org.vertx.java.core
	 * .json.JsonObject)
	 */
	@Override
	protected void fromJson(JsonObject json) {
		this.action = json.getString("action");
		this.senderId= json.getLong("senderId");
		JsonObject contents = json.getObject("contents");
		if (contents != null) {
			this.object = FlightObject.createFromJson(contents);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.flight.resources.MessageObject#toJson(org.vertx.java.core
	 * .json.JsonObject)
	 */
	@Override
	protected void toJson(JsonObject json, boolean thin) {
		json.putString("action", action);
		json.putNumber("senderId", senderId);
		if (object != null) {
			json.putObject("contents", object.toJson(thin));
		} else {
			json.putObject("contents", null);
		}
	}

	/**
	 * @return the object
	 */
	public FlightObject getObject() {
		return object;
	}

	/**
	 * @return the action
	 */
	public String getDescription() {
		return action;
	}
}
