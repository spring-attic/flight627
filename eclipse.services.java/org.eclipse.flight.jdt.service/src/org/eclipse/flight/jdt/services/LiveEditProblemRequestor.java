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
package org.eclipse.flight.jdt.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.flight.Constants;
import org.eclipse.flight.resources.Edit;
import org.eclipse.flight.resources.Resource;
import org.eclipse.flight.resources.ResourceMarker;
import org.eclipse.flight.resources.vertx.VertxManager;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.compiler.IProblem;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class LiveEditProblemRequestor implements IProblemRequestor {

	private Resource resource;

	private List<IProblem> problems;

	public LiveEditProblemRequestor(Resource resource) {
		this.resource = resource;
		this.problems = new ArrayList<IProblem>();
	}

	@Override
	public void acceptProblem(IProblem problem) {
		this.problems.add(problem);
	}

	@Override
	public void beginReporting() {
		this.problems.clear();
	}

	@Override
	public void endReporting() {
		sendMarkers((IProblem[]) this.problems.toArray(new IProblem[this.problems.size()]));
	}

	@Override
	public boolean isActive() {
		return true;
	}

	private void sendMarkers(IProblem[] problems) {
		resource.getMarkers().clear();
		for (IProblem problem : problems) {
			ResourceMarker marker = new ResourceMarker();
			marker.setDescription(problem.getMessage());
			marker.setSeverity((problem.isError() ? "error" : "warning"));
			marker.setStart(problem.getSourceLineNumber());
			marker.setStart(problem.getSourceStart());
			marker.setEnd(problem.getSourceEnd() + 1);
			resource.getMarkers().add(marker);
		}
		VertxManager.get().publish(Constants.EDIT_PARTICIPANT, Constants.LIVE_METADATA_CHANGED, resource);
	}
}
