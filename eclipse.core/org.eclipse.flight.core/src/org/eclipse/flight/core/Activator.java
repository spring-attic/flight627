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
package org.eclipse.flight.core;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.flight.core.internal.CloudSyncMetadataListener;
import org.eclipse.flight.core.internal.CloudSyncResourceListener;
import org.eclipse.flight.core.internal.messaging.SocketIOMessagingConnector;
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

	private IMessagingConnector messagingConnector;
	private Repository repository;
	private LiveEditCoordinator liveEditCoordinator;
	
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("core activator");
		
		plugin = this;
		messagingConnector = new SocketIOMessagingConnector();
		repository = new Repository(messagingConnector);
		liveEditCoordinator = new LiveEditCoordinator(messagingConnector);
		
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
	
	public IMessagingConnector getMessagingConnector() {
		return messagingConnector;
	}
	
	public Repository getRepository() {
		return repository;
	}
	
	public LiveEditCoordinator getLiveEditCoordinator() {
		return liveEditCoordinator;
	}

}
