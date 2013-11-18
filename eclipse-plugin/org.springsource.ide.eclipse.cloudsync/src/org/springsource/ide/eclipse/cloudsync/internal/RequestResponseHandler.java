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
package org.springsource.ide.eclipse.cloudsync.internal;

import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public abstract class RequestResponseHandler {
	
	private String requestType;
	private String responseType;
	private JSONObject message;
	private int callbackID;

	public RequestResponseHandler(String requestType, JSONObject message, String responseType) {
		this.requestType = requestType;
		this.responseType = responseType;
		this.message = message;
		
		this.callbackID = hashCode();
	}
	
	public int getCallbackID() {
		return this.callbackID;
	}
	
	public JSONObject getMessage() {
		return this.message;
	}
	
	public String getRequestType() {
		return this.requestType;
	}
	
	public String getResponseType() {
		return this.responseType;
	}
	
	public abstract void receive(JSONObject response);

}
