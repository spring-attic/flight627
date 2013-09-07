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
package org.springsource.ide.eclipse.cloudsync.internal;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Martin Lippert
 */
public class CloudSyncMetadataListener implements IResourceChangeListener{

	private CloudSyncController controller;
	private CloudSyncService service;

	public CloudSyncMetadataListener(CloudSyncController controller, CloudSyncService service) {
		this.controller = controller;
		this.service = service;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IProject project = delta.getResource().getProject();
					IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
					if (project != null && controller.isConnected(project) && markerDeltas != null && markerDeltas.length > 0) {
						service.updateMetadata(controller.getProject(project), delta.getResource());
					}
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
