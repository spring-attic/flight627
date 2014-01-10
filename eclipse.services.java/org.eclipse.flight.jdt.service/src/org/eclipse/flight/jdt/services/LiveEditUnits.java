/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
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
import org.eclipse.flight.Ids;
import org.eclipse.flight.core.ILiveEditConnector;
import org.eclipse.flight.core.IRepositoryListener;
import org.eclipse.flight.core.LiveEditCoordinator;
import org.eclipse.flight.core.WorkspaceRepository;
import org.eclipse.flight.resources.Edit;
import org.eclipse.flight.resources.Resource;
import org.eclipse.flight.resources.vertx.Requester;
import org.eclipse.flight.resources.vertx.VertxManager;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Martin Lippert
 */
public class LiveEditUnits {

	private static final String LIVE_EDIT_CONNECTOR_ID = "JDT-Service-Live-Edit-Connector";

	private static int GET_LIVE_RESOURCES_CALLBACK = "LiveEditUnits - getLiveResourcesCallback".hashCode();

	private ConcurrentMap<String, ICompilationUnit> liveEditUnits;

	private WorkspaceRepository repository;

	private LiveEditCoordinator liveEditCoordinator;

	public LiveEditUnits(LiveEditCoordinator liveEditCoordinator, WorkspaceRepository repository) {
		this.liveEditCoordinator = liveEditCoordinator;
		this.repository = repository;

		this.liveEditUnits = new ConcurrentHashMap<String, ICompilationUnit>();

		ILiveEditConnector liveEditConnector = new ILiveEditConnector() {

			@Override
			public String getConnectorID() {
				return LIVE_EDIT_CONNECTOR_ID;
			}

			@Override
			public void liveEditingEvent(Edit edit) {
				modelChanged(edit);
			}

			@Override
			public void liveEditingStarted(Edit edit) {
				startLiveUnit(edit);
			}

			@Override
			public void liveEditingStartedResponse(Edit edit) {
				updateLiveUnit(edit);
			}
		};
		liveEditCoordinator.addLiveEditConnector(liveEditConnector);

		this.repository.addRepositoryListener(new IRepositoryListener() {
			@Override
			public void projectConnected(IProject project) {
				startupConnectedProject(project);
			}

			@Override
			public void projectDisconnected(IProject project) {
			}
		});

		startup();
	}

	protected void startup() {
		Resource resource = new Resource();
		resource.setUserName(repository.getUsername());
		VertxManager.get().request(Ids.EDIT_PARTICIPANT, Ids.LIVE_RESOURCE_REQUEST, resource,
				new Requester() {

					@Override
					public void accept(JsonObject message) {
						//not sure what to do w/ this yet..
					}
				});
	}

	protected void startupConnectedProject(IProject project) {
		Resource resource = new Resource();
		resource.setUserName(repository.getUsername());
		resource.setProjectName(project.getName());
		VertxManager.get().request(Ids.EDIT_PARTICIPANT, Ids.LIVE_RESOURCE_REQUEST, resource,
				new Requester() {

					@Override
					public void accept(JsonObject message) {
						//not sure what to do w/ this yet..
					}
				});
	}

	protected void disconnect() {
	}

	public boolean isLiveEditResource(String username, String resourcePath) {
		return repository.getUsername().equals(username) && liveEditUnits.containsKey(resourcePath);
	}

	public ICompilationUnit getLiveEditUnit(String username, String resourcePath) {
		if (repository.getUsername().equals(username)) {
			return liveEditUnits.get(resourcePath);
		} else {
			return null;
		}
	}

	protected void startLiveUnit(Edit edit) {
		if (repository.getUsername().equals(edit.getUserName()) && edit.getPath().endsWith(".java")) {

			ICompilationUnit liveUnit = liveEditUnits.get(edit.getFullPath());

			if (liveUnit != null) {
				try {
					String liveContent = liveUnit.getBuffer().getContents();
					String liveUnitHash = DigestUtils.shaHex(liveContent);
					if (!liveUnitHash.equals(edit.getHash())) {
						liveEditCoordinator.sendLiveEditStartedResponse(edit);
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			} else {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(edit.getProjectName());
				if (project != null && repository.isConnected(project)) {
					IFile file = project.getFile(edit.getPath());
					if (file != null) {
						try {
							final LiveEditProblemRequestor liveEditProblemRequestor = new LiveEditProblemRequestor(edit);
							liveUnit = ((ICompilationUnit) JavaCore.create(file)).getWorkingCopy(
									new WorkingCopyOwner() {
										@Override
										public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
											return liveEditProblemRequestor;
										}
									}, new NullProgressMonitor());
							liveEditUnits.put(edit.getPath(), liveUnit);
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

	protected void updateLiveUnit(Edit edit) {
		if (repository.getUsername().equals(edit.getUserName()) && edit.getPath().endsWith(".java")
				&& repository.isConnected(edit.getProjectName())) {
			ICompilationUnit liveUnit = liveEditUnits.get(edit.getFullPath());
			if (liveUnit != null) {
				try {
					String liveContent = liveUnit.getBuffer().getContents();
					String liveUnitHash = DigestUtils.shaHex(liveContent);

					String remoteContentHash = DigestUtils.shaHex(edit.getData());
					if (!liveUnitHash.equals(remoteContentHash)) {
						liveUnit.getBuffer().setContents(edit.getData());
						liveUnit.reconcile(ICompilationUnit.NO_AST, true, null, null);
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void modelChanged(Edit edit) {
		if (repository.getUsername().equals(edit.getUserName()) && liveEditUnits.containsKey(edit.getPath())) {
			System.out.println("live edit compilation unit found");
			ICompilationUnit unit = liveEditUnits.get(edit.getPath());
			try {
				IBuffer buffer = unit.getBuffer();
				buffer.replace(edit.getOffset(), edit.getRemoveCount(), edit.getData());

				if (edit.getRemoveCount() > 0 || edit.getData().length() > 0) {
					unit.reconcile(ICompilationUnit.NO_AST, true, null, null);
				}

			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

}
