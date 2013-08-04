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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.springsource.ide.eclipse.cloudsync.Activator;
import org.springsource.ide.eclipse.cloudsync.internal.CloudSyncController;

/**
 * @author Martin Lippert
 */
public class SyncConnectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<IProject> selectedProjects = new ArrayList<IProject>();
		
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object[] selectedObjects = structuredSelection.toArray();
			for (int i = 0; i < selectedObjects.length; i++) {
				if (selectedObjects[i] instanceof IAdaptable) {
					IProject project = (IProject) ((IAdaptable)selectedObjects[i]).getAdapter(IProject.class);
					if (project != null) {
						selectedProjects.add(project);
					}
				}
			}
		}
		
		CloudSyncController syncController = Activator.getDefault().getController();

		for (Iterator<IProject> iterator = selectedProjects.iterator(); iterator.hasNext();) {
			IProject project = iterator.next();
			if (!syncController.isConnected(project)) {
				syncController.connect(project);
			}
		}
		
		return null;
	}

}
