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

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.SSLContext;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class CloudSyncController {

	static {
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
			public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
				return true;
			}
		});
	}

	private CloudSyncService syncService;
	private ConcurrentMap<IProject, ConnectedProject> syncedProjects;
	private ConcurrentMap<String, ICompilationUnit> liveEditUnits;
	private SocketIO socket;

	public CloudSyncController() {
		String host = System.getProperty("flight627-host", "http://localhost:3000");

		this.syncService = new CloudSyncService(host + "/api/");
		this.syncedProjects = new ConcurrentHashMap<IProject, ConnectedProject>();
		this.liveEditUnits = new ConcurrentHashMap<String, ICompilationUnit>();

		CloudSyncResourceListener resourceListener = new CloudSyncResourceListener(this, this.syncService);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);

		CloudSyncMetadataListener metadataListener = new CloudSyncMetadataListener(this, this.syncService);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(metadataListener, IResourceChangeEvent.POST_BUILD);

		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getInstance("Default"));
			socket = new SocketIO(host);
			socket.connect(new IOCallback() {

				@Override
				public void onMessage(JSONObject arg0, IOAcknowledge arg1) {
					System.out.println("websocket message arrived");
				}

				@Override
				public void onMessage(String arg0, IOAcknowledge arg1) {
					System.out.println("websocket message arrived");
				}

				@Override
				public void onError(SocketIOException ex) {
					System.out.println("websocket error");
					ex.printStackTrace();
				}

				@Override
				public void onDisconnect() {
					System.out.println("websocket disconnect");
				}

				@Override
				public void onConnect() {
					System.out.println("websocket connect");
				}

				@Override
				public void on(String event, IOAcknowledge ack, Object... data) {
					System.out.println("websocket message arrived: " + event);

					if (data.length == 1 && data[0] instanceof JSONObject) {
						if ("resourceupdate".equals(event)) {
							updateResource((JSONObject) data[0]);
						} else if ("startedediting".equals(event)) {
							startedEditing((JSONObject) data[0]);
						} else if ("modelchanged".equals(event)) {
							modelChanged((JSONObject) data[0]);
						} else if ("contentassistrequest".equals(event)) {
							contentAssistRequest((JSONObject) data[0]);
						}
					} else {
						System.out.println("unknown data on websocket");
					}
				}
			});
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
					} else if (addedCharCount > 0) {
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

	protected void contentAssistRequest(JSONObject jsonObject) {
		try {
			String resourcePath = jsonObject.getString("resource");
			int callbackID = jsonObject.getInt("callback_id");
			if (liveEditUnits.containsKey(resourcePath)) {

				ContentAssistService assistService = new ContentAssistService(resourcePath, liveEditUnits.get(resourcePath));

				int offset = jsonObject.getInt("offset");
				String proposalsSource = assistService.compute(offset);

				JSONObject message = new JSONObject();
				message.put("resource", resourcePath);
				message.put("callback_id", callbackID);

				JSONArray proposals = new JSONArray(proposalsSource);
				message.put("proposals", proposals);

				socket.emit("contentassistresponse", message);
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
		} catch (Exception e) {
		}
	}

	public void disconnect(IProject project) {
		try {
			this.syncService.disconnect(project);
			this.syncedProjects.remove(project);
		} catch (Exception e) {
		}
	}

}
