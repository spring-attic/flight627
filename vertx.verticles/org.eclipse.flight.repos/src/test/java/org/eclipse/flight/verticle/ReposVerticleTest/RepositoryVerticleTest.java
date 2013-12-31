package org.eclipse.flight.verticle.ReposVerticleTest;/*
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
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import static org.hamcrest.CoreMatchers.is;
import static org.vertx.testtools.VertxAssert.assertNotNull;
import static org.vertx.testtools.VertxAssert.assertThat;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.eclipse.flight.messages.Messages;
import org.eclipse.flight.verticle.repos.Resource;
import org.eclipse.flight.verticle.repos.ResourceIdentifier;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

/**
 * Maintaining these tests is a PITA. We need a better way of handling all of
 * these reply responses.
 */
public class RepositoryVerticleTest extends TestVerticle {

	@Test
	public void testGetProjectsEmpty() {
		vertx.eventBus().send(Messages.GET_ALL_PROJECTS, "",
				new Handler<Message<JsonArray>>() {
					@Override
					public void handle(Message<JsonArray> reply) {
						assertThat(reply.body().size(), is(0));
						testComplete();
					}
				});
	}

	@Test
	public void testCreateProjects() {
		vertx.eventBus().send(Messages.CREATE_PROJECT, "foo",
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> reply) {
						assertThat(reply.body().getString("id"), is("foo"));
						vertx.eventBus().send(Messages.GET_ALL_PROJECTS, "",
								new Handler<Message<JsonArray>>() {
									@Override
									public void handle(Message<JsonArray> reply) {
										assertThat(reply.body().size(), is(1));
										assertThat(((JsonObject) reply.body().get(0))
												.getString("id"), is("foo"));
										testComplete();
									}
								});
					}
				});
	}

	@Test
	public void testGetProject() {
		vertx.eventBus().send(Messages.CREATE_PROJECT, "foo",
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> reply) {
						assertThat(reply.body().getString("id"), is("foo"));
						vertx.eventBus().send(Messages.GET_PROJECT, "foo",
								new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> reply) {
										assertThat(reply.body().getString("id"),
												is("foo"));
										testComplete();
									}
								});
					}
				});
	}

	@Test
	public void testCreateResource() {
		final Resource resource = new Resource();
		resource.setHash("12345678");
		resource.setPath("src/foo/bar/MyClass.java");
		resource.setTimestamp(System.currentTimeMillis());
		resource.setType("java");
		resource.setUserName("defaultUser");
		resource.setData("package foo.bar;...");
		resource.setProjectId("my.project");

		final ResourceIdentifier resourceIdent = new ResourceIdentifier();
		resourceIdent.setProjectId("my.project");
		resourceIdent.setPath("src/foo/bar/MyClass.java");
		vertx.eventBus().send(Messages.CREATE_PROJECT, "my.project",
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> reply) {

						vertx.eventBus().send(Messages.CREATE_RESOURCE,
								resource.toJson(), new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> reply) {
										assertThat(reply.body().getString("path"),
												is("src/foo/bar/MyClass.java"));
										assertThat(reply.body().getString("hash"),
												is("12345678"));
										assertThat(reply.body().getString("data"),
												is((String) null));
										vertx.eventBus().send(Messages.GET_RESOURCE,
												resourceIdent.toJson(),
												new Handler<Message<JsonObject>>() {
													@Override
													public void handle(
															Message<JsonObject> reply) {
														assertThat(reply.body()
																.getString("hash"),
																is("12345678"));
														assertThat(reply.body()
																.getString("data"),
																is("package foo.bar;..."));
														testComplete();
													}
												});
									}
								});
					}
				});
	}

	@Test
	public void testHasResource() {
		final Resource resource = new Resource();
		resource.setHash("12345678");
		resource.setPath("src/foo/bar/MyClass.java");
		resource.setTimestamp(System.currentTimeMillis());
		resource.setType("java");
		resource.setUserName("defaultUser");
		resource.setData("package foo.bar;...");
		resource.setProjectId("my.project");

		final ResourceIdentifier resourceIdent = new ResourceIdentifier();
		resourceIdent.setProjectId("my.project");
		resourceIdent.setPath("src/foo/bar/MyClass.java");
		vertx.eventBus().send(Messages.CREATE_PROJECT, "my.project",
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> reply) {
						vertx.eventBus().send(Messages.CREATE_RESOURCE,
								resource.toJson(), new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> reply) {
										vertx.eventBus().send(Messages.HAS_RESOURCE,
												resourceIdent.toJson(),
												new Handler<Message<Boolean>>() {
													@Override
													public void handle(
															Message<Boolean> reply) {
														assertThat(reply.body(), is(true));
														testComplete();
													}
												});
									}
								});
					}
				});
	}

	@Test
	public void testNeedsUpdate() {
		//TODO
		testComplete();
	}
	
	@Override
	public void start() {
		// Make sure we call initialize() - this sets up the assert stuff so
		// assert functionality works correctly
		initialize();
		// Deploy the module - the System property `vertx.modulename` will
		// contain the name of the module so you
		// don't have to hardecode it in your tests
		container.deployModule(System.getProperty("vertx.modulename"),
				new AsyncResultHandler<String>() {
					@Override
					public void handle(AsyncResult<String> asyncResult) {
						// Deployment is asynchronous and this this handler will
						// be called when it's complete (or failed)
						if (asyncResult.failed()) {
							container.logger().error(asyncResult.cause());
						}
						assertTrue(asyncResult.succeeded());
						assertNotNull("deploymentID should not be null",
								asyncResult.result());
						// If deployed correctly then start the tests!
						startTests();
					}
				});
	}

}
