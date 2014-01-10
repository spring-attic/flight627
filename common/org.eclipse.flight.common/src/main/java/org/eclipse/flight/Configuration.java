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

package org.eclipse.flight;

/**
 * @author Miles Parker
 *
 */
public class Configuration {
	
	private static int DEFAULT_EVENT_BUS_PORT = 6270;
	
	private static int DEFAULT_EVENT_BUS_BRIDGE_PORT = 6271;
	
	private static int DEFAULT_WEB_EDITOR_PORT = 3000;
	
	public static String DEFAULT_HOST = "localhost";
	
	public static int getEventBusPort() {
		String port = System.getProperty("flight.eventbus.port");
		if (port != null) {
			return Integer.parseInt(port);
		}
		return DEFAULT_EVENT_BUS_PORT;
	}
	
	public static int getEventBusBridgePort() {
		String port = System.getProperty("flight.eventbusbridge.port");
		if (port != null) {
			return Integer.parseInt(port);
		}
		return DEFAULT_EVENT_BUS_BRIDGE_PORT;
	}
	
	public static int getWebEditorPort() {
		String port = System.getProperty("flight.webeditor.port");
		if (port != null) {
			return Integer.parseInt(port);
		}
		return DEFAULT_WEB_EDITOR_PORT;
	}
	
	public static String getHost() {
		String host = System.getProperty("flight.host");
		if (host != null) {
			return host;
		}
		return DEFAULT_HOST;
	}

}
