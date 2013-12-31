package org.eclipse.flight.verticle.repos;

import org.vertx.java.core.json.JsonObject;

public class Resource extends ResourceIdentifier {

	String data;

	public String getData() {
		return data;
	}
	
	@Override
	public void toJson(JsonObject json) {
		super.toJson(json);
		json.putString("data", data);
	}

	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		data = json.getString("data");
	}

	public static Resource createFromJsonResource(JsonObject json) {
		Resource id = new Resource();
		id.fromJson(json);
		return id;
	}

	public ResourceIdentifier toIdentifier() {
		ResourceIdentifier identifier = new ResourceIdentifier();
		identifier.fromJson(super.toJson());
		return identifier;
	}
	
	public void setData(String data) {
		this.data = data;
	}
}
