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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.vertx.java.core.json.JsonObject;

/**
 * @author Martin Lippert
 */
public class LiveEditCoordinator {
	
	private Collection<ILiveEditConnector> liveEditConnectors;
	
	public LiveEditCoordinator() {
		this.liveEditConnectors = new CopyOnWriteArrayList<>();
		
//		IMessageHandler startLiveUnit = new AbstractMessageHandler("liveResourceStarted") {
//			@Override
//			public void handleMessage(String messageType, JsonObject message) {
//				startLiveUnit(message);
//			}
//		};
//		messagingConnector.addMessageHandler(startLiveUnit);
//		
//		IMessageHandler modelChangedHandler = new AbstractMessageHandler("liveResourceChanged") {
//			@Override
//			public void handleMessage(String messageType, JsonObject message) {
//				modelChanged(message);
//			}
//		};
//		messagingConnector.addMessageHandler(modelChangedHandler);
	}
	
	protected void startLiveUnit(JsonObject message) {
		try {
			String requestSenderID = message.getString("requestSenderID");
			int callbackID = message.getInteger("callback_id");
			String username = message.getString("username");
			String projectName = message.getString("project");
			String resourcePath = message.getString("resource");
			String hash = message.getString("hash");
			long timestamp = message.getLong("timestamp");

			String liveEditID = projectName + "/" + resourcePath;
			for (ILiveEditConnector connector : liveEditConnectors) {
				connector.liveEditingStarted(requestSenderID, callbackID, username, liveEditID, hash, timestamp);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void modelChanged(JsonObject message) {
		try {
			String username = message.getString("username");
			String projectName = message.getString("project");
			String resourcePath = message.getString("resource");

			int offset = message.getInteger("offset");
			int removedCharCount = message.getInteger("removedCharCount");
			String addedChars = message.getValue("addedCharacters") != null ? message.getString("addedCharacters") : "";

			String liveEditID = projectName + "/" + resourcePath;

			for (ILiveEditConnector connector : liveEditConnectors) {
				connector.liveEditingEvent(username, liveEditID, offset, removedCharCount, addedChars);
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
	
	public void sendModelChangedMessage(String changeOriginID, String username, String projectName, String resourcePath, int offset, int removedCharactersCount, String newText) {
		try {
			JsonObject message = new JsonObject();
			message.putString("username", username);
			message.putString("project", projectName);
			message.putString("resource", resourcePath);
			message.putNumber("offset", offset);
			message.putNumber("offset", offset);
			message.putNumber("removedCharCount", removedCharactersCount);
			message.putString("addedCharacters", newText != null ? newText : "");

//			this.messagingConnector.send("liveResourceChanged", message);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(changeOriginID)) {
				connector.liveEditingEvent(username, resourcePath, offset, removedCharactersCount, newText);
			}
		}
	}

	public void sendLiveEditStartedMessage(String changeOriginID, String username, String projectName, String resourcePath, String hash, long timestamp) {
		try {
			JsonObject message = new JsonObject();
			message.putString("username", username);
			message.putString("project", projectName);
			message.putString("resource", resourcePath);
			message.putString("hash", hash);
			message.putNumber("timestamp", timestamp);
			
//			this.messagingConnector.send("liveResourceStarted", message);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(changeOriginID)) {
				connector.liveEditingStarted("local", 0, username, resourcePath, hash, timestamp);
			}
		}
	}
	
	public void sendLiveEditStartedResponse(String responseOriginID, String requestSenderID, int callbackID, String username, String projectName, String resourcePath, String savePointHash, long savePointTimestamp, String content) {
		try {
			JsonObject message = new JsonObject();
			message.putString("username", username);
			message.putString("project", projectName);
			message.putString("resource", resourcePath);
			message.putNumber("savePointTimestamp", savePointTimestamp);
			message.putString("savePointHash", savePointHash);
			message.putString("liveContent", content);
	
//			this.messagingConnector.send("liveResourceStartedResponse", message);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(responseOriginID)) {
				connector.liveEditingStartedResponse(requestSenderID, callbackID, username, projectName, resourcePath, content);
			}
		}
	}

}
