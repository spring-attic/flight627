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
package org.eclipse.flight.verticle;

import org.eclipse.flight.messages.Messages;
import org.eclipse.flight.resources.Project;
import org.eclipse.flight.resources.Resource;
import org.eclipse.flight.resources.ResourceAddress;
import org.eclipse.flight.resources.Response;
import org.eclipse.flight.verticle.repos.RepositoryInMemory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * A simple in memory container for shared project resources.
 * 
 * @author Miles Parker
 */
public class VolatileRepositoryVerticle extends Verticle {

	RepositoryInMemory repos = new RepositoryInMemory();

	public void start() {

		final Logger logger = container.logger();

		vertx.eventBus().registerHandler(Messages.RESOURCE_PROVIDER,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						if (message.body().getString("kind").equals("request")) {
							String desc = message.body().getString("description");
							JsonObject contents = message.body().getObject("contents");
							switch (desc) {
							case Messages.CREATE_PROJECT: {
								Project project = repos.createProject(contents
										.getString("name"));
								message.reply(new Response(desc, project.toJson()).toJson());
								logger.info("Got project " + project);
								return;
							}
							case Messages.GET_PROJECT: {
								Project project = repos.getProject(contents
										.getString("name"));
								if (project != null) {
									message.reply(new Response(desc, project.toJson()).toJson());
								} else {
									message.fail(404, "No project found.");
								}
								logger.info("Got project " + project);
								return;
							}
							case Messages.GET_ALL_PROJECTS: {
								message.reply(new Response(desc, repos
										.getProjectsAsJson()).toJson());
								return;
							}
							case Messages.GET_RESOURCE: {
								ResourceAddress ident = ResourceAddress
										.createJsonResourceIdentifier(contents);
								Resource resource = repos.getResource(ident);
								if (resource != null) {
									message.reply(new Response(desc, resource.toJson()).toJson());
								} else {
									message.fail(404, "No resources found at: " + ident);
								}
								logger.info("Got resource " + ident);
								return;
							}
							case Messages.HAS_RESOURCE: {
								ResourceAddress ident = ResourceAddress
										.createJsonResourceIdentifier(contents);
								boolean hasResource = repos.hasResource(ident);
								JsonObject reply = new JsonObject().putBoolean("exists",
										hasResource);
								message.reply(new Response(desc, reply).toJson().putObject("request", message.body().getObject("contents")));
								logger.info("Had resource " + ident);
								return;
							}
							case Messages.NEEDS_UPDATE_RESOURCE: {
								ResourceAddress ident = ResourceAddress
										.createJsonResourceIdentifier(contents);
								boolean needsUpdate = repos.needsUpdate(ident);
								message.reply(new Response(desc, (new JsonObject())
										.putBoolean("needsUpdate", needsUpdate)).toJson());
								logger.info("Needs Update " + ident);
							}
							case Messages.CREATE_RESOURCE: {
								Resource resource = Resource
										.createFromJsonResource(contents);
								if (repos.getProject(resource.getProjectName()) == null) {
									message.fail(404, "No project exists with name: "
											+ resource.getProjectName());
								}
								ResourceAddress ident = repos.putResource(resource);
								message.reply(new Response(desc, ident.toJson()).toJson());
								logger.info("Created resource " + ident);
							}
							}
						}
					}
				});

	}
}
