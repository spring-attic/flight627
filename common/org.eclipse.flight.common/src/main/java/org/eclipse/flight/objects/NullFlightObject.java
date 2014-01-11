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

import org.vertx.java.core.json.JsonObject;

public class NullFlightObject extends FlightObject {

	/* (non-Javadoc)
	 * @see org.eclipse.flight.objects.FlightObject#fromJson(org.vertx.java.core.json.JsonObject)
	 */
	@Override
	protected void fromJson(JsonObject json) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.flight.objects.FlightObject#toJson(org.vertx.java.core.json.JsonObject, boolean)
	 */
	@Override
	protected void toJson(JsonObject json, boolean thin) {
		json.putString("exists", "false");
	}

}
