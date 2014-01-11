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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.flight.Ids;
import org.eclipse.flight.messages.RequestMessage;
import org.eclipse.flight.messages.ResponseMessage;
import org.eclipse.flight.objects.FlightObject;
import org.eclipse.flight.objects.Project;
import org.eclipse.flight.objects.Resource;
import org.eclipse.flight.vertx.Requester;
import org.eclipse.flight.vertx.VertxManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Martin Lippert
 * @author Miles Parker
 */
public class ConnectedProject extends Project {

	private IProject project;

	public ConnectedProject(IProject project) {
		setName(project.getName());
		setUserName("defaultuser");
		this.project = project;
		try {
			updateResources();
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		// requestResources();
	}

	/**
	 * 
	 */
	public ConnectedProject(String projectName, CompletionCallback completionCallback) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject(projectName);
		try {
			project.create(null);
			project.open(null);
			// requestResources();
		} catch (CoreException e1) {
			completionCallback.downloadFailed();
		}
	}

	private void requestResources() {
		VertxManager.get().request(Ids.RESOURCE_PROVIDER, Ids.GET_PROJECT, ConnectedProject.this,
				new Requester() {

					@Override
					public void accept(FlightObject message) {
						synchronizeProject((Project) message, null);
					}
				});
	}

	public void updateResources() throws CoreException {
		project.accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) throws CoreException {
				String path = resource.getProjectRelativePath().toString();
				Resource flightResource = getResource(path);
				if (flightResource == null) {
					flightResource = new ConnectedResource();
					flightResource.setPath(path);
					putResource(flightResource);
				}
				flightResource.setTimestamp(resource.getLocalTimeStamp());
				if (resource instanceof IFile) {
					try {
						IFile file = (IFile) resource;
						flightResource.setHash(DigestUtils.shaHex(file.getContents()));
						flightResource.setType("file");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else if (resource instanceof IFolder) {
					flightResource.setHash("0");
					flightResource.setType("folder");
				}
				flightResource.setUserName("defaultuser");
				return true;
			}
		}, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
	}

	public void synchronizeProject(Project remoteProject, final CompletionCallback completionCallback) {
		if (remoteProject.getName().equals(getName()) && remoteProject.getUserName().equals(getUserName())) {

			final AtomicInteger requestedFileCount = new AtomicInteger(0);
			final AtomicInteger downloadedFileCount = new AtomicInteger(0);

			for (Resource remoteResource : remoteProject.getResources()) {

				final Resource localResource = getResource(remoteResource.getPath());
				boolean newResource = localResource == null;
				boolean updatedResource = localResource != null
						&& !remoteResource.getHash().equals(localResource.getHash())
						&& localResource.getTimestamp() < remoteResource.getTimestamp();
				if (newResource) {
					putResource(remoteResource);
				}
				if (remoteResource.getType().equals("file")) {
					if (newResource || updatedResource) {
						VertxManager.get().request(Ids.RESOURCE_PROVIDER, Ids.GET_RESOURCE,
								ConnectedProject.this, new Requester() {
									@Override
									public void accept(FlightObject reply) {
										synchronizeResource((Resource) reply);
										if (completionCallback != null) {
											int downloaded = downloadedFileCount.incrementAndGet();
											if (downloaded == requestedFileCount.get()) {
												completionCallback.downloadComplete(project);
											}
										}
									}
								});
					}
				} else if (remoteResource.getType().equals("folder") && newResource) {
					IFolder folder = project.getFolder(remoteResource.getPath());
					try {
						folder.create(true, true, null);
						folder.setLocalTimeStamp(remoteResource.getTimestamp());
					} catch (CoreException e) {
						throw new RuntimeException(e);
					}
				}
			}

			// if (deleted != null) {
			// for (int i = 0; i < deleted.length(); i++) {
			// JsonObject deletedResource = deleted.getJsonObject(i);
			//
			// String resourcePath = deletedResource.getString("path");
			// long deletedTimestamp = deletedResource.getLong("timestamp");
			//
			// IProject project = connectedProject.getProject();
			// IResource resource = project.findMember(resourcePath);
			//
			// if (resource != null && resource.exists() && (resource
			// instanceof IFile || resource instanceof IFolder)) {
			// long localTimestamp =
			// connectedProject.getTimestamp(resourcePath);
			//
			// if (localTimestamp < deletedTimestamp) {
			// resource.delete(true, null);
			// }
			// }
			// }
			// }
		}

	}

