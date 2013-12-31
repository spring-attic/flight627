package org.eclipse.flight.verticle.repos;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonObject;

public class ResourceIdentifier extends JsonProvider {

	Map<String, String> metadata = new HashMap<String, String>();

	String projectId;

	String userName;

	String hash;

	long timestamp;

	String type;

	String path;

	public String getProjectId() {
		return projectId;
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
		this.projectId = projectId;
	}

	public void fromJson(JsonObject json) {
		hash = json.getString("hash");
		timestamp = json.getLong("timestamp");
		type = json.getString("type");
		path = json.getString("path");
		userName = json.getString("userName");
		projectId = json.getString("projectId");
	}

	@Override
	protected void toJson(JsonObject json) {
		json.putString("hash", hash).putNumber("timestamp", timestamp)
				.putString("type", type).putString("path", path)
				.putString("userName", userName).putString("projectId", projectId);
	}

	public static ResourceIdentifier createJsonResourceIdentifier(JsonObject json) {
		ResourceIdentifier id = new ResourceIdentifier();
		id.fromJson(json);
		return id;
	}
}
