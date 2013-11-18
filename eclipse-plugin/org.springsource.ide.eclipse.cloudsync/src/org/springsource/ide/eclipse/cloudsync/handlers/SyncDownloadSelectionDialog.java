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
package org.springsource.ide.eclipse.cloudsync.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springsource.ide.eclipse.cloudsync.internal.CloudSyncController;
import org.springsource.ide.eclipse.cloudsync.internal.RequestResponseHandler;

/**
 * @author Martin Lippert
 */
public class SyncDownloadSelectionDialog extends ElementListSelectionDialog {

	private CloudSyncController syncController;

	public SyncDownloadSelectionDialog(final Shell parent, final ILabelProvider renderer, final CloudSyncController syncController) {
		super(parent, renderer);
		this.syncController = syncController;

		this.setMultipleSelection(true);
		this.setAllowDuplicates(false);
		this.setTitle("Import Synced Projects...");
	}
	
	@Override
	public int open() {
		try {
			JSONObject message = new JSONObject();

			RequestResponseHandler getProjectsRequest = new RequestResponseHandler("getProjectsRequest", message, "getProjectsResponse") {
				@Override
				public void receive(JSONObject response) {
					try {
						List<String> projectsNames = new ArrayList<String>();
						JSONArray projects = response.getJSONArray("projects");
						for (int i = 0; i < projects.length(); i++) {
							JSONObject projectObject = projects.getJSONObject(i);
							String projectName = projectObject.keys().next().toString();
							projectsNames.add(projectName);
						}
						setElements((String[]) projectsNames.toArray(new String[projectsNames.size()]));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			};

			message.put("callback_id", getProjectsRequest.getCallbackID());
			this.syncController.sendRequest(getProjectsRequest);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}		
		
		return super.open();
	}

}
