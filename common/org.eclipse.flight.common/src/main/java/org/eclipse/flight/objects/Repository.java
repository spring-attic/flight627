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

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * 
 * @author Miles Parker
 */
public class Repository extends FlightObject {
	Map<String, Project> projects = createMap();

	public Repository() {
		projects = createMap();
	}

	public Project getProject(String id) {
		return projects.get(id);
	}

	public Project putProject(Project project) {
		projects.put(project.getName(), project);
		return project;
	}

	public Project putProject(String id) {
		Project project = projects.get(id);
		if (project == null) {
			project = createProject();
			project.setName(id);
			projects.put(id, project);
		}
		return project;
	}

	public Resource getResource(Resource remoteResource) {
		Project project = getProject(remoteResource.getProjectName());
		if (project == null) {
			return null;
		}
		return project.getResource(remoteResource);
	}

	public Resource putResource(Resource remoteResource) {
		Project project = getProject(remoteResource.getProjectName());
		if (project == null) {
			return null;
		}
		return project.putResource(remoteResource);
	}

	public boolean needsUpdate(Resource remoteResource) {
		Project project = getProject(remoteResource.getProjectName());
		if (project == null) {
			return false;
		}
		return project.needsUpdate(remoteResource);
	}

	public Project removeProject(String id) {
		return projects.remove(id);
	}

	protected Project createProject() {
		return new Project();
	}

	protected Map<String, Project> createMap() {
		return new HashMap<String, Project>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.flight.resources.MessageObject#fromJson(org.vertx.java.core
	 * .json.JsonObject)
	 */
	@Override
	protected void fromJson(JsonObject json) {
		JsonArray projects = json.getArray("projects");
		for (Object id : projects) {
			putProject((String) id);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.flight.resources.MessageObject#toJson(org.vertx.java.core
	 * .json.JsonObject)
	 */
	@Override
	protected void toJson(JsonObject json, boolean thin) {
		JsonArray jsonArray = new JsonArray();
		for (Project project : projects.values()) {
			jsonArray.add(project.toJson(thin));
		}
		json.putArray("projects", jsonArray);
	}
}
