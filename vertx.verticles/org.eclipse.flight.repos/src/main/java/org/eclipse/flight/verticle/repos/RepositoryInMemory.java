package org.eclipse.flight.verticle.repos;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;

public class RepositoryInMemory {
	Map<String, Project> projects = new HashMap<String, Project>();

	public Project getProject(String id) {
		return projects.get(id);
	}

	public String[] getProjectIds() {
		return projects.keySet().toArray(new String[]{});
	}

	public JsonArray getProjectsAsJson() {
		JsonArray jsonArray = new JsonArray();
		for (Project project : projects.values()) {
			jsonArray.addObject(project.toJson());
		}
		return jsonArray;
	}

	public Project createProject(String id) {
		Project project = projects.get(id);
		if (project == null) {
			project = new Project(id);
			projects.put(id, project);
		}
		return project;
	}

	public boolean hasResource(ResourceIdentifier id) {
		Project project = getProject(id.getProjectId());
		if (project != null) {
			return project.hasResource(id);
		}
		return false;
	}

	public boolean needsUpdate(ResourceIdentifier id) {
		Project project = getProject(id.getProjectId());
		if (project != null) {
			return project.needsUpdate(id);
		}
		return false;
	}

	public Resource getResource(ResourceIdentifier id) {
		Project project = getProject(id.getProjectId());
		if (project != null) {
			return project.getResource(id);
		}
		return null;
	}

	public ResourceIdentifier putResource(Resource resource) {
		Project project = getProject(resource.getProjectId());
		if (project != null) {
			return project.putResource(resource);
		}
		return null;
	}
}
