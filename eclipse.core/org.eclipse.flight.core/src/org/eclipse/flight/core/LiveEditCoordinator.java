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

import org.eclipse.flight.Ids;
import org.eclipse.flight.objects.FlightObject;
import org.eclipse.flight.objects.services.Edit;
import org.eclipse.flight.vertx.Receiver;
import org.eclipse.flight.vertx.VertxManager;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Martin Lippert
 * @author Miles Parker
 */
public class LiveEditCoordinator {

	private Collection<ILiveEditConnector> liveEditConnectors;

	public LiveEditCoordinator() {
		this.liveEditConnectors = new CopyOnWriteArrayList<>();
		VertxManager.get().register(new Receiver(Ids.EDIT_PARTICIPANT, Ids.LIVE_RESOURCE_STARTED) {

			@Override
			public void receive(FlightObject edit) {
				startLiveUnit((Edit) edit);
			}
		});
		VertxManager.get().register(new Receiver(Ids.EDIT_PARTICIPANT, Ids.LIVE_RESOURCE_RESPONSE) {

			@Override
			public void receive(FlightObject edit) {
				startLiveUnitResponse((Edit) edit);
			}
		});
		VertxManager.get().register(new Receiver(Ids.EDIT_PARTICIPANT, Ids.LIVE_RESOURCE_CHANGED) {

			@Override
			public void receive(FlightObject edit) {
				modelChanged((Edit) edit);
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
		VertxManager.get().publish(Ids.EDIT_PARTICIPANT, Ids.LIVE_RESOURCE_CHANGED, edit);

		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(edit.getEditType())) {
				connector.liveEditingEvent(edit);
			}
		}
	}

	public void sendLiveEditStartedMessage(Edit edit) {
		VertxManager.get().publish(Ids.EDIT_PARTICIPANT, Ids.LIVE_RESOURCE_STARTED, edit);

		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(edit.getEditType())) {
				connector.liveEditingStarted(edit);
			}
		}
	}

	protected void startLiveUnitResponse(Edit edit) {
		for (ILiveEditConnector connector : liveEditConnectors) {
			connector.liveEditingStartedResponse(edit);
		}
	}

	public void sendLiveEditStartedResponse(Edit edit) {
		VertxManager.get().publish(Ids.EDIT_PARTICIPANT, Ids.LIVE_RESOURCE_RESPONSE, edit);

		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(edit.getEditType())) {
				connector.liveEditingStartedResponse(edit);
			}
		}
	}

}
