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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.flight.core.DownloadProject;
import org.eclipse.flight.core.DownloadProject.CompletionCallback;
import org.eclipse.flight.core.IMessagingConnector;
import org.eclipse.flight.core.Repository;
import org.eclipse.flight.ui.integration.Activator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Martin Lippert
 */
public class SyncDownloadHandler extends AbstractHandler {

	public static final String ID = "org.springsource.ide.eclipse.ui.cloudsync.connect";
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Repository repository = org.eclipse.flight.core.Activator.getDefault().getRepository();
		final IMessagingConnector messagingConnector = org.eclipse.flight.core.Activator.getDefault().getMessagingConnector();
		
		final Shell shell = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();

		SyncDownloadSelectionDialog selectionDialog = new SyncDownloadSelectionDialog(shell, new LabelProvider(), messagingConnector);
		int result = selectionDialog.open();
		
		if (result == Dialog.OK) {
			Object[] selectedProjects = selectionDialog.getResult();
			
			for (Object selectedProject : selectedProjects) {
				if (selectedProject instanceof String) {
					DownloadProject downloadProject = new DownloadProject(messagingConnector, (String) selectedProject, repository.getUsername());
					downloadProject.run(new CompletionCallback() {
						@Override
						public void downloadFailed() {
						}
						@Override
						public void downloadComplete(IProject project) {
							repository.addProject(project);
						}
					});
				}
			}
		}
		
		return null;
	}

}
