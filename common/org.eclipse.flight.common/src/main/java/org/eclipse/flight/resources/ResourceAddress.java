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

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonObject;

public class ResourceAddress extends MessageObject {

	Map<String, String> metadata = new HashMap<String, String>();

	String projectName;

	String userName;

	String hash;

	long timestamp;

	String type;

	String path;

	public String getProjectName() {
		return projectName;
	}

	public String getUserName() {
		return userName;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setProjectId(String projectId) {
		this.projectName = projectId;
	}

	public void fromJson(JsonObject json) {
		hash = json.getString("hash");
		timestamp = json.getLong("timestamp");
		type = json.getString("type");
		path = json.getString("path");
		userName = json.getString("userName");
		projectName = json.getString("projectName");
	}

	@Override
	protected void toJson(JsonObject json) {
		json.putString("hash", hash).putNumber("timestamp", timestamp)
				.putString("type", type).putString("path", path)
				.putString("userName", userName).putString("projectName", projectName);
	}

	public static ResourceAddress createJsonResourceIdentifier(JsonObject json) {
		ResourceAddress id = new ResourceAddress();
		id.fromJson(json);
		return id;
	}
}
