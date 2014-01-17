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
package org.eclipse.flight.ui.integration.handlers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.flight.core.ConnectedProject;
import org.eclipse.flight.core.ConnectedResource;
import org.eclipse.flight.core.ILiveEditConnector;
import org.eclipse.flight.core.IRepositoryListener;
import org.eclipse.flight.core.LiveEditCoordinator;
import org.eclipse.flight.core.WorkspaceRepository;
import org.eclipse.flight.objects.Resource;
import org.eclipse.flight.objects.services.Edit;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * @author Martin Lippert
 */
public class LiveEditConnector {

	private static final String LIVE_EDIT_CONNECTOR_ID = "UI-Editor-Live-Edit-Connector";

	private IDocumentListener documentListener;

	private WorkspaceRepository repository;

	private ConcurrentMap<IDocument, String> resourceMappings;

	private ConcurrentMap<String, IDocument> documentMappings;

	private LiveEditCoordinator liveEditCoordinator;

	public LiveEditConnector(LiveEditCoordinator liveEditCoordinator, WorkspaceRepository repository) {
		this.liveEditCoordinator = liveEditCoordinator;
		this.repository = repository;

		this.resourceMappings = new ConcurrentHashMap<IDocument, String>();
		this.documentMappings = new ConcurrentHashMap<String, IDocument>();

		this.documentListener = new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
				sendModelChangedMessage(event);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		};

		FileBuffers.getTextFileBufferManager().addFileBufferListener(new IFileBufferListener() {
			@Override
			public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
			}

			@Override
			public void underlyingFileDeleted(IFileBuffer buffer) {
			}

			@Override
			public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
			}

			@Override
			public void stateChanging(IFileBuffer buffer) {
			}

			@Override
			public void stateChangeFailed(IFileBuffer buffer) {
			}

			@Override
			public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {
			}

			@Override
			public void bufferDisposed(IFileBuffer buffer) {
			}

			@Override
			public void bufferCreated(IFileBuffer buffer) {
			}

			@Override
			public void bufferContentReplaced(IFileBuffer buffer) {
				IPath path = buffer.getLocation();

				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				IResource resource = root.findMember(path);
				if (resource != null) {
					IProject project = resource.getProject();
					String resourcePath = resource.getProjectRelativePath().toString();

					String fullPath = project.getName() + "/" + resourcePath;
					IDocument doc = documentMappings.get(fullPath);
					if (doc != null) {
						doc.addDocumentListener(documentListener);
					}
				}

				System.out.println("content replaced by new version on the file system");
			}

