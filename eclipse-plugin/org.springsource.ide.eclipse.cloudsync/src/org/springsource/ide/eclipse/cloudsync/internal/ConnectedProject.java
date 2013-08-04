package org.springsource.ide.eclipse.cloudsync.internal;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;

public class ConnectedProject {
	
	private IProject project;
	private Map<String, Integer> resourceVersions; 
	
	public ConnectedProject(IProject project) {
		this.project = project;
		this.resourceVersions = new ConcurrentHashMap<String, Integer>();
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

}
