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

package org.eclipse.flight.resources;

import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 * 
 */
public class MessageObjectWrapper extends MessageObject {

	String description;

	MessageObject object;

	public MessageObjectWrapper(String description, MessageObject object) {
		this.description = description;
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
		this.description = json.getString("description");
		JsonObject contents = json.getObject("contents");
		if (contents != null) {
			this.object = MessageObject.createFromJson(contents);
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
		json.putString("description", description);
		if (object != null) {
			json.putObject("contents", object.toJson(thin));
		} else {
			json.putObject("contents", null);
		}
	}

	/**
	 * @return the object
	 */
	public MessageObject getObject() {
		return object;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
}
