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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.springsource.ide.eclipse.cloudsync.Activator;
import org.springsource.ide.eclipse.cloudsync.internal.CloudSyncController;

/**
 * @author Martin Lippert
 */
public class SyncDownloadHandler extends AbstractHandler {

	public static final String ID = "org.springsource.ide.eclipse.ui.cloudsync.connect";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CloudSyncController syncController = Activator.getDefault().getController();
		Shell shell = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();

		SyncDownloadSelectionDialog selectionDialog2 = new SyncDownloadSelectionDialog(shell, new LabelProvider(), syncController);
		int result = selectionDialog2.open();
		
		if (result == Dialog.OK) {
			Object[] selectedProjects = selectionDialog2.getResult();
			
			for (Object selectedProject : selectedProjects) {
				if (selectedProject instanceof String) {
					syncController.download((String) selectedProject);
				}
			}
		}
		
		return null;
	}

}
