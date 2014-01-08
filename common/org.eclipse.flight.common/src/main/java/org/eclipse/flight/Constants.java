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
public interface Constants {
	
	public static int PORT = 6270;
	
	public static String HOST = "localhost";

	public static final String RESOURCE_PROVIDER = "flight.resourceProvider";

	public static final String CREATE_PROJECT = "project.create";

	public static final String GET_PROJECT = "project.get";

	public static final String GET_ALL_PROJECTS = "project.getAll";

	public static final String GET_RESOURCE = "resource.get";

	public static final String HAS_RESOURCE = "resource.has";

	public static final String NEEDS_UPDATE_RESOURCE = "resource.needsUpdate";

	public static final String CREATE_RESOURCE = "resource.create";
}
