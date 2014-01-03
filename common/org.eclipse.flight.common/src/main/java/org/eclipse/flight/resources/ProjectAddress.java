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

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ProjectAddress extends MessageObject {

	String name;

	public ProjectAddress(String name) {
		this.name = name;
	}

	@Override
	public void fromJson(JsonObject json) {
		name = json.getString("name");
	}

	@Override
	protected void toJson(JsonObject json) {
		json.putString("name", name);
	}
}
