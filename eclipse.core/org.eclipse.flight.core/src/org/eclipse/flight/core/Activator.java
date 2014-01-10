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
package org.eclipse.flight.core;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.flight.core.internal.CloudSyncMetadataListener;
import org.eclipse.flight.core.internal.CloudSyncResourceListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Martin Lippert
 */
public class Activator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.flight.core"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private WorkspaceRepository repository;

	private LiveEditCoordinator liveEditCoordinator;

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("core activator");

		plugin = this;

		String username = System.getProperty("flight-username", "defaultuser");
		// TODO: change this username property to a preference and add authentication

		repository = new WorkspaceRepository(username);
		liveEditCoordinator = new LiveEditCoordinator();

		CloudSyncResourceListener resourceListener = new CloudSyncResourceListener(repository);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);

		CloudSyncMetadataListener metadataListener = new CloudSyncMetadataListener(repository);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(metadataListener, IResourceChangeEvent.POST_BUILD);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public WorkspaceRepository getRepository() {
		return repository;
	}

	public LiveEditCoordinator getLiveEditCoordinator() {
		return liveEditCoordinator;
	}

}
