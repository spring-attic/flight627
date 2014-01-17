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
package org.eclipse.flight.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.flight.objects.services.ContentAssist.Proposal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Note that we could handle all of the object coersion through reflection (e.g.
 * GSON) but my assumption is that we don't want that performance hit.
 * 
 * @author Miles Parker
 * 
 */
public abstract class FlightObject {

	private static Map<String, Class<?>> classForName = new HashMap<String, Class<?>>();

	protected abstract void fromJson(JsonObject json);

	protected abstract void toJson(JsonObject json, boolean thin);

	public JsonObject toJson(boolean thin) {
		JsonObject json = new JsonObject();
		json.putString("class", getClass().getName());
		toJson(json, thin);
		return json;
	}

	public final JsonObject toJson() {
		return toJson(false);
	}

	// TODO Scalability. Should special case the known objects so we can avoid
	// reflective operation once we've nailed down objects.
	public static FlightObject createFromJson(JsonObject json) {
		String className = json.getString("class");
		if (className == null) {
			throw new RuntimeException("No class defined in: "
					+ json);
		}
		FlightObject result = null;
		Class<?> clazz = classForName.get(className);
		if (clazz == null) {
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Class not found: " + className + "\nJson: "
						+ json);
			}
			classForName.put(className, clazz);
		}
		try {
			result = (FlightObject) clazz.newInstance();
		} catch (ClassCastException e) {
			throw new RuntimeException("Class doesn't implement MessageObject: " + clazz);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Couldn't locate class for json object class: "
					+ clazz);
		}
		result.fromJson(json);
		return result;
	}

	public void toJsonArray(JsonObject json, String field,
			Collection<? extends FlightObject> objects) {
		JsonArray jsonArray = new JsonArray();
		for (FlightObject object : objects) {
			jsonArray.add(object.toJson());
		}
		json.putArray(field, jsonArray);
	}

	@Override
	public String toString() {
		return toJson().encodePrettily();
	}
}
