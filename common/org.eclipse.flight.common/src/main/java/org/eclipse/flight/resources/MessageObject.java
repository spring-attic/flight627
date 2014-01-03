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

public abstract class MessageObject {

	protected abstract void fromJson(JsonObject json);
	
	protected abstract void toJson(JsonObject json);
	
	public final JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.putString("class", getClass().getName());
		toJson(json);
		return json;
	}
	
	protected static MessageObject createFromJson(JsonObject json) {
		String clazz = json.getString("class");
		MessageObject result = null;
		try {
			result = (MessageObject) Class.forName(clazz).newInstance();
			result.fromJson(json);
		} catch (ClassCastException e) {
			throw new RuntimeException("Class doesn't implement MessageObject: " + clazz);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Couldn't locate class for json object class: " + clazz);
		}
		return result;
	}
	
	@Override
	public String toString() {
		return toJson().encodePrettily();
	}
}
