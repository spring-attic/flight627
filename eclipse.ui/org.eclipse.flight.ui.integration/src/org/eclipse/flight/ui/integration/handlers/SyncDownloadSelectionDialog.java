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
package org.eclipse.flight.ui.integration.handlers;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * @author Martin Lippert
 */
public class SyncDownloadSelectionDialog extends ElementListSelectionDialog {

	public SyncDownloadSelectionDialog(final Shell parent, final ILabelProvider renderer) {
		super(parent, renderer);

		this.setMultipleSelection(true);
		this.setAllowDuplicates(false);
		this.setTitle("Import Synced Projects...");
	}
	
	@Override
	public int open() {
//		try {
//			int callbackID = this.hashCode();
//			
//			CallbackIDAwareMessageHandler responseHandler = new CallbackIDAwareMessageHandler("getProjectsResponse", callbackID) {
//				@Override
//				public void handleMessage(String messageType, JSONObject response) {
//					try {
//						List<String> projectsNames = new ArrayList<String>();
//						JSONArray projects = response.getJSONArray("projects");
//						for (int i = 0; i < projects.length(); i++) {
//							JSONObject project = projects.getJSONObject(i);
//							String projectName = project.getString("name");
//
//							projectsNames.add(projectName);
//						}
//						setElements((String[]) projectsNames.toArray(new String[projectsNames.size()]));
//					}
//					catch (Exception e) {
//						e.printStackTrace();
//					}
//					
//					messagingConnector.removeMessageHandler(this);
//				}
//			};
//			
//			this.messagingConnector.addMessageHandler(responseHandler);
//			
//			JSONObject message = new JSONObject();
//			message.put("callback_id", callbackID);
//			this.messagingConnector.send("getProjectsRequest", message);
//		} catch (JSONException e1) {
//			e1.printStackTrace();
//		}		
		
		return super.open();
	}

}
