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
public class NotificationMessage extends FlightMessage {

	/**
	 * @param type
	 * @param object
	 */
	public NotificationMessage(long senderId, String action, FlightObject object) {
		super(senderId, action, object);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.flight.resources.JsonProvider#toJson(org.vertx.java.core.json.JsonObject)
	 */
	@Override
	protected void toJson(JsonObject json, boolean thin) {
		super.toJson(json, thin);
		json.putString("kind", "notification");
	}

}
