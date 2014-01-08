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

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.flight.core.ILiveEditConnector;
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
	private LiveEditCoordinator liveEditCoordinator;
	
	public LiveEditUnits(LiveEditCoordinator liveEditCoordinator, Repository repository) {
		this.liveEditCoordinator = liveEditCoordinator;
		this.repository = repository;

		this.liveEditUnits = new ConcurrentHashMap<String, ICompilationUnit>();
		
		ILiveEditConnector liveEditConnector = new ILiveEditConnector() {
			@Override
			public String getConnectorID() {
				return LIVE_EDIT_CONNECTOR_ID;
			}

			@Override
			public void liveEditingEvent(String username, String resourcePath, int offset, int removeCount, String newText) {
				modelChanged(username, resourcePath, offset, removeCount, newText);
			}

			@Override
			public void liveEditingStarted(String requestSenderID, int callbackID, String username, String resourcePath, String hash, long timestamp) {
				startLiveUnit(requestSenderID, callbackID, username, resourcePath, hash, timestamp);
			}

			@Override
			public void liveEditingStartedResponse(String requestSenderID, int callbackID, String username, String projectName,
					String resourcePath, String content) {
				// TODO Auto-generated method stub
			}
		};
		liveEditCoordinator.addLiveEditConnector(liveEditConnector);
	}
	
	public boolean isLiveEditResource(String username, String resourcePath) {
		return repository.getUsername().equals(username) && liveEditUnits.containsKey(resourcePath);
	}

	public ICompilationUnit getLiveEditUnit(String username, String resourcePath) {
		if (repository.getUsername().equals(username)) {
			return liveEditUnits.get(resourcePath);
		}
		else {
			return null;
		}
	}

	protected void startLiveUnit(String requestSenderID, int callbackID, String username, String resourcePath, String hash, long timestamp) {
		if (repository.getUsername().equals(username) && resourcePath.endsWith(".java")) {
			
			String projectName = resourcePath.substring(0, resourcePath.indexOf('/'));
			String relativeResourcePath = resourcePath.substring(projectName.length() + 1);
			
			ICompilationUnit liveUnit = liveEditUnits.get(resourcePath);
			String liveUnitHash = "";
			if (liveUnit != null) {
				try {
					String liveContent = liveUnit.getBuffer().getContents();
					liveUnitHash = DigestUtils.shaHex(liveContent);
					if (!liveUnitHash.equals(hash)) {
						liveEditCoordinator.sendLiveEditStartedResponse(LIVE_EDIT_CONNECTOR_ID, requestSenderID, callbackID, username, projectName, relativeResourcePath, hash, timestamp, liveContent);
					}
				}
				catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
			else {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (project != null && repository.isConnected(project)) {
					IFile file = project.getFile(relativeResourcePath);
					if (file != null) {
						try {
							final LiveEditProblemRequestor liveEditProblemRequestor = new LiveEditProblemRequestor(messagingConnector, username, projectName, relativeResourcePath);
							liveUnit = ((ICompilationUnit) JavaCore.create(file)).getWorkingCopy(new WorkingCopyOwner() {
								@Override
								public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
									return liveEditProblemRequestor;
								}
							}, new NullProgressMonitor());
							liveEditUnits.put(resourcePath, liveUnit);
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			if (liveUnit != null) {
				try {
					liveUnit.reconcile(ICompilationUnit.NO_AST, true, null, null);
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void modelChanged(String username, String resourcePath, int offset, int removedCharacterCount, String newText) {
		if (repository.getUsername().equals(username) && liveEditUnits.containsKey(resourcePath)) {
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