			@Override
			public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
				IPath path = buffer.getLocation();

				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				IResource resource = root.findMember(path);
				if (resource != null) {
					IProject project = resource.getProject();
					String resourcePath = resource.getProjectRelativePath().toString();

					String fullPath = project.getName() + "/" + resourcePath;
					IDocument doc = documentMappings.get(fullPath);
					if (doc != null) {
						doc.removeDocumentListener(documentListener);
					}
				}
			}
		});

		this.repository.addRepositoryListener(new IRepositoryListener() {
			@Override
			public void projectConnected(IProject project) {
				connectOpenEditors(project);
			}

			@Override
			public void projectDisconnected(IProject project) {
				disconnectOpenEditors(project);
			}
		});

		ILiveEditConnector liveEditConnector = new ILiveEditConnector() {

			@Override
			public String getConnectorID() {
				return LIVE_EDIT_CONNECTOR_ID;
			}

			@Override
			public void liveEditingEvent(Edit edit) {
				handleModelChanged(edit);
			}

			@Override
			public void liveEditingStarted(Edit edit) {
				remoteEditorStarted(edit);
			}

			@Override
			public void liveEditingStartedResponse(Edit edit) {
				handleRemoteLiveContent(edit);
			}
		};
		this.liveEditCoordinator.addLiveEditConnector(liveEditConnector);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(new IResourceChangeListener() {
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				reactToResourceChanged(event);
			}
		});

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				window.getActivePage().addPartListener(new IPartListener2() {
					@Override
					public void partVisible(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partOpened(IWorkbenchPartReference partRef) {
						IWorkbenchPart part = partRef.getPart(false);
						if (part instanceof AbstractTextEditor) {
							connectEditor((AbstractTextEditor) part);
						}
					}

					@Override
					public void partInputChanged(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partHidden(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partDeactivated(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partClosed(IWorkbenchPartReference partRef) {
						IWorkbenchPart part = partRef.getPart(false);
						if (part instanceof AbstractTextEditor) {
							disconnectEditor((AbstractTextEditor) part);
						}
					}

					@Override
					public void partBroughtToTop(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partActivated(IWorkbenchPartReference partRef) {
					}
				});
			}
		});
	}

	protected void remoteEditorStarted(Edit edit) {
		// a different editor was started editing the resource, we need to send back live content
		if (this.repository.getUsername().equals(edit.getUserName())
				&& documentMappings.containsKey(edit.getFullPath())) {
			final IDocument document = documentMappings.get(edit.getFullPath());
			edit.setData(document.get());
			this.liveEditCoordinator.sendLiveEditStartedResponse(edit);
		}
	}

	protected void handleRemoteLiveContent(final Edit edit) {
		// we started the editing and are getting remote live content back
		if (this.repository.getUsername().equals(edit.getUserName())
				&& documentMappings.containsKey(edit.getFullPath())) {
			final IDocument document = documentMappings.get(edit.getFullPath());

			ConnectedProject connectedProject = (ConnectedProject) repository.getProject(edit.getProjectName());
			final ConnectedResource localResource = (ConnectedResource) connectedProject.getResource(edit.getPath());

			if (localResource.getHash() != null && localResource.getHash().equals(edit.getSavePointHash())
					&& localResource.getTimestamp() == edit.getSavePointTimestamp()) {
				String openedContent = document.get();
				if (!openedContent.equals(localResource.getData())) {
					try {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								try {
									document.removeDocumentListener(documentListener);
									document.set(localResource.getData());
									document.addDocumentListener(documentListener);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected void handleModelChanged(final Edit edit) {
		if (repository.getUsername().equals(edit.getUserName()) && edit.getFullPath() != null
				&& documentMappings.containsKey(edit.getFullPath())) {
			final IDocument document = documentMappings.get(edit.getFullPath());

			try {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						try {
							document.removeDocumentListener(documentListener);
							document.replace(edit.getOffset(), edit.getRemoveCount(), edit.getData());
							document.addDocumentListener(documentListener);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void sendModelChangedMessage(DocumentEvent event) {
		String resourcePath = resourceMappings.get(event.getDocument());
		if (resourcePath != null) {
			String projectName = resourcePath.substring(0, resourcePath.indexOf('/'));
			String relativeResourcePath = resourcePath.substring(projectName.length() + 1);
			Edit edit = new Edit();
			edit.setUserName(repository.getUsername());
			edit.setProjectName(projectName);
			edit.setPath(relativeResourcePath);
			edit.setOffset(event.getOffset());
			edit.setRemoveCount(event.getLength());
			edit.setData(event.getText());
			edit.setEditType(LIVE_EDIT_CONNECTOR_ID);
			this.liveEditCoordinator.sendModelChangedMessage(edit);
		}
	}

	protected void connectEditor(AbstractTextEditor texteditor) {
		final IDocument document = texteditor.getDocumentProvider().getDocument(texteditor.getEditorInput());
		IResource eclipseResource = (IResource) texteditor.getEditorInput().getAdapter(IResource.class);

		if (document != null && eclipseResource != null) {
			IProject project = eclipseResource.getProject();
			String resourcePath = eclipseResource.getProject().getName() + "/"
					+ eclipseResource.getProjectRelativePath().toString();

			if (repository.isConnected(project)) {
				documentMappings.put(resourcePath, document);
				resourceMappings.put(document, resourcePath);

				document.addDocumentListener(documentListener);

				ConnectedProject connectedProject = (ConnectedProject) repository.getProject(project);
				Resource resource = connectedProject.getResource(eclipseResource.getProjectRelativePath().toString());
				this.liveEditCoordinator.sendLiveEditStartedMessage(resource.toEdit());
			}
		}
	}

	protected void disconnectEditor(AbstractTextEditor texteditor) {
		final IDocument document = texteditor.getDocumentProvider().getDocument(texteditor.getEditorInput());

		String resourcePath = resourceMappings.get(document);

		document.removeDocumentListener(documentListener);
		if (resourcePath != null) {
			documentMappings.remove(resourcePath);
			resourceMappings.remove(document);
		}
	}

	protected void connectOpenEditors(IProject project) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IEditorReference[] editorReferences = window.getActivePage().getEditorReferences();
				for (IEditorReference editorReference : editorReferences) {
					IEditorPart editorPart = editorReference.getEditor(false);
					if (editorPart instanceof AbstractTextEditor) {
						connectEditor((AbstractTextEditor) editorPart);
					}
				}
			}
		});
	}

	protected void disconnectOpenEditors(IProject project) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IEditorReference[] editorReferences = window.getActivePage().getEditorReferences();
				for (IEditorReference editorReference : editorReferences) {
					IEditorPart editorPart = editorReference.getEditor(false);
					if (editorPart instanceof AbstractTextEditor) {
						disconnectEditor((AbstractTextEditor) editorPart);
					}
				}
			}
		});
	}

	public void reactToResourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					reactToResourceChanged(delta);
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	protected void reactToResourceChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		if (project != null) {
			if (repository.isConnected(project)) {
				reactToResourceChange(delta);
			}
		}
	}

	private void reactToResourceChange(IResourceDelta delta) {
		IResource resource = delta.getResource();

		if (resource != null && resource.isDerived(IResource.CHECK_ANCESTORS)) {
			return;
		}

		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			// TODO: cleanup
			break;
		case IResourceDelta.REMOVED:
			// TODO: cleanup
			break;
		case IResourceDelta.CHANGED:
			reactOnResourceChange(resource);
			break;
		}
	}

	private void reactOnResourceChange(IResource resource) {
		if (resource != null && resource instanceof IFile) {
			final ConnectedProject connectedProject = (ConnectedProject) repository.getProject(resource.getProject());
			final String resourcePath = resource.getProjectRelativePath().toString();

//			if (connectedProject != null && connectedProject.containsResource(resourcePath)) {
//				String key = connectedProject.getName() + "/" + resourcePath;
//				PendingLiveEditStartedResponse pendingResponse = pendingLiveEditStartedResponses.get(key);
//				if (pendingResponse != null) {
//					handleRemoteLiveContent(pendingResponse);
//				}
//			}
		}
	}

}
