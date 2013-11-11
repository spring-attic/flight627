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

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.json.JSONException;
import org.json.JSONObject;

public class NavigationService {

	private String resourcePath;
	private ICompilationUnit unit;

	public NavigationService(String resourcePath, ICompilationUnit unit) {
		this.resourcePath = resourcePath;
		this.unit = unit;
	}

	public JSONObject compute(int offset, int length) {
		try {
			IJavaElement[] elements = unit.codeSelect(offset, length);

			if (elements != null && elements.length > 0) {
				JSONObject result = new JSONObject();
				
				IJavaElement element = elements[0];
				IResource resource = element.getResource();
				
				if (resource != null && resource.getProject() != null) {
					String projectName = resource.getProject().getName();
					String resourcePath = resource.getProjectRelativePath().toString();
					
					result.put("project", projectName);
					result.put("resource", resourcePath);
	
					if (element instanceof ISourceReference) {
						ISourceRange nameRange = ((ISourceReference) element).getNameRange();
						result.put("offset", nameRange.getOffset());
						result.put("length", nameRange.getLength());
					}
					
					return result;
				}
			}

		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

}
