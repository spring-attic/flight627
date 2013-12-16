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
package org.eclipse.flight.jdt.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.flight.core.AbstractMessageHandler;
import org.eclipse.flight.core.IMessageHandler;
import org.eclipse.flight.core.IMessagingConnector;
import org.eclipse.flight.core.Repository;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class LiveEditUnits {

	private ConcurrentMap<String, ICompilationUnit> liveEditUnits;
	
	private Repository repository;
	private IMessagingConnector messagingConnector;
	
	public LiveEditUnits(IMessagingConnector messagingConnector, Repository repository) {
		this.messagingConnector = messagingConnector;
		this.repository = repository;
		this.liveEditUnits = new ConcurrentHashMap<String, ICompilationUnit>();
		
		IMessageHandler startLiveUnit = new AbstractMessageHandler("startedediting") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				startLiveUnit(message);
			}
		};
		messagingConnector.addMessageHandler(startLiveUnit);
		
		IMessageHandler modelChangedHandler = new AbstractMessageHandler("modelchanged") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				modelChanged(message);
			}
		};
		messagingConnector.addMessageHandler(modelChangedHandler);
	}
	
	public boolean isLiveEditResource(String resourcePath) {
		return liveEditUnits.containsKey(resourcePath);
	}

	public ICompilationUnit getLiveEditUnit(String resourcePath) {
		return liveEditUnits.get(resourcePath);
	}

	protected void startLiveUnit(JSONObject message) {
		try {
			String resourcePath = message.getString("resource");
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
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void modelChanged(JSONObject message) {
		try {
			String resourcePath = message.getString("resource");
			if (liveEditUnits.containsKey(resourcePath)) {
				System.out.println("live edit compilation unit found");
				ICompilationUnit unit = liveEditUnits.get(resourcePath);
				try {
					IBuffer buffer = unit.getBuffer();

					int start = message.getInt("start");
					int addedCharCount = message.getInt("addedCharCount");
					int removedCharCount = message.getInt("removedCharCount");

					String addedChars = message.has("addedCharacters") ? message.getString("addedCharacters") : "";

					if (removedCharCount > 0) {
						buffer.replace(start, removedCharCount, "");
					}
					
					if (addedCharCount > 0) {
						buffer.replace(start, 0, addedChars);
						unit.reconcile(ICompilationUnit.NO_AST, true, null, null);
					}
					
					if (removedCharCount > 0 || addedCharCount > 0) {
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

}
