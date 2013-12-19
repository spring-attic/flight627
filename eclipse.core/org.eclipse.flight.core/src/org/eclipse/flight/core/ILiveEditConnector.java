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
package org.eclipse.flight.core;

/**
 * @author Martin Lippert
 */
public interface ILiveEditConnector {

	String getConnectorID();
	
	void liveEditingStarted(String resourcePath);
	void liveEditingEvent(String resourcePath, int offset, int removeCount, String newText);

}