	// public void getClasspathResource(JsonObject request) {
	// try {
	// final int callbackID = request.getInt("callback_id");
	// final String sender = request.getString("requestSenderID");
	// final String projectName = request.getString("project");
	// final String resourcePath = request.getString("resource");
	// final String username = request.getString("username");
	//
	// ConnectedProject connectedProject = this.syncedProjects.get(projectName);
	// if (this.username.equals(username) && connectedProject != null) {
	// String typeName = resourcePath.substring("classpath:/".length());
	// if (typeName.endsWith(".class")) {
	// typeName = typeName.substring(0, typeName.length() - ".class".length());
	// }
	// typeName = typeName.replace('/', '.');
	//
	// IJavaProject javaProject =
	// JavaCore.create(connectedProject.getProject());
	// if (javaProject != null) {
	// IType type = javaProject.findType(typeName);
	// IClassFile classFile = type.getClassFile();
	// if (classFile != null && classFile.getSourceRange() != null) {
	//
	// JsonObject message = new JsonObject();
	// message.put("callback_id", callbackID);
	// message.put("requestSenderID", sender);
	// message.put("username", this.username);
	// message.put("project", projectName);
	// message.put("resource", resourcePath);
	// message.put("readonly", true);
	//
	// String content = classFile.getSource();
	//
	// message.put("content", content);
	// message.put("type", "file");
	//
	// messagingConnector.send("getResourceResponse", message);
	// }
	// }
	// }
	// } catch (JSONException e) {
	// e.printStackTrace();
	// } catch (JavaModelException e) {
	// e.printStackTrace();
	// }
	// }

