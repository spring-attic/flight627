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
package org.eclipse.flight.verticle.ReposVerticleTest;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.vertx.testtools.VertxAssert.assertNotNull;
import static org.vertx.testtools.VertxAssert.assertThat;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.eclipse.flight.messages.Messages;
import org.eclipse.flight.resources.Request;
import org.eclipse.flight.resources.Resource;
import org.eclipse.flight.resources.ResourceAddress;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.eventbus.impl.ReplyFailureMessage;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public class RepositoryVerticleTest extends TestVerticle {

	static long TIME_OUT = 1000;

	class Harness {
		int processIndex;
	}

	abstract class TestHandler {
		private String address;
		private JsonObject message;

		TestHandler next;

		TestHandler(String address, JsonObject message) {
			this.address = address;
			this.message = message;
		}

		/**
		 * Something has happened, so handle it.
		 */
		abstract void expect(Message<JsonObject> reply);

		void execute() {
			vertx.eventBus().send(Messages.RESOURCE_PROVIDER, new Request(address, message).toJson(), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> reply) {
					expect(reply);
					if (next == null) {
						testComplete();
					} else {
						next.execute();
					}
				}
			});
		}

		/**
		 * @param handler
		 *            the next to set
		 */
		public void setNext(TestHandler handler) {
			this.next = handler;
		}
	}

	public void execute(TestHandler... handlers) {
		int index = 0;
		for (TestHandler testHandler : handlers) {
			if (index < handlers.length - 1) {
				testHandler.setNext(handlers[index + 1]);
			}
			testHandler.execute();
			index++;
		}
	}

	TestHandler createFooProject = new TestHandler(Messages.CREATE_PROJECT,
			(new JsonObject()).putString("name", "foo")) {
		@Override
		void expect(Message<JsonObject> reply) {
			assertThat(reply.body().getObject("contents").getString("name"), is("foo"));
		}
	};

	@Test
	public void testGetProjectsEmpty() {
		new TestHandler(Messages.GET_ALL_PROJECTS, new JsonObject()) {
			@Override
			void expect(Message<JsonObject> reply) {
				JsonArray projects = reply.body().getObject("contents").getArray("projects");
				assertThat(projects.size(), is(0));
			}
		}.execute();
	}

	@Test
	public void testCreateProjects() {
		execute(createFooProject, new TestHandler(Messages.GET_ALL_PROJECTS,
				new JsonObject()) {
			@Override
			void expect(Message<JsonObject> reply) {
				JsonArray projects = reply.body().getObject("contents").getArray("projects");
				assertThat(projects.size(), is(1));
				assertThat(((JsonObject) projects.get(0)).getString("name"), is("foo"));
			}
		});
	}

	@Test
	public void testGetProject() {
		execute(createFooProject, new TestHandler(Messages.GET_PROJECT,
				(new JsonObject()).putString("name", "foo")) {
			@Override
			void expect(Message<JsonObject> reply) {
				assertThat(reply.body().getObject("contents").getString("name"), is("foo"));
			}
		});
	}

	@Test
	public void testCreateGetHasResource() {
		final Resource resource = new Resource();
		resource.setHash("12345678");
		resource.setPath("src/foo/bar/MyClass.java");
		resource.setTimestamp(System.currentTimeMillis());
		resource.setType("java");
		resource.setUserName("defaultUser");
		resource.setData("package foo.bar;...");
		resource.setProjectId("foo");

		final ResourceAddress resourceIdent = new ResourceAddress();
		resourceIdent.setProjectId("foo");
		resourceIdent.setPath("src/foo/bar/MyClass.java");

		execute(createFooProject,
				new TestHandler(Messages.CREATE_RESOURCE, resource.toJson()) {
					@Override
					void expect(Message<JsonObject> reply) {
						JsonObject contents = reply.body().getObject("contents");
						assertThat(contents.getString("path"),
								is("src/foo/bar/MyClass.java"));
						assertThat(contents.getString("hash"), is("12345678"));
						assertThat(contents.getString("data"), is((String) null));
					}
				}, new TestHandler(Messages.GET_RESOURCE, resourceIdent.toJson()) {
					@Override
					void expect(Message<JsonObject> reply) {
						JsonObject contents = reply.body().getObject("contents");
						assertThat(contents.getString("hash"), is("12345678"));
						assertThat(contents.getString("data"),
								is("package foo.bar;..."));
					}
				}, new TestHandler(Messages.HAS_RESOURCE, resourceIdent.toJson()) {
					@Override
					void expect(Message<JsonObject> reply) {
						JsonObject contents = reply.body().getObject("contents");
						assertThat(contents.getBoolean("exists"), is(true));
						assertThat(reply.body().getObject("request").getString("path"), is("src/foo/bar/MyClass.java"));
					}
				});
	}

	@Test
	public void testHasResourceFalse() {
		final ResourceAddress resourceIdent = new ResourceAddress();
		resourceIdent.setProjectId("foo");
		resourceIdent.setPath("src/foo/bar/Missing.java");
		execute(createFooProject,
				new TestHandler(Messages.GET_RESOURCE, resourceIdent.toJson()) {
					@Override
					void expect(Message<JsonObject> reply) {
						assertThat(reply, instanceOf(ReplyFailureMessage.class));
					}
				}, new TestHandler(Messages.HAS_RESOURCE, resourceIdent.toJson()) {
					@Override
					void expect(Message<JsonObject> reply) {
						assertThat(reply.body().getObject("contents").getBoolean("exists"), is(false));
					}
				});
	}

	@Test
	public void testHasResourceNoProjectFail() {
		final ResourceAddress resourceIdent = new ResourceAddress();
		resourceIdent.setProjectId("my.project");
		resourceIdent.setPath("src/foo/bar/Missing.java");
		execute(createFooProject,
				new TestHandler(Messages.GET_RESOURCE, resourceIdent.toJson()) {
					@Override
					void expect(Message<JsonObject> reply) {
						assertThat(reply, instanceOf(ReplyFailureMessage.class));
						assertThat(reply.body(), instanceOf(ReplyException.class));
					}
				}, new TestHandler(Messages.HAS_RESOURCE, resourceIdent.toJson()) {
					@Override
					void expect(Message<JsonObject> reply) {
						assertThat(reply.body().getObject("contents").getBoolean("exists"), is(false));
					}
				});
	}

	@Test
	public void testNeedsUpdate() {
		// TODO
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
