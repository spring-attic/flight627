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

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * 
 * @author Miles Parker
 */
public class Project extends ProjectAddress {
	
	Map<String, Resource> resources = new HashMap<String, Resource>();

	public Project(String name) {
		super(name);
	}

	public boolean hasResource(ResourceAddress id) {
		return resources.containsKey(id.getPath());
	}

	public boolean needsUpdate(ResourceAddress id) {
		Resource resource = resources.get(id.getPath());
		if (resource != null) {
			return !resource.getType().equals(id.getType()) || resource.getTimestamp() < id.getTimestamp();
		}
		return false;
	}

	public Resource getResource(ResourceAddress id) {
		return resources.get(id.getPath());
	}

	public ResourceAddress putResource(Resource resource) {
		resources.put(resource.getPath(), resource);
		resource.setProjectId(name);
		return resource.toIdentifier();
	}

	@Override
	protected void toJson(JsonObject json) {
		super.toJson(json);
		JsonArray jsonResources = new JsonArray();
		for (Resource resource : resources.values()) {
			jsonResources.addObject(resource.toIdentifier().toJson());
		}
	}
	
	public String getName() {
		return name;
	}
}