	// public void updateResource(JsonObject request) {
	// try {
	// final String username = request.getString("username");
	// final String projectName = request.getString("project");
	// final String resourcePath = request.getString("resource");
	// final long updateTimestamp = request.getLong("timestamp");
	// final String updateHash = request.optString("hash");
	//
	// ConnectedProject connectedProject = this.syncedProjects.get(projectName);
	// if (this.username.equals(username) && connectedProject != null) {
	// IProject project = connectedProject.getProject();
	// IResource resource = project.findMember(resourcePath);
	//
	// if (resource != null && resource instanceof IFile) {
	// String localHash = connectedProject.getHash(resourcePath);
	// long localTimestamp = connectedProject.getTimestamp(resourcePath);
	//
	// if (localHash != null && !localHash.equals(updateHash) && localTimestamp
	// < updateTimestamp) {
	// JsonObject message = new JsonObject();
	// message.put("callback_id", GET_RESOURCE_CALLBACK);
	// message.put("username", this.username);
	// message.put("project", projectName);
	// message.put("resource", resourcePath);
	// message.put("timestamp", updateTimestamp);
	// message.put("hash", updateHash);
	//
	// messagingConnector.send("getResourceRequest", message);
	// }
	// }
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	// public void deleteResource(JsonObject request) {
	// try {
	// final String username = request.getString("username");
	// final String projectName = request.getString("project");
	// final String resourcePath = request.getString("resource");
	// final long deletedTimestamp = request.getLong("timestamp");
	//
	// ConnectedProject connectedProject = this.syncedProjects.get(projectName);
	// if (this.username.equals(username) && connectedProject != null) {
	// IProject project = connectedProject.getProject();
	// IResource resource = project.findMember(resourcePath);
	//
	// if (resource != null && resource.exists() && (resource instanceof IFile
	// || resource instanceof IFolder)) {
	// long localTimestamp = connectedProject.getTimestamp(resourcePath);
	//
	// if (localTimestamp < deletedTimestamp) {
	// resource.delete(true, null);
	// }
	// }
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public void synchronizeResource(Resource remoteResource) {
		if (remoteResource.getType().equals("file")) {

			Resource localResource = getResource(remoteResource.getPath());
			ByteArrayInputStream remoteData = new ByteArrayInputStream(remoteResource.getData().getBytes());
			try {
				if (localResource == null) {
					IFile file = project.getFile(remoteResource.getPath());
					putResource(remoteResource);
					file.create(remoteData, true, null);
				} else {
					// TODO..deal w/ degenerate case where old file is actually
					// folder
					IResource member = project.findMember(remoteResource.getPath());
					if (member instanceof IFile) {
						IFile file = (IFile) member;
						file.setContents(remoteData, true, true, null);
						file.setLocalTimeStamp(remoteResource.getTimestamp());
					}
				}
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
			localResource.setTimestamp(remoteResource.getTimestamp());
			localResource.setHash(remoteResource.getHash());
		}
	}

	public void reactToResourceChange(IResourceDelta delta) {
		IResource resource = delta.getResource();

		if (resource != null && resource.isDerived(IResource.CHECK_ANCESTORS)) {
			return;
		}

		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			reactOnResourceAdded(resource);
			break;
		case IResourceDelta.REMOVED:
			reactOnResourceRemoved(resource);
			break;
		case IResourceDelta.CHANGED:
			reactOnResourceChange(resource);
			break;
		}
	}

