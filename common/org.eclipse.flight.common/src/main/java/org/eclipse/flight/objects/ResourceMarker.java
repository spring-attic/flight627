/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.flight.objects;

import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 */
public class ResourceMarker extends FlightObject {

	private Resource resource;

	private String description;

	private String severity;

	private Integer sourceLine;

	private Integer start;

	private Integer end;

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public Integer getStart() {
		return start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public Integer getEnd() {
		return end;
	}

	public void setEnd(Integer end) {
		this.end = end;
	}

	public Integer getSourceLine() {
		return sourceLine;
	}

	public void setSourceLine(Integer sourceLine) {
		this.sourceLine = sourceLine;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.flight.resources.FlightObject#fromJson(org.vertx.java.core.json.JsonObject)
	 */
	@Override
	protected void fromJson(JsonObject json) {
		description = json.getString("description");
		severity = json.getString("severity");
		start = json.getInteger("start");
		end = json.getInteger("end");
		sourceLine = json.getInteger("sourceLine");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.flight.resources.FlightObject#toJson(org.vertx.java.core.json.JsonObject, boolean)
	 */
	@Override
	protected void toJson(JsonObject json, boolean thin) {
		json.putString("de", description)
				.putString("severity", severity)
				.putNumber("start", start)
				.putNumber("end", end)
				.putNumber("sourceLine", sourceLine);
	}

}
