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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * 
 * @author Miles Parker
 */
public class Project extends ProjectAddress {
	
	Map<String, ResourceAddress> resources = new HashMap<String, ResourceAddress>();

	public boolean hasResource(ResourceAddress id) {
		return resources.containsKey(id.getPath());
	}

	public boolean needsUpdate(ResourceAddress id) {
		ResourceAddress resource = resources.get(id.getPath());
		if (resource != null) {
			return !resource.getType().equals(id.getType()) || resource.getTimestamp() < id.getTimestamp();
		}
		return false;
	}

	public ResourceAddress getResource(String path) {
		return resources.get(path);
	}

	public ResourceAddress getResource(ResourceAddress id) {
		return getResource(id.getPath());
	}

	public ResourceAddress putResource(ResourceAddress resource) {
		resources.put(resource.getPath(), resource);
		resource.setProjectName(name);
		return resource;
	}

	@Override
	protected void toJson(JsonObject json) {
		super.toJson(json);
		JsonArray jsonResources = new JsonArray();
		for (ResourceAddress resource : resources.values()) {
			jsonResources.addObject(resource.toJson());
		}
		json.putArray("resources", jsonResources);
	}

	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		JsonArray jsonResources = json.getArray("resources");
		for (Object object : jsonResources) {
			JsonObject jsonResource =(JsonObject) object; 
			String path = jsonResource.getString("path");
			ResourceAddress resource = getResource(path);
			if (resource == null) {
				resource = new ResourceAddress();
				putResource(resource);
			}
			resource.fromJson(jsonResource);
		}
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * Not thread safe!
	 * @return the resources
	 */
	public Collection<ResourceAddress> getResources() {
		return resources.values();
	}
}
