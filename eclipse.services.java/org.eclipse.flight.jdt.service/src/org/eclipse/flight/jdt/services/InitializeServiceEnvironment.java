package org.eclipse.flight.jdt.services;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.flight.core.AbstractMessageHandler;
import org.eclipse.flight.core.CallbackIDAwareMessageHandler;
import org.eclipse.flight.core.DownloadProject;
import org.eclipse.flight.core.DownloadProject.CompletionCallback;
import org.eclipse.flight.core.IMessageHandler;
import org.eclipse.flight.core.IMessagingConnector;
import org.eclipse.flight.core.Repository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InitializeServiceEnvironment {

	private static int GET_PROJECTS_CALLBACK = "InitializeServiceEnvironment - getProjectsCallback".hashCode();
	
	private IMessagingConnector messagingConnector;
	private Repository repository;

	private IMessageHandler getProjectsResponseHandler;
	private IMessageHandler projectConnectedHandler;

	public InitializeServiceEnvironment(IMessagingConnector messagingConnector, Repository repository) {
		this.messagingConnector = messagingConnector;
		this.repository = repository;
	}

	public void start() {
		getProjectsResponseHandler = new CallbackIDAwareMessageHandler("getProjectsResponse", GET_PROJECTS_CALLBACK) {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleGetProjectsResponse(message);
			}
		};
		messagingConnector.addMessageHandler(getProjectsResponseHandler);
		
		projectConnectedHandler = new AbstractMessageHandler("projectConnected") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleProjectConnected(message);
			}
		};
		messagingConnector.addMessageHandler(projectConnectedHandler);
		
		try {
			JSONObject message = new JSONObject();
			message.put("callback_id", GET_PROJECTS_CALLBACK);
			this.messagingConnector.send("getProjectsRequest", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void handleGetProjectsResponse(JSONObject message) {
		try {
			JSONArray projects = message.getJSONArray("projects");
			for (int i = 0; i < projects.length(); i++) {
				JSONObject projectObject = projects.getJSONObject(i);
				String projectName = projectObject.keys().next().toString();
				
				initializeProject(projectName);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void handleProjectConnected(JSONObject message) {
		try {
			String projectName = message.getString("project");
			initializeProject(projectName);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeProject(String projectName) {
		try {
			// already connected project
			if (repository.isConnected(projectName))
				return;
	
			// project exists in workspace, but is not yet connected
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(projectName);
			if (project.exists()) {
				if (!project.isOpen()) {
					project.open(null);
				}
				repository.addProject(project);
				return;
			}
			
			// project doesn't exist in workspace
			DownloadProject downloadProject = new DownloadProject(messagingConnector, projectName);
			downloadProject.run(new CompletionCallback() {
				@Override
				public void downloadFailed() {
				}
				@Override
				public void downloadComplete(IProject project) {
					try {
						project.build(IncrementalProjectBuilder.FULL_BUILD, null);
					} catch (CoreException e) {
						e.printStackTrace();
					}
					repository.addProject(project);
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
