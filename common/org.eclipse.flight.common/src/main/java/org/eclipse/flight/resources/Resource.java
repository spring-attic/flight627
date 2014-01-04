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

public class Resource extends ResourceAddress {

	String data;

	public String getData() {
		return data;
	}
	
	@Override
	public void toJson(JsonObject json) {
		super.toJson(json);
		json.putString("data", data);
	}

	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		data = json.getString("data");
	}

	public static Resource createFromJsonResource(JsonObject json) {
		Resource id = new Resource();
		id.fromJson(json);
		return id;
	}

	public ResourceAddress getAddress() {
		ResourceAddress identifier = new ResourceAddress();
		identifier.fromJson(super.toJson());
		return identifier;
	}
	
	public void setData(String data) {
		this.data = data;
	}
}
