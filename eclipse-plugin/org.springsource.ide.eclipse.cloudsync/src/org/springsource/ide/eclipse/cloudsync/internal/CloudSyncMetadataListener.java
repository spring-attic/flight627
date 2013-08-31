package org.springsource.ide.eclipse.cloudsync.internal;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

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
