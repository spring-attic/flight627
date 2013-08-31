package org.springsource.ide.eclipse.cloudsync.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;

import com.clwillingham.socket.io.IOSocket;
import com.clwillingham.socket.io.MessageCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudSyncController {
	
	private CloudSyncService syncService;
	private ConcurrentMap<IProject, ConnectedProject> syncedProjects;

	public CloudSyncController() {
		this.syncService = new CloudSyncService("http://localhost:3000/api/");
		this.syncedProjects = new ConcurrentHashMap<IProject, ConnectedProject>();
		
		CloudSyncResourceListener resourceListener = new CloudSyncResourceListener(this, this.syncService);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);
		
		CloudSyncMetadataListener metadataListener = new CloudSyncMetadataListener(this, this.syncService);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(metadataListener, IResourceChangeEvent.POST_BUILD);
		
		try {
			IOSocket socket = new IOSocket("http://localhost:3000", new MessageCallback() {
				  @Override
				  public void on(String event, JSONObject... data) {
					  System.out.println("message");
					  if ("resourceupdate".equals(event)) {
						  updateResource(data[0]);
					  }
				  }

				  @Override
				  public void onMessage(String message) {
					  System.out.println("message");
				  }

				  @Override
				  public void onMessage(JSONObject message) {
					  System.out.println("message");
				  }

				  @Override
				  public void onConnect() {
					  System.out.println("websocket connected");
				  }

				  @Override
				  public void onDisconnect() {
					  System.out.println("websocket disconnected");
				  }
				});

			socket.connect();			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void updateResource(JSONObject jsonObject) {
		try {
			String projectName = jsonObject.getString("project");
			String updatedResource = jsonObject.getString("resource");
			int newVersion = jsonObject.getInt("newversion");
			String fingerprint = jsonObject.getString("fingerprint");
			
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (project != null && isConnected(project)) {
				ConnectedProject connectedProject = getProject(project);
				this.syncService.receivedResourceUpdate(connectedProject, updatedResource, newVersion, fingerprint);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public ConnectedProject getProject(IProject project) {
		return this.syncedProjects.get(project);
	}

	public boolean isConnected(IProject project) {
		return this.syncedProjects.containsKey(project);
	}

	public void connect(IProject project) {
		try {
			ConnectedProject connected = this.syncService.connect(project);
			this.syncedProjects.putIfAbsent(project, connected);
		}
		catch (Exception e) {
		}
	}

	public void disconnect(IProject project) {
		try {
			this.syncService.disconnect(project);
			this.syncedProjects.remove(project);
		}
		catch (Exception e) {
		}
	}

}
