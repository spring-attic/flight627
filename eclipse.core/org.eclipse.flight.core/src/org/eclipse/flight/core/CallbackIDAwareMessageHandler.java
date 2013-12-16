/*******************************************************************************
 *  Copyright (c) 2013 GoPivotal, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.flight.core;

import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public abstract class CallbackIDAwareMessageHandler extends AbstractMessageHandler implements IMessageHandler {
	
	private int expectedCallbackID;

	public CallbackIDAwareMessageHandler(String messageType, int callbackID) {
		super(messageType);
		this.expectedCallbackID = callbackID;
	}
	
	@Override
	public boolean canHandle(String messageType, JSONObject message) {
		return super.canHandle(messageType, message) && message.has("callback_id") && message.optInt("callback_id") == this.expectedCallbackID;
	}

}
