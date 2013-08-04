package org.springsource.ide.eclipse.cloudsync.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class CloudSyncResourceListener implements IResourceChangeListener {

	private CloudSyncController controller;
	private CloudSyncService service;

	public CloudSyncResourceListener(CloudSyncController controller, CloudSyncService service) {
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
					if (project != null) {
						if (controller.isConnected(project)) {
							service.sendResourceUpdate(controller.getProject(project), delta);
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
