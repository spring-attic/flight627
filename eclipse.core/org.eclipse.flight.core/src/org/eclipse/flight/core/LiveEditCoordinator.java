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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class LiveEditCoordinator {
	
	private IMessagingConnector messagingConnector;
	private Collection<ILiveEditConnector> liveEditConnectors;
	
	public LiveEditCoordinator(IMessagingConnector messagingConnector) {
		this.messagingConnector = messagingConnector;
		this.liveEditConnectors = new CopyOnWriteArrayList<>();
		
		IMessageHandler startLiveUnit = new AbstractMessageHandler("startedediting") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				startLiveUnit(message);
			}
		};
		messagingConnector.addMessageHandler(startLiveUnit);
		
		IMessageHandler modelChangedHandler = new AbstractMessageHandler("modelchanged") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				modelChanged(message);
			}
		};
		messagingConnector.addMessageHandler(modelChangedHandler);
	}
	
	protected void startLiveUnit(JSONObject message) {
		try {
			String resourcePath = message.getString("resource");
			if (resourcePath != null) {
				for (ILiveEditConnector connector : liveEditConnectors) {
					connector.liveEditingStarted(resourcePath);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void modelChanged(JSONObject message) {
		try {
			String resourcePath = message.getString("resource");
			int offset = message.getInt("offset");
			int removedCharCount = message.getInt("removedCharCount");
			String addedChars = message.has("addedCharacters") ? message.getString("addedCharacters") : "";

			if (resourcePath != null) {
				for (ILiveEditConnector connector : liveEditConnectors) {
					connector.liveEditingEvent(resourcePath, offset, removedCharCount, addedChars);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addLiveEditConnector(ILiveEditConnector connector) {
		liveEditConnectors.add(connector);
	}
	
	public void removeLiveEditConnector(ILiveEditConnector connector) {
		liveEditConnectors.remove(connector);
	}
	
	public void sendModelChangedMessage(String changeOriginID, String resourcePath, int offset, int removedCharactersCount, String newText) {
		try {
			JSONObject message = new JSONObject();
			message.put("resource", resourcePath);
			message.put("offset", offset);
			message.put("removedCharCount", removedCharactersCount);
			message.put("addedCharacters", newText != null ? newText : "");

			this.messagingConnector.send("modelchanged", message);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(changeOriginID)) {
				connector.liveEditingEvent(resourcePath, offset, removedCharactersCount, newText);
			}
		}
	}

	public void sendLiveEditStartedMessage(String changeOriginID, String resourcePath) {
		try {
			JSONObject message = new JSONObject();
			this.messagingConnector.send("startedediting", message);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(changeOriginID)) {
				connector.liveEditingStarted(resourcePath);
			}
		}
	}

}
