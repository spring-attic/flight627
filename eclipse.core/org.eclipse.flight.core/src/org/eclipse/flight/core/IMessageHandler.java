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
package org.eclipse.flight.core;

import org.json.JsonObject;

/**
 * @author Martin Lippert
 */
public interface IMessageHandler {
	
	String getMessageType();
	
	boolean canHandle(String messageType, JsonObject message);
	void handleMessage(String messageType, JsonObject message);

}