	protected void reactOnResourceAdded(IResource resource) {
		try {
			String resourcePath = resource.getProjectRelativePath().toString();
			long timestamp = resource.getLocalTimeStamp();
			String hash = "0";
			String type = null;
			getResource(resourcePath);

			// connectedProject.setTimestamp(resourcePath, timestamp);
			//
			// if (resource instanceof IFile) {
			// try {
			// IFile file = (IFile) resource;
			// hash = DigestUtils.shaHex(file.getContents());
			// type = "file";
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// } else if (resource instanceof IFolder) {
			// type = "folder";
			// }
			//
			// connectedProject.setHash(resourcePath, hash);
			//
			// JsonObject message = new JsonObject();
			// message.put("username", this.username);
			// message.put("project", connectedProject.getName());
			// message.put("resource", resourcePath);
			// message.put("timestamp", timestamp);
			// message.put("hash", hash);
			// message.put("type", type);
			//
			// messagingConnector.send("resourceCreated", message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void reactOnResourceRemoved(IResource resource) {
		// if (resource instanceof IProject) {
		// this.removeProject((IProject) resource);
		// } else if (!resource.isDerived()
		// && (resource instanceof IFile || resource instanceof IFolder)) {
		// ConnectedProject connectedProject = this.syncedProjects.get(resource
		// .getProject().getName());
		// String resourcePath = resource.getProjectRelativePath().toString();
		// long deletedTimestamp = System.currentTimeMillis();
		//
		// try {
		// JsonObject message = new JsonObject();
		// message.put("username", this.username);
		// message.put("project", connectedProject.getName());
		// message.put("resource", resourcePath);
		// message.put("timestamp", deletedTimestamp);
		//
		// messagingConnector.send("resourceDeleted", message);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
	}

	protected void reactOnResourceChange(IResource resource) {
		// if (resource != null && resource instanceof IFile) {
		// IFile file = (IFile) resource;
		//
		// ConnectedProject connectedProject =
		// this.syncedProjects.get(file.getProject()
		// .getName());
		// String resourcePath = resource.getProjectRelativePath().toString();
		//
		// try {
		//
		// long changeTimestamp = file.getLocalTimeStamp();
		// if (changeTimestamp > connectedProject.getTimestamp(resourcePath)) {
		// String changeHash = DigestUtils.shaHex(file.getContents());
		// if (!changeHash.equals(connectedProject.getHash(resourcePath))) {
		//
		// connectedProject.setTimestamp(resourcePath, changeTimestamp);
		// connectedProject.setHash(resourcePath, changeHash);
		//
		// JsonObject message = new JsonObject();
		// message.put("username", this.username);
		// message.put("project", connectedProject.getName());
		// message.put("resource", resourcePath);
		// message.put("timestamp", changeTimestamp);
		// message.put("hash", changeHash);
		//
		// messagingConnector.send("resourceChanged", message);
		// }
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
	}

	// public void getMetadata(JsonObject request) {
	// try {
	// final String username = request.getString("username");
	// final int callbackID = request.getInt("callback_id");
	// final String sender = request.getString("requestSenderID");
	// final String projectName = request.getString("project");
	// final String resourcePath = request.getString("resource");
	//
	// ConnectedProject connectedProject = this.syncedProjects.get(projectName);
	// if (this.username.equals(username) && connectedProject != null) {
	// IProject project = connectedProject.getProject();
	// IResource resource = project.findMember(resourcePath);
	//
	// JsonObject message = new JsonObject();
	// message.put("callback_id", callbackID);
	// message.put("requestSenderID", sender);
	// message.put("username", this.username);
	// message.put("project", projectName);
	// message.put("resource", resourcePath);
	// message.put("type", "marker");
	//
	// IMarker[] markers = resource.findMarkers(null, true,
	// IResource.DEPTH_INFINITE);
	// String markerJSON = toJSON(markers);
	// JSONArray content = new JSONArray(markerJSON);
	// message.put("metadata", content);
	//
	// messagingConnector.send("getMetadataResponse", message);
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public void sendMetadataUpdate(IResource resource) {
		// try {
		// String project = resource.getProject().getName();
		// String resourcePath = resource.getProjectRelativePath().toString();
		//
		// JsonObject message = new JsonObject();
		// message.put("username", this.username);
		// message.put("project", project);
		// message.put("resource", resourcePath);
		// message.put("type", "marker");
		//
		// IMarker[] markers = resource
		// .findMarkers(null, true, IResource.DEPTH_INFINITE);
		// String markerJSON = toJSON(markers);
		// JSONArray content = new JSONArray(markerJSON);
		// message.put("metadata", content);
		//
		// messagingConnector.send("metadataChanged", message);
		// } catch (Exception e) {
		//
		// }
	}

	// public String toJSON(IMarker[] markers) {
	// StringBuilder result = new StringBuilder();
	// boolean flag = false;
	// result.append("[");
	// for (IMarker m : markers) {
	// if (flag) {
	// result.append(",");
	// }
	//
	// result.append("{");
	// result.append("\"description\":"
	// + JsonObject.quote(m.getAttribute("message", "")));
	// result.append(",\"line\":" + m.getAttribute("lineNumber", 0));
	// result.append(",\"severity\":\""
	// + (m.getAttribute("severity", IMarker.SEVERITY_WARNING) ==
	// IMarker.SEVERITY_ERROR ? "error"
	// : "warning") + "\"");
	// result.append(",\"start\":" + m.getAttribute("charStart", 0));
	// result.append(",\"end\":" + m.getAttribute("charEnd", 0));
	// result.append("}");
	//
	// flag = true;
	// }
	// result.append("]");
	// return result.toString();
	// }

	public IProject getProject() {
		return project;
	}

	public String getName() {
		return this.project.getName();
	}

	public static ConnectedProject readFromJSON(InputStream inputStream, IProject project) {
		return new ConnectedProject(project);
	}

	public void disconnect() {
		// TODO Auto-generated method stub

	}
}
