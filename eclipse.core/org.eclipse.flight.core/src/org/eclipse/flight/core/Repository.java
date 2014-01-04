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
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
import org.eclipse.flight.core.internal.vertx.EclipseVertx;
import org.eclipse.flight.messages.Messages;
import org.eclipse.flight.resources.Project;
import org.eclipse.flight.resources.ProjectAddress;
import org.eclipse.flight.resources.Request;
import org.eclipse.flight.resources.Response;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Martin Lippert
 */
public class Repository {

	private String username;

	private ConcurrentMap<String, ConnectedProject> syncedProjects;
	private Collection<IRepositoryListener> repositoryListeners;

	public Repository(final String user) {
		this.username = user;

		this.syncedProjects = new ConcurrentHashMap<String, ConnectedProject>();
		this.repositoryListeners = new ConcurrentLinkedDeque<>();

		// IMessageHandler resourceChangedHandler = new
		// AbstractMessageHandler("resourceChanged") {
		// @Override
		// public void handleMessage(String messageType, JsonObject message) {
		// updateResource(message);
		// }
		// };
		// this.messagingConnector.addMessageHandler(resourceChangedHandler);
		//
		// IMessageHandler resourceCreatedHandler = new
		// AbstractMessageHandler("resourceCreated") {
		// @Override
		// public void handleMessage(String messageType, JsonObject message) {
		// createResource(message);
		// }
		// };
		// this.messagingConnector.addMessageHandler(resourceCreatedHandler);
		//
		// IMessageHandler resourceDeletedHandler = new
		// AbstractMessageHandler("resourceDeleted") {
		// @Override
		// public void handleMessage(String messageType, JsonObject message) {
		// deleteResource(message);
		// }
		// };
		// this.messagingConnector.addMessageHandler(resourceDeletedHandler);

		EclipseVertx
				.get()
				.eventBus()
				.registerHandler(Messages.GET_ALL_PROJECTS,
						new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								JsonArray projects = new JsonArray();
								for (ConnectedProject project : syncedProjects.values()) {
									projects.addObject(project.toJson());
								}
								JsonObject response = new JsonObject();
								response.putArray("projects", projects);
								response.putString("user", user);
								message.reply(new Response(Messages.GET_ALL_PROJECTS,
										response));
							}
						});

		// IMessageHandler getProjectRequestHandler = new
		// AbstractMessageHandler("getProjectRequest") {
		// @Override
		// public void handleMessage(String messageType, JsonObject message) {
		// getProject(message);
		// }
		// };
		// this.messagingConnector.addMessageHandler(getProjectRequestHandler);
		//
		// IMessageHandler getProjectResponseHandler = new
		// CallbackIDAwareMessageHandler("getProjectResponse",
		// Repository.GET_PROJECT_CALLBACK) {
		// @Override
		// public void handleMessage(String messageType, JsonObject message) {
		// getProjectResponse(message);
		// }
		// };
		// this.messagingConnector.addMessageHandler(getProjectResponseHandler);
		//
		// IMessageHandler getResourceRequestHandler = new
		// AbstractMessageHandler("getResourceRequest") {
		// @Override
		// public void handleMessage(String messageType, JsonObject message) {
		// try {
		// final String resourcePath = message.getString("resource");
		//
		// if (resourcePath.startsWith("classpath:")) {
		// getClasspathResource(message);
		// }
		// else {
		// getResource(message);
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		// };
		// this.messagingConnector.addMessageHandler(getResourceRequestHandler);
		//
		// IMessageHandler getResourceResponseHandler = new
		// CallbackIDAwareMessageHandler("getResourceResponse",
		// Repository.GET_RESOURCE_CALLBACK) {
		// @Override
		// public void handleMessage(String messageType, JsonObject message) {
		// getResourceResponse(message);
		// }
		// };
		// this.messagingConnector.addMessageHandler(getResourceResponseHandler);
		//
		// IMessageHandler getMetadataRequestHandler = new
		// AbstractMessageHandler("getMetadataRequest") {
		// @Override
		// public void handleMessage(String messageType, JsonObject message) {
		// getMetadata(message);
		// }
		// };
		// this.messagingConnector.addMessageHandler(getMetadataRequestHandler);
	}

	public String getUsername() {
		return this.username;
	}

	// protected void connect() {
	// for (String projectName : syncedProjects.keySet()) {
	// sendProjectConnectedMessage(projectName);
	// syncConnectedProject(projectName);
	// }
	// }

	public ConnectedProject getProject(String name) {
		return syncedProjects.get(name);
	}

	public ConnectedProject getProject(IProject project) {
		return getProject(project.getName());
	}

	public boolean isConnected(IProject project) {
		return syncedProjects.containsKey(project.getName());
	}

	public boolean isConnected(String project) {
		return syncedProjects.containsKey(project);
	}

	public void addProject(final IProject project) {
		final String projectName = project.getName();
		if (!this.syncedProjects.containsKey(projectName)) {
			ConnectedProject flightProject = new ConnectedProject(project);
			syncedProjects.put(projectName, flightProject);
			notifyProjectConnected(project);
		}
	}

	public void removeProject(IProject project) {
		String projectName = project.getName();
		if (this.syncedProjects.containsKey(projectName)) {
			ConnectedProject removed = this.syncedProjects.remove(projectName);
			removed.disconnect();
			notifyProjectDisonnected(project);

			// try {
			// JsonObject message = new JsonObject();
			// message.put("username", this.username);
			// message.put("project", projectName);
			// messagingConnector.send("projectDisconnected", message);
			// } catch (JSONException e) {
			// e.printStackTrace();
			// }
		}
	}

	public void resourceChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		ConnectedProject connectedProject = getProject(project);
		if (connectedProject != null) {
			connectedProject.reactToResourceChange(delta);
		}
	}

	public void metadataChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
		if (markerDeltas != null && markerDeltas.length > 0) {
			ConnectedProject connectedProject = getProject(project);
			connectedProject.sendMetadataUpdate(delta.getResource());
		}
	}

	public void addRepositoryListener(IRepositoryListener listener) {
		this.repositoryListeners.add(listener);
	}

	public void removeRepositoryListener(IRepositoryListener listener) {
		this.repositoryListeners.remove(listener);
	}

	protected void notifyProjectConnected(IProject project) {
		for (IRepositoryListener listener : this.repositoryListeners) {
			listener.projectConnected(project);
		}
	}

	protected void notifyProjectDisonnected(IProject project) {
		for (IRepositoryListener listener : this.repositoryListeners) {
			listener.projectDisconnected(project);
		}
	}

}
