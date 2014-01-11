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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.flight.objects.services.Edit;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Resource extends FlightObject {

	Project project;

	String projectName;

	String username;

	String hash;

	Long timestamp;

	String type;

	String path;

	String data;

	Collection<ResourceMarker> markers;

	public String getProjectName() {
		return projectName;
	}

	public String getUserName() {
		return username;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFullPath() {
		return getProjectName() + "/" + getPath();
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setUserName(String username) {
		this.username = username;
	}

	public void setProjectName(String projectId) {
		this.projectName = projectId;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
		this.projectName = project.getName();
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	/**
	 * @return the markers
	 */
	public Collection<ResourceMarker> getMarkers() {
		//Lazy...
		if (markers == null) {
			markers = new ArrayList<ResourceMarker>();
		}
		return markers;
	}

	public void fromJson(JsonObject json) {
		hash = json.getString("hash");
		timestamp = json.getLong("timestamp");
		type = json.getString("type");
		path = json.getString("path");
		username = json.getString("username");
		projectName = json.getString("projectName");
		data = json.getString("data");
		//Don't have to worry abotu this until we have non-Eclipse sources of markers
//		JsonArray jsonMarkers = json.getArray("markers");
//		if (jsonMarkers != null) {
//			for (Object object : jsonMarkers) {
//				if (object instanceof JsonObject) {
//					JsonObject marker = new JsonObject();
//				}
//			}
//		}
	}

	@Override
	protected void toJson(JsonObject json, boolean thin) {
		json.putString("hash", hash)
				.putNumber("timestamp", timestamp)
				.putString("type", type)
				.putString("path", path)
				.putString("username", username)
				.putString("projectName", projectName);
		if (!thin) {
			json.putString("data", data);
		}
		if (markers != null) {
			toJsonArray(json, "markers", markers);
		}
	}
	
	public Edit toEdit() {
		Edit edit = new Edit();
		edit.setUserName(getUserName());
		edit.setProjectName(getProjectName());
		edit.setPath(getPath());
		edit.setHash(getHash());
		edit.setTimestamp(getTimestamp());
		return edit;
	}
}
