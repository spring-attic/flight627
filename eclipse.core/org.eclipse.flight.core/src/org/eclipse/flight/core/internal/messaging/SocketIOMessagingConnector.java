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
package org.eclipse.flight.core.internal.messaging;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import javax.net.ssl.SSLContext;

import org.eclipse.flight.core.IMessagingConnector;
import org.json.JsonObject;

/**
 * @author Martin Lippert
 */
public class SocketIOMessagingConnector extends AbstractMessagingConnector implements IMessagingConnector {

	static {
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
			public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
				return true;
			}
		});
	}

	private SocketIO socket;
	
	public SocketIOMessagingConnector(String username) {
		String host = System.getProperty("flight-host", "http://localhost:3000");
		
		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getInstance("Default"));
			socket = new SocketIO(host);
			socket.connect(new IOCallback() {

				@Override
				public void onMessage(JsonObject arg0, IOAcknowledge arg1) {
				}

				@Override
				public void onMessage(String arg0, IOAcknowledge arg1) {
				}

				@Override
				public void onError(SocketIOException ex) {
					ex.printStackTrace();
				}

				@Override
				public void onConnect() {
					notifyConnected();
				}

				@Override
				public void onDisconnect() {
					notifyDisconnected();
				}

				@Override
				public void on(String event, IOAcknowledge ack, Object... data) {
					if (data.length == 1 && data[0] instanceof JsonObject) {
						handleIncomingMessage(event, (JsonObject)data[0]);
					}
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void send(String messageType, JsonObject message) {
		socket.emit(messageType, message);
	}

	@Override
	public boolean isConnected() {
		return socket.isConnected();
	}
	
}
