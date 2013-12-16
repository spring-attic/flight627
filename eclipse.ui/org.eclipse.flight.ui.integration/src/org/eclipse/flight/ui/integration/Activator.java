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
package org.eclipse.flight.ui.integration;

import org.eclipse.core.resources.IProject;
import org.eclipse.flight.core.IRepositoryListener;
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

	/**
	 * The constructor
	 */
	public Activator() {
	}

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
