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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.flight.resources.Resource;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 */
public class ConnectedResource extends Resource {

	/*
	 * For Eclipse resources, we never want to save the data as part of the
	 * resource, so we add it to the marshalled data here instead.
	 * 
	 * @see
	 * org.eclipse.flight.resources.Resource#toJson(org.vertx.java.core.json
	 * .JsonObject, boolean)
	 */
	@Override
	protected void toJson(JsonObject json, boolean thin) {
		super.toJson(json, thin);
		if (!thin) {
			IResource eclipseResource = ((ConnectedProject) getProject()).getProject().findMember(getPath());

			if (eclipseResource instanceof IFile) {
				IFile file = (IFile) eclipseResource;
				try {
					ByteArrayOutputStream array = new ByteArrayOutputStream();
					if (!file.isSynchronized(IResource.DEPTH_ZERO)) {
						file.refreshLocal(IResource.DEPTH_ZERO, null);
					}

					IOUtils.copy(file.getContents(), array);

					String content = new String(array.toByteArray(), file.getCharset());
					json.putString("data", content);
				} catch (CoreException e) {
					json.putString("failed", "Exception: " + e.getMessage());
				} catch (IOException e) {
					json.putString("failed", "Exception: " + e.getMessage());
				}
			}
		}
	}
}
