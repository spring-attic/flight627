/*******************************************************************************
 *  Copyright (c) 2013 GoPivotal, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.cloudsync.internal;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;

/**
 * @author Martin Lippert
 */
public class ConnectedProject {
	
	private IProject project;
	private Map<String, Integer> resourceVersions;
	private Map<String, String> resourceFingerprints; 
	
	public ConnectedProject(IProject project) {
		this.project = project;
		this.resourceVersions = new ConcurrentHashMap<String, Integer>();
		this.resourceFingerprints = new ConcurrentHashMap<String, String>();
	}
	
	public IProject getProject() {
		return project;
	}
	
	public String getName() {
		return this.project.getName();
	}

	public static ConnectedProject readFromJSON(InputStream inputStream, IProject project) {
		return new ConnectedProject(project);
	}
	
	public void setVersion(String resourcePath, int newVersion) {
		this.resourceVersions.put(resourcePath, newVersion);
	}
	
	public int getVersion(String resourcePath) {
		return this.resourceVersions.get(resourcePath);
	}

	public void setFingerprint(String resourcePath, String fingerprint) {
		this.resourceFingerprints.put(resourcePath, fingerprint);
	}
	
	public String getLastFingerprint(String resourcePath) {
		return this.resourceFingerprints.get(resourcePath);
	}

}
