package org.eclipse.flight.verticle.repos;

import org.vertx.java.core.json.JsonObject;

public abstract class JsonProvider {

	protected abstract void fromJson(JsonObject json);
	
	protected abstract void toJson(JsonObject json);
	
	public final JsonObject toJson() {
		JsonObject json = new JsonObject();
		toJson(json);
		return json;
	}
	
	@Override
	public String toString() {
		return toJson().encodePrettily();
	}
}
