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

import io.socket.SocketIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class CloudRepository {
	
	private SocketIO socket;
	private ConcurrentMap<String, ConnectedProject> syncedProjects;

	public CloudRepository() {
		this.syncedProjects = new ConcurrentHashMap<String, ConnectedProject>();
	}
	
	public void connect(SocketIO socket) {
		this.socket = socket;
	}
	
	public void disconnect() {
		this.socket = null;
	}
	
	public boolean isConnected() {
		return socket != null;
	}
	
	public ConnectedProject getProject(IProject project) {
		return this.syncedProjects.get(project.getName());
	}

	public boolean isConnected(IProject project) {
		return this.syncedProjects.containsKey(project.getName());
	}

	public void addProject(IProject project) {
		if (!this.syncedProjects.containsKey(project.getName())) {
			this.syncedProjects.put(project.getName(), new ConnectedProject(project));
		}
	}

	public void removeProject(IProject project) {
		if (this.syncedProjects.containsKey(project.getName())) {
			this.syncedProjects.remove(project.getName());
		}
	}

	public void getProjects(JSONObject request) {
		try {
			int callbackID = request.getInt("callback_id");
			String sender = request.getString("requestSenderID");
			
			JSONArray projects = new JSONArray();
			for (String projectName : this.syncedProjects.keySet()) {
				JSONObject proj = new JSONObject();
				proj.put(projectName, "/api/" + projectName);
				projects.put(proj);
			}
			
			JSONObject message = new JSONObject();
			message.put("callback_id", callbackID);
			message.put("requestSenderID", sender);
			message.put("projects", projects);

			socket.emit("getProjectsResponse", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void getProject(JSONObject request) {
		try {
			final int callbackID = request.getInt("callback_id");
			final String sender = request.getString("requestSenderID");
			final String projectName = request.getString("project");
			
			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (connectedProject != null) {
				JSONObject content = new JSONObject();
				content.put("name", projectName);
				
				final JSONArray files = new JSONArray();
				
				IProject project = connectedProject.getProject();

				try {
					project.accept(new IResourceVisitor() {
						@Override
						public boolean visit(IResource resource) throws CoreException {
							JSONObject projectResource = new JSONObject();
							String path = resource.getProjectRelativePath().toString();
							try {
								projectResource.put("path", path);
								projectResource.put("uri", "/api/" + projectName + "/" + path);

								if (resource instanceof IFile) {
									projectResource.put("type", "file");
								} else if (resource instanceof IFolder) {
									projectResource.put("type", "folder");
								}
								files.put(projectResource);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							return true;
						}
					}, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				content.put("files", files);

				JSONObject message = new JSONObject();
				message.put("callback_id", callbackID);
				message.put("requestSenderID", sender);
				message.put("project", projectName);
				message.put("content", content);

				socket.emit("getProjectResponse", message);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void getResource(JSONObject request) {
		try {
			final int callbackID = request.getInt("callback_id");
			final String sender = request.getString("requestSenderID");
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");
			
			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (connectedProject != null) {
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);
				
				JSONObject message = new JSONObject();
				message.put("callback_id", callbackID);
				message.put("requestSenderID", sender);
				message.put("project", projectName);
				message.put("resource", resourcePath);

				if (resource instanceof IFile) {
					IFile file = (IFile) resource;

					ByteArrayOutputStream array = new ByteArrayOutputStream();
					pipe(file.getContents(), array);
					
					String content = new String(array.toByteArray(), file.getCharset());
					
					message.put("content", content);
				}

				socket.emit("getResourceResponse", message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void pipe(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}

		input.close();
	}

}
