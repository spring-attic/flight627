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
package org.eclipse.flight;

/**
 * Common ids for messaging.
 * 
 * @author Miles Parker
 */
public interface Ids {

	public static final String RESOURCE_PROVIDER = "flight.resourceProvider";

	public static final String PROJECT_DISCONNECTED = "notify.project.disconnected";

	public static final String PROJECT_CONNECTED = "notify.project.connected";

	public static final String RESOURCE_CREATED = "notify.resource.created";

	public static final String RESOURCE_DELETED = "notify.resource.deleted";

	public static final String RESOURCE_MODIFIED = "notify.resource.modified";

	public static final String CREATE_PROJECT = "project.create";

	public static final String GET_PROJECT = "project.get";

	public static final String GET_ALL_PROJECTS = "project.getAll";

	public static final String GET_RESOURCE = "resource.get";

	public static final String HAS_RESOURCE = "resource.has";

	public static final String NEEDS_UPDATE_RESOURCE = "resource.needsUpdate";

	public static final String CREATE_RESOURCE = "resource.create";

	
	public static final String EDIT_PARTICIPANT = "flight.editParticipant";

	public static final String LIVE_RESOURCE_REQUEST = "live.resource.request";

	public static final String LIVE_RESOURCE_STARTED = "live.resource.started";

	public static final String LIVE_RESOURCE_RESPONSE = "live.resource.startedResponse";

	public static final String LIVE_RESOURCE_CHANGED = "live.resource.changed";

	public static final String LIVE_METADATA_CHANGED = "live.metadata.changed";
	
	
	public static final String CONTENT_ASSSIST_SERVICE = "flight.proposalService";

	public static final String CONTENT_ASSIST_REQUEST = "proposal.request";
}
