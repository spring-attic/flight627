package org.eclipse.flight.resources.vertx;

import org.eclipse.flight.Constants;
import org.eclipse.flight.resources.Project;
import org.eclipse.flight.resources.Repository;
import org.eclipse.flight.resources.Resource;
import org.eclipse.flight.resources.Response;
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
 * 
 */
public class VertxRepository extends Repository {

	public VertxRepository(Vertx vertx) {
		vertx.eventBus().registerHandler(Constants.RESOURCE_PROVIDER,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						if (message.body().getString("kind").equals("request")) {
							String desc = message.body().getString("description");
							JsonObject contents = message.body().getObject("contents");
							switch (desc) {
							case Constants.CREATE_PROJECT: {
								Project project = putProject(contents.getString("name"));
								message.reply(new Response(desc, project).toJson(true));
								return;
							}
							case Constants.GET_PROJECT: {
								Project project = getProject(contents.getString("name"));
								if (project != null) {
									message.reply(new Response(desc, project).toJson());
								} else {
									message.fail(404, "No project found.");
								}
								return;
							}
							case Constants.GET_ALL_PROJECTS: {
								message.reply(new Response(desc, VertxRepository.this)
										.toJson(true));
								return;
							}
							case Constants.GET_RESOURCE:
							case Constants.HAS_RESOURCE:
							case Constants.NEEDS_UPDATE_RESOURCE:
							case Constants.CREATE_RESOURCE:
								Resource remoteResource = new Resource();
								remoteResource.fromJson(contents);
								Project project = getProject(remoteResource
										.getProjectName());
								if (getProject(remoteResource.getProjectName()) == null) {
									message.fail(404, "No project exists with name: "
											+ remoteResource.getProjectName());
									return;
								}
								Resource localResource = project
										.getResource(remoteResource);
								switch (desc) {
								case Constants.GET_RESOURCE:
									if (localResource != null) {
										message.reply(new Response(desc, localResource)
												.toJson());
									} else {
										message.fail(404, "No resources found at: "
												+ remoteResource);
									}
									return;
								case Constants.HAS_RESOURCE:
									message.reply(new Response(desc, remoteResource)
											.toJson(true).putBoolean("exists",
													localResource != null));
									return;
								case Constants.NEEDS_UPDATE_RESOURCE: {
									boolean needsUpdate = project
											.needsUpdate(remoteResource);
									message.reply(new Response(desc, remoteResource)
											.toJson(true).putBoolean("needsUpdate",
													needsUpdate));
									return;
								}
								case Constants.CREATE_RESOURCE: {
									project.putResource(remoteResource);
									message.reply(new Response(desc, remoteResource)
											.toJson(true));
									return;
								}
								}
							}
						}

					}
				});
		HttpServer server = vertx.createHttpServer();

		server.websocketHandler(new Handler<ServerWebSocket>() {
			public void handle(final ServerWebSocket sock) {
				Pump.createPump(sock, sock).start();
			}
		}).listen(3001, "localhost");
	}
	//
	// @Override
	// public Vertx getVertx() {
	// return vertx;
	// }
}
