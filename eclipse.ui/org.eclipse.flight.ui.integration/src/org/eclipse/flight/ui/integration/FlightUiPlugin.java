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
package org.eclipse.flight.ui.integration;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.flight.core.IRepositoryListener;
import org.eclipse.flight.core.LiveEditCoordinator;
import org.eclipse.flight.core.WorkspaceRepository;
import org.eclipse.flight.objects.Repository;
import org.eclipse.flight.ui.integration.handlers.LiveEditConnector;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Martin Lippert
 */
public class FlightUiPlugin extends AbstractUIPlugin implements IStartup {

	private static final String CONNECTED_PROJECTS_ID = "connected.projects";

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.flight.ui.integration"; //$NON-NLS-1$

	// The shared instance
	private static FlightUiPlugin plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		try {
			Class ignore = org.eclipse.flight.jdt.services.Activator.class;
		} catch (NoClassDefFoundError e) {
			StatusManager.getManager().handle(new Status(IStatus.INFO, PLUGIN_ID, "The jdt.services bundle is not installed. Some flight services will not be available."));
		}
		
		org.eclipse.flight.core.Activator.getDefault().getRepository().addRepositoryListener(new IRepositoryListener() {
			@Override
			public void projectDisconnected(IProject project) {
				updateProjectLabel(project);
				removeConnectedProjectPreference(project.getName());
			}

			@Override
			public void projectConnected(IProject project) {
				updateProjectLabel(project);
				addConnectedProjectPreference(project.getName());
			}
		});

		if (Boolean.getBoolean("flight-eclipse-editor-connect")) {
			WorkspaceRepository repository = org.eclipse.flight.core.Activator.getDefault()
					.getRepository();
			LiveEditCoordinator liveEditCoordinator = org.eclipse.flight.core.Activator
					.getDefault().getLiveEditCoordinator();
			new LiveEditConnector(liveEditCoordinator, repository);
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResourceChangeListener listener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getResource() instanceof IProject) {
					IResourceDelta delta = event.getDelta();
					if (delta == null) {
						return;
					}
					if (delta.getKind() == IResourceDelta.REMOVED) {
						IProject project = (IProject) event.getResource();
						removeConnectedProjectPreference(project.getName());
					} else if (delta.getKind() == IResourceDelta.CHANGED) {
						// TODO, we aren't handling project renaming yet
						// IProject project = (IProject) event.getResource();
						// String oldName =
						// delta.getMovedFromPath().lastSegment();
						// removeConnectedProjectPreference(oldName);
						// addConnectedProjectPreference(project.getName());
					}
				}
				System.out.println("Something changed!");
			}
		};
		workspace.addResourceChangeListener(listener);
		
		updateProjectConnections();
	}

	private void updateProjectConnections() throws CoreException {
		String[] projects = getConnectedProjectPreferences();
		for (String projectName : projects) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(projectName);
			if (project.exists()) {
				if (!project.isOpen()) {
					project.open(null);
				}
				WorkspaceRepository repository = org.eclipse.flight.core.Activator.getDefault()
						.getRepository();
				repository.addProject(project);
			}
		}
	}

	private String[] getConnectedProjectPreferences() {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		String[] projects = StringUtils.split(preferences.get(CONNECTED_PROJECTS_ID, ""),
				";");
		return projects;
	}

	private void addConnectedProjectPreference(String projectName) {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		String currentPreferences = preferences.get(CONNECTED_PROJECTS_ID, "");
		String[] projects = StringUtils.split(currentPreferences, ";");
		for (String existingProjectName : projects) {
			if (existingProjectName.equals(projectName)) {
				return;
			}
		}
		currentPreferences += ";" + projectName;
		preferences.put(CONNECTED_PROJECTS_ID, currentPreferences);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			// We really don't care that much..
		}
	}

	private void removeConnectedProjectPreference(String projectName) {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		String currentPreferences = preferences.get(CONNECTED_PROJECTS_ID, "");
		String[] projects = StringUtils.split(currentPreferences, ";");
		Collection<String> retainedProjects = new HashSet<String>();
		for (String existingProjectName : projects) {
			if (!existingProjectName.equals(projectName)) {
				retainedProjects.add(existingProjectName);
			}
		}
		String newPreferences = StringUtils.join(retainedProjects, ";");
		preferences.put(CONNECTED_PROJECTS_ID, newPreferences);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			// We really don't care that much..
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static FlightUiPlugin getDefault() {
		return plugin;
	}

	protected static void updateProjectLabel(final IProject project) {
		final CloudProjectDecorator projectDecorator = CloudProjectDecorator
				.getInstance();
		if (projectDecorator != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					projectDecorator
							.fireLabelProviderChanged(new LabelProviderChangedEvent(
									projectDecorator, project));
				}
			});
		}
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	@Override
	public void earlyStartup() {
	}
}
