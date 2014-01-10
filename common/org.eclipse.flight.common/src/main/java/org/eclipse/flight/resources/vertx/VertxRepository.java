package org.eclipse.flight.resources.vertx;

import org.eclipse.flight.Ids;
import org.eclipse.flight.resources.Project;
import org.eclipse.flight.resources.Repository;
import org.eclipse.flight.resources.Resource;
import org.eclipse.flight.resources.ResponseMessage;
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
			public Object respond(JsonObject request) {
				Project project = putProject(request.getString("name"));
				return project.toJson(true);
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.GET_PROJECT) {

			@Override
			public Object respond(JsonObject request) {
				Project project = getProject(request.getString("name"));
				return project.toJson();
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.GET_ALL_PROJECTS) {

			@Override
			public Object respond(JsonObject request) {
				return VertxRepository.this.toJson(true);
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.GET_RESOURCE) {

			@Override
			public Object respond(JsonObject request) {
				Resource remoteResource = new Resource();
				remoteResource.fromJson(request);
				return getResource(remoteResource);
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.HAS_RESOURCE) {

			@Override
			public Object respond(JsonObject request) {
				Resource remoteResource = new Resource();
				remoteResource.fromJson(request);
				Resource resource = getResource(remoteResource);
				return remoteResource.toJson().putBoolean("exists", resource != null);
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.NEEDS_UPDATE_RESOURCE) {

			@Override
			public Object respond(JsonObject request) {
				Resource remoteResource = new Resource();
				remoteResource.fromJson(request);
				return new JsonObject().putBoolean("needsUpdate", needsUpdate(remoteResource));
			}
		});
		vertx.register(new Responder(Ids.RESOURCE_PROVIDER, Ids.CREATE_RESOURCE) {

			@Override
			public Object respond(JsonObject request) {
				Resource remoteResource = new Resource();
				remoteResource.fromJson(request);
				return putResource(remoteResource).toJson(true);
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
