/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.flight.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.flight.resources.Project;
import org.eclipse.flight.resources.vertx.VertxManager;
import org.eclipse.flight.resources.vertx.VertxRepository;

/**
 * @author Martin Lippert
 * @author Miles Parker
 */
public class Repository extends VertxRepository {

	private String username;

	private Collection<IRepositoryListener> repositoryListeners;

	public Repository(final String user) {
		super(VertxManager.get());
		this.username = user;
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

//		EclipseVertx
//				.get()
//				.eventBus()
//				.registerHandler(Messages.GET_ALL_PROJECTS,
//						new Handler<Message<JsonObject>>() {
//							@Override
//							public void handle(Message<JsonObject> message) {
//								JsonArray projects = new JsonArray();
//								for (ConnectedProject project : syncedProjects.values()) {
//									projects.addObject(project.toJson());
//								}
//								JsonObject response = new JsonObject();
//								response.putArray("projects", projects);
//								response.putString("user", user);
//								message.reply(new Response(Messages.GET_ALL_PROJECTS,
//										response));
//							}
//						});

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

	/* (non-Javadoc)
	 * @see org.eclipse.flight.resources.Repository#createMap()
	 */
	@Override
	protected Map<String, Project> createMap() {
		return new ConcurrentHashMap<String, Project>();
	}

	public Project getProject(IProject project) {
		return getProject(project.getName());
	}

	public boolean isConnected(IProject project) {
		return getProject(project) != null;
	}

	public boolean isConnected(String project) {
		return getProject(project) != null;
	}

	public void addProject(final IProject project) {
		if (!isConnected(project)) {
			ConnectedProject flightProject = new ConnectedProject(project);
			putProject(flightProject);
			notifyProjectConnected(project);
		}
	}

	public void removeProject(IProject project) {
		String projectName = project.getName();
		if (isConnected(projectName)) {
			Project removed = removeProject(projectName);
			((ConnectedProject) removed).disconnect();
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
		if (project == null) {
			return;
		}
		ConnectedProject connectedProject = (ConnectedProject) getProject(project);
		if (connectedProject != null) {
			connectedProject.reactToResourceChange(delta);
		}
	}

	public void metadataChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		if (project == null) {
			return;
		}
		IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
		if (markerDeltas != null && markerDeltas.length > 0) {
			ConnectedProject connectedProject = (ConnectedProject) getProject(project);
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
