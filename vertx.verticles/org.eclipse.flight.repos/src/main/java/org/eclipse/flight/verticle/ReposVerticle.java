/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 *
 */
package org.eclipse.flight.verticle;

import java.util.Collection;

import org.eclipse.flight.messages.Messages;
import org.eclipse.flight.verticle.repos.Project;
import org.eclipse.flight.verticle.repos.RepositoryInMemory;
import org.eclipse.flight.verticle.repos.Resource;
import org.eclipse.flight.verticle.repos.ResourceIdentifier;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/*
 * A simple in memory container for shared project resources.
 */
public class ReposVerticle extends Verticle {

	RepositoryInMemory repos = new RepositoryInMemory();

	public void start() {

		final Logger logger = container.logger();

		vertx.eventBus().registerHandler(Messages.CREATE_PROJECT,
				new Handler<Message<String>>() {
					@Override
					public void handle(Message<String> message) {
						Project project = repos.createProject(message.body());
						message.reply(project.toJson());
						logger.info("Got project " + project);
					}
				});

		vertx.eventBus().registerHandler(Messages.GET_PROJECT,
				new Handler<Message<String>>() {
					@Override
					public void handle(Message<String> message) {
						Project project = repos.getProject(message.body());
						if (project != null) {
							message.reply(project.toJson());
						} else {
							message.fail(404, "No project found.");
						}
						logger.info("Got project " + project);
					}
				});

		vertx.eventBus().registerHandler(Messages.GET_ALL_PROJECTS,
				new Handler<Message<String>>() {
					@Override
					public void handle(Message<String> message) {
						message.reply(repos.getProjectsAsJson());
					}
				});

		vertx.eventBus().registerHandler(Messages.GET_RESOURCE,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						ResourceIdentifier ident = ResourceIdentifier
								.createJsonResourceIdentifier(message.body());
						Resource resource = repos.getResource(ident);
						if (resource != null) {
							message.reply(resource.toJson());
						} else {
							message.fail(404, "No resources found at: " + ident);
						}
						logger.info("Got resource " + ident);
					}
				});

		vertx.eventBus().registerHandler(Messages.HAS_RESOURCE,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						ResourceIdentifier ident = ResourceIdentifier
								.createJsonResourceIdentifier(message.body());
						boolean hasResource = repos.hasResource(ident);
						message.reply(hasResource);
						logger.info("Had resource " + ident);
					}
				});

		vertx.eventBus().registerHandler(Messages.NEEDS_UPDATE_RESOURCE,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						ResourceIdentifier ident = ResourceIdentifier
								.createJsonResourceIdentifier(message.body());
						boolean needsUpdate = repos.needsUpdate(ident);
						message.reply(needsUpdate);
						logger.info("Needs Update " + ident);
					}
				});

		vertx.eventBus().registerHandler(Messages.CREATE_RESOURCE,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						Resource resource = Resource.createFromJsonResource(message.body());
						ResourceIdentifier ident = repos.putResource(resource);
						message.reply(ident.toJson());
						logger.info("Had resource " + ident);
					}
				});
	}
}
