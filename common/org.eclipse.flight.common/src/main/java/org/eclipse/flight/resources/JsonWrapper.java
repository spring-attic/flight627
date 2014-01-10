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

public class JsonWrapper extends FlightObject {

	JsonObject object;
	
	public JsonWrapper(JsonObject object) {
		this.object = object;
	}

	protected void fromJson(JsonObject json) {
		object = json;
	}
	
	protected void toJson(JsonObject json, boolean thin) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.flight.resources.FlightObject#toJson(boolean)
	 */
	@Override
	public JsonObject toJson(boolean thin) {
		return object;
	}
}
