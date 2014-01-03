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

package org.eclipse.flight.core.internal.vertx;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

/**
 * @author Miles Parker
 * 
 */
public class EclipseVertx {
	private static Vertx INSTANCE;
	
	public static Vertx get() {
		if (INSTANCE == null) {
			INSTANCE = VertxFactory.newVertx();
		}
		return INSTANCE;
	}
}
