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
package org.eclipse.flight.verticle.repos;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.flight.resources.Project;
import org.eclipse.flight.resources.Resource;
import org.eclipse.flight.resources.ResourceAddress;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * 
 * @author Miles Parker
 */
public class RepositoryInMemory {
	Map<String, Project> projects = new HashMap<String, Project>();

	public Project getProject(String id) {
		return projects.get(id);
	}

	public String[] getProjectIds() {
		return projects.keySet().toArray(new String[] {});
	}

	public JsonObject getProjectsAsJson() {
		JsonArray jsonArray = new JsonArray();
		for (Project project : projects.values()) {
			jsonArray.addObject(project.toJson());
		}
		return new JsonObject().putArray("projects", jsonArray);
	}

	public Project createProject(String id) {
		Project project = projects.get(id);
		if (project == null) {
			project = new Project(id);
			projects.put(id, project);
		}
		return project;
	}

	public boolean hasResource(ResourceAddress id) {
		Project project = getProject(id.getProjectName());
		if (project != null) {
			return project.hasResource(id);
		}
		return false;
	}

	public boolean needsUpdate(ResourceAddress id) {
		Project project = getProject(id.getProjectName());
		if (project != null) {
			return project.needsUpdate(id);
		}
		return false;
	}

	public Resource getResource(ResourceAddress id) {
		Project project = getProject(id.getProjectName());
		if (project != null) {
			return project.getResource(id);
		}
		return null;
	}

	public ResourceAddress putResource(Resource resource) {
		Project project = getProject(resource.getProjectName());
		if (project == null) {
			throw new IllegalArgumentException("No project exists with name: " + resource.getProjectName());
		}
		return project.putResource(resource);
	}
}
