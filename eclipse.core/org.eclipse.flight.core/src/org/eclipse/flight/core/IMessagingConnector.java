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
public interface IMessagingConnector {
	
	boolean isConnected();
	void send(String messageType, JSONObject message);

	void addConnectionListener(IConnectionListener connectionListener);
	void removeConnectionListener(IConnectionListener connectionListener);
	void addMessageHandler(IMessageHandler messageHandler);
	void removeMessageHandler(IMessageHandler messageHandler);

}
