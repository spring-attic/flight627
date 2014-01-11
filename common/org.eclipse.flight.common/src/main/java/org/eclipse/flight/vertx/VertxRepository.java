package org.eclipse.flight.vertx;

import org.eclipse.flight.Ids;
import org.eclipse.flight.messages.ResponseMessage;
import org.eclipse.flight.objects.FlightObject;
import org.eclipse.flight.objects.NullFlightObject;
import org.eclipse.flight.objects.Project;
import org.eclipse.flight.objects.Repository;
import org.eclipse.flight.objects.Resource;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.streams.Pump;

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

/**
 * @author Miles Parker
 */
public class VertxRepository extends Repository {

	public VertxRepository(VertxManager vertx) {

		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.CREATE_PROJECT) {

			@Override
			public FlightObject respond(FlightObject request) {
				return putProject(((Project) request).getName());
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.GET_PROJECT) {

			@Override
			public FlightObject respond(FlightObject request) {
				return getProject(((Project) request).getName());
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.GET_ALL_PROJECTS, true) {

			@Override
			public FlightObject respond(FlightObject request) {
				return VertxRepository.this;
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.GET_RESOURCE) {

			@Override
			public FlightObject respond(FlightObject request) {
				return getResource((Resource) request);
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.HAS_RESOURCE, true) {

			@Override
			public FlightObject respond(FlightObject request) {
				return request;
			}
			
			@Override
			public void modifyJsonResponse(FlightObject request, JsonObject json) {
				json.putBoolean("exists", getResource((Resource) request) != null);
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.NEEDS_UPDATE_RESOURCE, true) {

			@Override
			public FlightObject respond(FlightObject request) {
				return request;
			}
			
			@Override
			public void modifyJsonResponse(FlightObject request, JsonObject json) {
				json.putBoolean("needsUpdate", needsUpdate((Resource) request));
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.CREATE_RESOURCE, true) {

			@Override
			public FlightObject respond(FlightObject request) {
				return putResource((Resource) request);
			}
		});

//		HttpServer server = vertx.getVertx().createHttpServer();
//
//		server.websocketHandler(new Handler<ServerWebSocket>() {
//			public void handle(final ServerWebSocket sock) {
//				Pump.createPump(sock, sock).start();
//			}
//		}).listen(3001, "localhost");
	}
	//
	// @Override
	// public Vertx getVertx() {
	// return vertx;
	// }
}
