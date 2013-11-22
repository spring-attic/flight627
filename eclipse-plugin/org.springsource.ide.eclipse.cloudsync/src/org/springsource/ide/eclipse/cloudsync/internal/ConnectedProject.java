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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Martin Lippert
 */
public class ConnectedProject {
	
	private IProject project;
	private Map<String, String> resourceHash;
	private Map<String, Long> resourceTimestamp;
	private Set<String> deletedResources;
	
	public ConnectedProject(IProject project) {
		this.project = project;
		this.resourceHash = new ConcurrentHashMap<String, String>();
		this.resourceTimestamp = new ConcurrentHashMap<String, Long>();
		this.deletedResources = new ConcurrentSkipListSet<String>();
		
		try {
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					String path = resource.getProjectRelativePath().toString();
					ConnectedProject.this.setTimestamp(path, resource.getLocalTimeStamp());
					
					if (resource instanceof IFile) {
						try {
							IFile file = (IFile) resource;
							ConnectedProject.this.setHash(path, DigestUtils.shaHex(file.getContents()));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					else if (resource instanceof IFolder) {
						ConnectedProject.this.setHash(path, "0");
					}
					
					return true;
				}
			}, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
		} catch (Exception e) {
			e.printStackTrace();
		}

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
	
	public void setTimestamp(String resourcePath, long newTimestamp) {
		this.resourceTimestamp.put(resourcePath, newTimestamp);
	}
	
	public long getTimestamp(String resourcePath) {
		return this.resourceTimestamp.get(resourcePath);
	}

	public void setHash(String resourcePath, String hash) {
		this.resourceHash.put(resourcePath, hash);
	}
	
	public String getHash(String resourcePath) {
		return this.resourceHash.get(resourcePath);
	}

	public boolean containsResource(String resourcePath) {
		return this.resourceTimestamp.containsKey(resourcePath);
	}
	
	public boolean isDeleted(String resourcePath) {
		return this.deletedResources.contains(resourcePath);
	}
	
	public void addDeleted(String resourcePath, long timestamp) {
		this.deletedResources.add(resourcePath);
		this.resourceTimestamp.put(resourcePath, timestamp);
		this.resourceHash.remove(resourcePath);
	}
	
	public void removeDeleted(String resourcePath) {
		this.deletedResources.remove(resourcePath);
	}
	
	public String[] getDeleted() {
		return (String[]) this.deletedResources.toArray(new String[this.deletedResources.size()]);
	}
	
}
