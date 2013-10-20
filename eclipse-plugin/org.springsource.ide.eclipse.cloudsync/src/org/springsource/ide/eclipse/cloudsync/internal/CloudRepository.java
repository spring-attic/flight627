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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
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
				message.put("timestamp", resource.getLocalTimeStamp());

				if (resource instanceof IFile) {
					IFile file = (IFile) resource;

					ByteArrayOutputStream array = new ByteArrayOutputStream();
					IOUtils.copy(file.getContents(), array);

					String content = new String(array.toByteArray(), file.getCharset());

					message.put("content", content);
				}

				socket.emit("getResourceResponse", message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateResource(JSONObject request) {
		try {
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");
			final long updateTimestamp = request.getLong("timestamp");
			final String updateHash = request.getString("hash");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (connectedProject != null) {
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);

				if (resource != null && resource instanceof IFile) {
					IFile file = (IFile) resource;

					String localHash = DigestUtils.shaHex(file.getContents());
					long localTimestamp = file.getLocalTimeStamp();

					if (localHash != null && !localHash.equals(updateHash) && localTimestamp < updateTimestamp) {
						JSONObject message = new JSONObject();
						message.put("callback_id", 0);
						message.put("project", projectName);
						message.put("resource", resourcePath);
						message.put("timestamp", updateTimestamp);
						message.put("hash", updateHash);

						socket.emit("getResourceRequest", message);
					}
				}
			}

		} catch (Exception e) {

		}
	}

	public void getResourceResponse(JSONObject response) {
		try {
			final String projectName = response.getString("project");
			final String resourcePath = response.getString("resource");
			final long updateTimestamp = response.getLong("timestamp");
			final String updateHash = response.getString("hash");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (connectedProject != null) {
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);

				if (resource instanceof IFile) {
					IFile file = (IFile) resource;
					String newResourceContent = response.getString("content");

					connectedProject.setTimestamp(resourcePath, updateTimestamp);
					connectedProject.setHash(resourcePath, updateHash);

					file.setContents(new ByteArrayInputStream(newResourceContent.getBytes()), true, true, null);
					file.setLocalTimeStamp(updateTimestamp);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void getMetadata(JSONObject request) {
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
				message.put("type", "marker");

				IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_INFINITE);
				String markerJSON = toJSON(markers);
				JSONArray content = new JSONArray(markerJSON);
				message.put("metadata", content);

				socket.emit("getMetadataResponse", message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void resourceChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		if (project != null) {
			if (isConnected(project)) {
				sendResourceUpdate(delta);
			}
		}
	}

	public void metadataChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
		if (project != null && isConnected(project) && markerDeltas != null && markerDeltas.length > 0) {
			sendMetadataUpdate(delta.getResource());
		}
	}

	public void sendResourceUpdate(IResourceDelta delta) {
		IResource resource = delta.getResource();

		if (resource != null && resource.isDerived(IResource.CHECK_ANCESTORS)) {
			return;
		}
		
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			break;
		case IResourceDelta.REMOVED:
			break;
		case IResourceDelta.CHANGED:
			if (resource != null && resource instanceof IFile) {
				IFile file = (IFile) resource;
				
				ConnectedProject connectedProject = this.syncedProjects.get(file.getProject().getName());
				String resourcePath = resource.getProjectRelativePath().toString();
				
				try {

					long changeTimestamp = file.getLocalTimeStamp();
					if (changeTimestamp > connectedProject.getTimestamp(resourcePath)) {
						String changeHash = DigestUtils.shaHex(file.getContents());
						if (!changeHash.equals(connectedProject.getHash(resourcePath))) {

							connectedProject.setTimestamp(resourcePath, changeTimestamp);
							connectedProject.setHash(resourcePath, changeHash);

							JSONObject message = new JSONObject();
							message.put("project", connectedProject.getName());
							message.put("resource", resourcePath);
							message.put("timestamp", changeTimestamp);
							message.put("hash", changeHash);

							this.socket.emit("resourceChanged", message);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}

	public void sendMetadataUpdate(IResource resource) {
		try {
			String project = resource.getProject().getName();
			String resourcePath = resource.getProjectRelativePath().toString();

			JSONObject message = new JSONObject();
			message.put("project", project);
			message.put("resource", resourcePath);
			message.put("type", "marker");
			
			IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_INFINITE);
			String markerJSON = toJSON(markers);
			JSONArray content = new JSONArray(markerJSON);
			message.put("metadata", content);

			this.socket.emit("metadataChanged", message);
		}
		catch (Exception e) {
			
		}
		
	}

	public String toJSON(IMarker[] markers) {
		StringBuilder result = new StringBuilder();
		boolean flag = false;
		result.append("[");
		for (IMarker m : markers) {
			if (flag) {
				result.append(",");
			}

			result.append("{");
			result.append("\"description\":" + JSONObject.quote(m.getAttribute("message", "")));
			result.append(",\"line\":" + m.getAttribute("lineNumber", 0));
			result.append(",\"severity\":\"" + (m.getAttribute("severity", IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR ? "error" : "warning") + "\"");
			result.append(",\"start\":" + m.getAttribute("charStart", 0));
			result.append(",\"end\":" + m.getAttribute("charEnd", 0));
			result.append("}");

			flag = true;
		}
		result.append("]");
		return result.toString();
	}

}
