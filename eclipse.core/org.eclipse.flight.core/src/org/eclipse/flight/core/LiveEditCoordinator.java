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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.flight.Constants;
import org.eclipse.flight.resources.Edit;
import org.eclipse.flight.resources.vertx.Receiver;
import org.eclipse.flight.resources.vertx.VertxManager;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Martin Lippert
 * @author Miles Parker
 */
public class LiveEditCoordinator {

	private Collection<ILiveEditConnector> liveEditConnectors;

	public LiveEditCoordinator() {
		this.liveEditConnectors = new CopyOnWriteArrayList<>();
		VertxManager.get().register(new Receiver(Constants.EDIT_PARTICIPANT, Constants.LIVE_RESOURCE_STARTED) {

			@Override
			public void receive(JsonObject contents) {
				Edit edit = new Edit();
				edit.fromJson(contents);
				startLiveUnit(edit);
			}
		});
		VertxManager.get().register(new Receiver(Constants.EDIT_PARTICIPANT, Constants.LIVE_RESOURCE_CHANGED) {

			@Override
			public void receive(JsonObject contents) {
				Edit edit = new Edit();
				edit.fromJson(contents);
				modelChanged(edit);
			}
		});
	}

	protected void startLiveUnit(Edit edit) {
		for (ILiveEditConnector connector : liveEditConnectors) {
			connector.liveEditingStarted(edit);
		}
	}

	protected void modelChanged(Edit edit) {
		for (ILiveEditConnector connector : liveEditConnectors) {
			connector.liveEditingEvent(edit);
		}
	}

	public void addLiveEditConnector(ILiveEditConnector connector) {
		liveEditConnectors.add(connector);
	}

	public void removeLiveEditConnector(ILiveEditConnector connector) {
		liveEditConnectors.remove(connector);
	}

	public void sendModelChangedMessage(Edit edit) {
		VertxManager.get().publish(Constants.EDIT_PARTICIPANT, Constants.LIVE_RESOURCE_CHANGED, edit);

		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getEditType().equals(edit.getEditType())) {
				connector.liveEditingEvent(edit);
			}
		}
	}

	public void sendLiveEditStartedMessage(Edit edit) {
		VertxManager.get().publish(Constants.EDIT_PARTICIPANT, Constants.LIVE_RESOURCE_STARTED, edit);

		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getEditType().equals(edit.getEditType())) {
				connector.liveEditingStarted(edit);
			}
		}
	}

	public void sendLiveEditStartedResponse(Edit edit) {
		VertxManager.get().publish(Constants.EDIT_PARTICIPANT, Constants.LIVE_RESOURCE_RESPONSE, edit);

		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getEditType().equals(edit.getEditType())) {
				connector.liveEditingStartedResponse(edit);
			}
		}
	}

}
