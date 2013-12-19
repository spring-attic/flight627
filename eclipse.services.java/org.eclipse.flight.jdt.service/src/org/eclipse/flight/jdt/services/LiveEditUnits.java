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
package org.eclipse.flight.jdt.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.flight.core.ILiveEditConnector;
import org.eclipse.flight.core.IMessagingConnector;
import org.eclipse.flight.core.LiveEditCoordinator;
import org.eclipse.flight.core.Repository;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;

/**
 * @author Martin Lippert
 */
public class LiveEditUnits {
	
	private static final String LIVE_EDIT_CONNECTOR_ID = "JDT-Service-Live-Edit-Connector";

	private ConcurrentMap<String, ICompilationUnit> liveEditUnits;
	private Repository repository;
	private IMessagingConnector messagingConnector;
	
	public LiveEditUnits(IMessagingConnector messagingConnector, LiveEditCoordinator liveEditCoordinator, Repository repository) {
		this.messagingConnector = messagingConnector;
		this.repository = repository;

		this.liveEditUnits = new ConcurrentHashMap<String, ICompilationUnit>();
		
		ILiveEditConnector liveEditConnector = new ILiveEditConnector() {
			@Override
			public String getConnectorID() {
				return LIVE_EDIT_CONNECTOR_ID;
			}

			@Override
			public void liveEditingStarted(String resourcePath) {
				startLiveUnit(resourcePath);
			}

			@Override
			public void liveEditingEvent(String resourcePath, int offset, int removeCount, String newText) {
				modelChanged(resourcePath, offset, removeCount, newText);
			}
		};
		liveEditCoordinator.addLiveEditConnector(liveEditConnector);
	}
	
	public boolean isLiveEditResource(String resourcePath) {
		return liveEditUnits.containsKey(resourcePath);
	}

	public ICompilationUnit getLiveEditUnit(String resourcePath) {
		return liveEditUnits.get(resourcePath);
	}

	protected void startLiveUnit(String resourcePath) {
		if (resourcePath.endsWith(".java")) {
			String projectName = resourcePath.substring(0, resourcePath.indexOf('/'));
			String relativeResourcePath = resourcePath.substring(projectName.length());

			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (project != null && repository.isConnected(project)) {
				IFile file = project.getFile(relativeResourcePath);
				if (file != null) {
					try {
						final LiveEditProblemRequestor liveEditProblemRequestor = new LiveEditProblemRequestor(messagingConnector, resourcePath);
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
	}

	protected void modelChanged(String resourcePath, int offset, int removedCharacterCount, String newText) {
		if (liveEditUnits.containsKey(resourcePath)) {
			System.out.println("live edit compilation unit found");
			ICompilationUnit unit = liveEditUnits.get(resourcePath);
			try {
				IBuffer buffer = unit.getBuffer();
				buffer.replace(offset, removedCharacterCount, newText);

				if (removedCharacterCount > 0 || newText.length() > 0) {
					unit.reconcile(ICompilationUnit.NO_AST, true, null, null);
				}

			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

}
