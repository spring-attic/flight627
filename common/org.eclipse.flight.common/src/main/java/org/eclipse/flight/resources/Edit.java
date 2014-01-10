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

package org.eclipse.flight.resources;

import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 */
public class Edit extends Resource {
	int offset;

	int removeCount;

	String editType;

	long savePointTimestamp;

	String savePointHash;

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getRemoveCount() {
		return removeCount;
	}

	public void setRemoveCount(int removeCount) {
		this.removeCount = removeCount;
	}

	public String getEditType() {
		return editType;
	}

	public void setEditType(String editType) {
		this.editType = editType;
	}

	public long getSavePointTimestamp() {
		return savePointTimestamp;
	}

	public void setSavePointTimestamp(long savePointTimestamp) {
		this.savePointTimestamp = savePointTimestamp;
	}

	public String getSavePointHash() {
		return savePointHash;
	}

	public void setSavePointHash(String savePointHash) {
		this.savePointHash = savePointHash;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.flight.resources.Resource#toJson(org.vertx.java.core.json.JsonObject, boolean)
	 */
	@Override
	protected void toJson(JsonObject json, boolean thin) {
		super.toJson(json, thin);
		json.putNumber("offset", offset)
				.putNumber("removeCount", removeCount)
				.putString("editType", editType)
				.putNumber("savePointTimestamp", savePointTimestamp)
				.putString("savePointHash", savePointHash);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.flight.resources.Resource#fromJson(org.vertx.java.core.json.JsonObject)
	 */
	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		offset = json.getInteger("offset");
		removeCount = json.getInteger("removeCount");
		editType = json.getString("editType");
		savePointTimestamp = json.getLong("savePointTimestamp");
		savePointHash = json.getString("savePointHash");
	}
}
