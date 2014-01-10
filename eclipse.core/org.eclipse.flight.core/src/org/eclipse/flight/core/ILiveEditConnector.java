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

import org.eclipse.flight.resources.Edit;

/**
 * @author Martin Lippert
 * @author Miles Parker
 */
public interface ILiveEditConnector {

	String getEditType();

	void liveEditingStarted(Edit edit);

	void liveEditingStartedResponse(Edit edit);

	void liveEditingEvent(Edit edit);

}
