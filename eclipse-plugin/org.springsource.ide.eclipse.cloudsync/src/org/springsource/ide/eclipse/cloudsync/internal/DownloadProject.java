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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DownloadProject {
	
	public interface CompletionCallback {
		public void downloadComplete(IProject project);
		public void downloadFailed();
	}
	
	private SocketIO socket;
	private String projectName;
	private int callbackID;
	private CompletionCallback completionCallback;
	
	private IProject project;
	
	private AtomicInteger requestedFileCount = new AtomicInteger(0);
	private AtomicInteger downloadedFileCount = new AtomicInteger(0);

	public DownloadProject(SocketIO socket, String projectName, int callbackID) {
		this.socket = socket;
		this.projectName = projectName;
		this.callbackID = callbackID;
	}
	
	public void run(CompletionCallback completionCallback) {
		this.completionCallback = completionCallback;
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject(projectName);

		try {
			project.create(null);
			project.open(null);
		
			JSONObject message = new JSONObject();
			message.put("callback_id", this.callbackID);
			message.put("project", this.projectName);

			socket.emit("getProjectRequest", message);
		} catch (CoreException e1) {
			e1.printStackTrace();
			this.completionCallback.downloadFailed();
		} catch (JSONException e) {
			e.printStackTrace();
			this.completionCallback.downloadFailed();
		}
	}
	
	public void getProjectResponse(JSONObject response) {
		try {
			final String projectName = response.getString("project");
			final JSONArray files = response.getJSONArray("files");

			for (int i = 0; i < files.length(); i++) {
				JSONObject resource = files.getJSONObject(i);

				String resourcePath = resource.getString("path");
				long timestamp = resource.getLong("timestamp");

				String type = resource.optString("type");
				
				if (type.equals("folder")) {
					IFolder folder = project.getFolder(new Path(resourcePath));
					if (!folder.exists()) {
						folder.create(true, true, null);
					}
					folder.setLocalTimeStamp(timestamp);
				}
				else if (type.equals("file")) {
					requestedFileCount.incrementAndGet();
				}
			}
			
			for (int i = 0; i < files.length(); i++) {
				JSONObject resource = files.getJSONObject(i);

				String resourcePath = resource.getString("path");
				String type = resource.optString("type");
				
				if (type.equals("file")) {
					JSONObject message = new JSONObject();
					message.put("callback_id", callbackID);
					message.put("project", projectName);
					message.put("resource", resourcePath);

					socket.emit("getResourceRequest", message);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.completionCallback.downloadFailed();
		}
	}
	
	public void getResourceResponse(JSONObject response) {
		try {
			final String resourcePath = response.getString("resource");
			final long timestamp = response.getLong("timestamp");
			final String content = response.getString("content");
			
			IFile file = project.getFile(resourcePath);
			if (!file.exists()) {
				file.create(new ByteArrayInputStream(content.getBytes()), true, null);
			}
			else {
				file.setContents(new ByteArrayInputStream(content.getBytes()), true, false, null);
			}
			file.setLocalTimeStamp(timestamp);
			
			int downloaded = this.downloadedFileCount.incrementAndGet();
			if (downloaded == this.requestedFileCount.get()) {
				this.completionCallback.downloadComplete(project);
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.completionCallback.downloadFailed();
		}
	}
	
}