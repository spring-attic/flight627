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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.json.JSONException;
import org.json.JSONObject;

import com.clwillingham.socket.io.IOSocket;
import com.clwillingham.socket.io.MessageCallback;

/**
 * @author Martin Lippert
 */
public class CloudSyncController {
	
	private CloudSyncService syncService;
	private ConcurrentMap<IProject, ConnectedProject> syncedProjects;
	private ConcurrentMap<String, ICompilationUnit> liveEditUnits;
	private IOSocket socket;

	public CloudSyncController() {
		this.syncService = new CloudSyncService("http://localhost:3000/api/");
		this.syncedProjects = new ConcurrentHashMap<IProject, ConnectedProject>();
		this.liveEditUnits = new ConcurrentHashMap<String, ICompilationUnit>();
		
		CloudSyncResourceListener resourceListener = new CloudSyncResourceListener(this, this.syncService);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);
		
		CloudSyncMetadataListener metadataListener = new CloudSyncMetadataListener(this, this.syncService);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(metadataListener, IResourceChangeEvent.POST_BUILD);
		
		try {
			socket = new IOSocket("http://localhost:3000", new MessageCallback() {
				  @Override
				  public void on(String event, JSONObject... data) {
					  System.out.println("websocket message arrived: " + event);
					  if ("resourceupdate".equals(event)) {
						  updateResource(data[0]);
					  }
					  else if ("startedediting".equals(event)) {
						  startedEditing(data[0]);
					  }
					  else if ("modelchanged".equals(event)) {
						  modelChanged(data[0]);
					  }
				  }

				  @Override
				  public void onMessage(String message) {
					  System.out.println("websocket message arrived: " + message);
				  }

				  @Override
				  public void onMessage(JSONObject message) {
					  System.out.println("websocket message arrived with pure message object");
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
	
	protected void modelChanged(JSONObject jsonObject) {
		try {
			String resourcePath = jsonObject.getString("resource");
			if (liveEditUnits.containsKey(resourcePath)) {
				System.out.println("live edit compilation unit found");
				ICompilationUnit unit = liveEditUnits.get(resourcePath);
				try {
					IBuffer buffer = unit.getBuffer();
					
					int start = jsonObject.getInt("start");
					int addedCharCount = jsonObject.getInt("addedCharCount");
					int removedCharCount = jsonObject.getInt("removedCharCount");
					
					String addedChars = jsonObject.has("addedCharacters") ? jsonObject.getString("addedCharacters") : ""; 
					
					if (removedCharCount > 0) {
						buffer.replace(start, removedCharCount, "");
						unit.reconcile(ICompilationUnit.NO_AST, true, null, null);
					}
					else if (addedCharCount > 0) {
						buffer.replace(start, 0, addedChars);
						unit.reconcile(ICompilationUnit.NO_AST, true, null, null);
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
				
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void startedEditing(JSONObject jsonObject) {
		try {
			String resourcePath = jsonObject.getString("resource");
			if (resourcePath.endsWith(".java")) {
				String projectName = resourcePath.substring(0, resourcePath.indexOf('/'));
				String relativeResourcePath = resourcePath.substring(projectName.length());

				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (project != null && isConnected(project)) {
					IFile file = project.getFile(relativeResourcePath);
					if (file != null) {
							try {
								final LiveEditProblemRequestor liveEditProblemRequestor = new LiveEditProblemRequestor(socket, resourcePath);
								ICompilationUnit unit = ((ICompilationUnit) JavaCore.create(file)).getWorkingCopy(new WorkingCopyOwner() {
									@Override
									public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
										return liveEditProblemRequestor;
									}
								}, new NullProgressMonitor());
								liveEditUnits.put(resourcePath, unit);
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
					}
				}
			}
		} catch (JSONException e) {
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
