package org.eclipse.flight.verticle.repos;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonObject;

public class Project extends JsonProvider {
	
	private String id;
	
	Map<String, Resource> resources = new HashMap<String, Resource>();
	
	public Project(String id) {
		this.id = id;
	}

	public boolean hasResource(ResourceIdentifier id) {
		return resources.containsKey(id.getPath());
	}

	public boolean needsUpdate(ResourceIdentifier id) {
		Resource resource = resources.get(id.getPath());
		if (resource != null) {
			return !resource.getType().equals(id.getType()) || resource.getTimestamp() < id.getTimestamp();
		}
		return false;
	}

	public Resource getResource(ResourceIdentifier id) {
		return resources.get(id.getPath());
	}

	public ResourceIdentifier putResource(Resource resource) {
		resources.put(resource.getPath(), resource);
		resource.setProjectId(id);
		return resource.toIdentifier();
	}

	@Override
	public void fromJson(JsonObject json) {
		id = json.getString("id");
	}

	@Override
	protected void toJson(JsonObject json) {
		json.putString("id", id);
	}
	
	public String getId() {
		return id;
	}
}
