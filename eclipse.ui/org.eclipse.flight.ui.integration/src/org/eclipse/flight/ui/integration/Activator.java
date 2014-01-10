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

import org.eclipse.core.resources.IProject;
import org.eclipse.flight.core.IRepositoryListener;
import org.eclipse.flight.core.LiveEditCoordinator;
import org.eclipse.flight.core.WorkspaceRepository;
import org.eclipse.flight.ui.integration.handlers.LiveEditConnector;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Martin Lippert
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.flight.ui.integration"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		System.out.println("ui activator");
		plugin = this;
		
		org.eclipse.flight.core.Activator.getDefault().getRepository().addRepositoryListener(new IRepositoryListener() {
			@Override
			public void projectDisconnected(IProject project) {
				updateProjectLabel(project);
			}
			@Override
			public void projectConnected(IProject project) {
				updateProjectLabel(project);
			}
		});
		
		if (Boolean.getBoolean("flight-eclipse-editor-connect")) {
			WorkspaceRepository repository = org.eclipse.flight.core.Activator.getDefault().getRepository();
			LiveEditCoordinator liveEditCoordinator = org.eclipse.flight.core.Activator.getDefault().getLiveEditCoordinator();
			new LiveEditConnector(liveEditCoordinator, repository);
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
	public static Activator getDefault() {
		return plugin;
	}
	
	protected static void updateProjectLabel(final IProject project) {
		final CloudProjectDecorator projectDecorator = CloudProjectDecorator.getInstance();
		if (projectDecorator != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					projectDecorator.fireLabelProviderChanged(new LabelProviderChangedEvent(projectDecorator, project));
				}
			});
		}
	}
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
