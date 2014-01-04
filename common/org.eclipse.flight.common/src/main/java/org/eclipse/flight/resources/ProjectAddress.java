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

import org.vertx.java.core.json.JsonObject;

public class ProjectAddress extends MessageObject {

	String name;

	String userName;
	
	@Override
	public void fromJson(JsonObject json) {
		name = json.getString("name");
		userName = json.getString("userName");
	}

	@Override
	protected void toJson(JsonObject json) {
		json.putString("name", name);
		json.putString("userName", userName);
	}

	public ProjectAddress getAddress() {
		ProjectAddress identifier = new ProjectAddress();
		identifier.fromJson(super.toJson());
		return identifier;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
}
