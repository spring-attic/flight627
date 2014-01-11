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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Project extends FlightObject {

	String name;

	String username;

	Map<String, Resource> resources = new HashMap<String, Resource>();

	@Override
	public void fromJson(JsonObject json) {
		name = json.getString("name");
		username = json.getString("username");
		JsonArray jsonResources = json.getArray("resources");
		for (Object object : jsonResources) {
			JsonObject jsonResource = (JsonObject) object;
			String path = jsonResource.getString("path");
			Resource resource = getResource(path);
			if (resource == null) {
				resource = new Resource();
				putResource(resource);
			}
			resource.fromJson(jsonResource);
		}
	}

	@Override
	protected void toJson(JsonObject json, boolean thin) {
		json.putString("name", name);
		json.putString("username", username);
		if (!thin) {
			JsonArray jsonResources = new JsonArray();
			for (Resource resource : resources.values()) {
				jsonResources.addObject(resource.toJson());
			}
			json.putArray("resources", jsonResources);
		}
	}

	public boolean hasResource(Resource id) {
		return resources.containsKey(id.getPath());
	}

	public boolean needsUpdate(Resource id) {
		Resource resource = resources.get(id.getPath());
		if (resource != null) {
			return !resource.getType().equals(id.getType())
					|| resource.getTimestamp() < id.getTimestamp();
		}
		return false;
	}

	public Resource getResource(String path) {
		return resources.get(path);
	}

	public Resource getResource(Resource id) {
		return getResource(id.getPath());
	}

	public Resource putResource(Resource resource) {
		resources.put(resource.getPath(), resource);
		resource.setProjectName(name);
		resource.setProject(this);
		return resource;
	}

	/**
	 * Not thread safe!
	 * 
	 * @return the resources
	 */
	public Collection<Resource> getResources() {
		return resources.values();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the username
	 */
	public String getUserName() {
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUserName(String username) {
		this.username = username;
	}
}
