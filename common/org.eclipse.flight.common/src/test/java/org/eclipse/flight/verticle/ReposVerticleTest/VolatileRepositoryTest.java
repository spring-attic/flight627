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
import static org.vertx.testtools.VertxAssert.*;

import org.apache.log4j.Logger;
import org.eclipse.flight.Ids;
import org.eclipse.flight.messages.RequestMessage;
import org.eclipse.flight.objects.FlightObject;
import org.eclipse.flight.objects.Project;
import org.eclipse.flight.objects.Resource;
import org.eclipse.flight.vertx.VertxManager;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.eventbus.impl.ReplyFailureMessage;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public class VolatileRepositoryTest extends TestVerticle {

	Logger logger = Logger.getLogger(VolatileRepositoryTest.class);
	
	static long TIME_OUT = 1000;

	class Harness {
		int processIndex;
	}

	abstract class TestHandler {
		private String action;
		private FlightObject message;

		TestHandler next;

		TestHandler(String action, FlightObject message) {
			this.action = action;
			this.message = message;
		}

		/**
		 * Something has happened, so handle it.
		 */
		abstract void expect(Message<JsonObject> reply);

		void execute() {
			logger.debug("test sending @" + Ids.RESOURCE_PROVIDER + " " + action);
			vertx.eventBus().send(Ids.RESOURCE_PROVIDER,
					new RequestMessage(0L, action, message).toJson(),
					new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> reply) {
							try {
								logger.debug("test_recieved @" + Ids.RESOURCE_PROVIDER + " " + action + "\n\t\t" + reply.body());
								expect(reply);
								if (next == null) {
									testComplete();
								} else {
									next.execute();
								}
							} catch (Exception e) {
								e.printStackTrace();
								fail(e.getMessage());
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
			index++;
		}
		handlers[0].execute();
	}

	Project fooProject = new Project();

	{
		fooProject.setName("foo");
	}

	TestHandler createFooProject = new TestHandler(Ids.CREATE_PROJECT, fooProject) {
		@Override
		void expect(Message<JsonObject> reply) {
			assertThat(reply.body().getObject("contents").getString("name"), is("foo"));
		}
	};

	@Test
	public void testGetProjectsEmpty() {
		new TestHandler(Ids.GET_ALL_PROJECTS, null) {
			@Override
			void expect(Message<JsonObject> reply) {
				JsonArray projects = reply.body().getObject("contents")
						.getArray("projects");
				assertThat(projects.size(), is(0));
			}
		}.execute();
	}

	@Test
	public void testCreateProjects() {
		execute(createFooProject, new TestHandler(Ids.GET_ALL_PROJECTS, null) {
			@Override
			void expect(Message<JsonObject> reply) {
				JsonArray projects = reply.body().getObject("contents")
						.getArray("projects");
				assertThat(projects.size(), is(1));
				assertThat(((JsonObject) projects.get(0)).getString("name"), is("foo"));
			}
		});
	}

	@Test
	public void testGetProject() {
		execute(createFooProject, new TestHandler(Ids.GET_PROJECT, fooProject) {
			@Override
			void expect(Message<JsonObject> reply) {
				assertThat(reply.body().getObject("contents").getString("name"),
						is("foo"));
			}
		});
	}

	@Test
	public void testCreateGetHasResource() {
		final Resource resource = new Resource();
		resource.setHash("12345678");
		resource.setPath("src/foo/bar/MyClass.java");
		resource.setTimestamp(System.currentTimeMillis());
		resource.setType("file");
		resource.setUserName("defaultUser");
		resource.setData("package foo.bar;...");
		resource.setProjectName("foo");

		final Resource resourceIdent = new Resource();
		resourceIdent.setProjectName("foo");
		resourceIdent.setPath("src/foo/bar/MyClass.java");

		execute(createFooProject, new TestHandler(Ids.CREATE_RESOURCE, resource) {
			@Override
			void expect(Message<JsonObject> reply) {
				JsonObject contents = reply.body().getObject("contents");
				assertThat(contents.getString("path"), is("src/foo/bar/MyClass.java"));
				assertThat(contents.getString("hash"), is("12345678"));
				assertThat(contents.getString("data"), is((String) null));
			}
		}, new TestHandler(Ids.GET_PROJECT, fooProject) {
			@Override
			void expect(Message<JsonObject> reply) {
				JsonObject contents = reply.body().getObject("contents");
				assertThat(contents.getString("name"), is("foo"));
				JsonArray array = contents.getArray("resources");
				assertThat(array.size(), is(1));
			}
		}, new TestHandler(Ids.GET_RESOURCE, resourceIdent) {
			@Override
			void expect(Message<JsonObject> reply) {
				JsonObject contents = reply.body().getObject("contents");
				assertThat(contents.getString("hash"), is("12345678"));
				assertThat(contents.getString("data"), is("package foo.bar;..."));
			}
		}, new TestHandler(Ids.HAS_RESOURCE, resourceIdent) {
			@Override
			void expect(Message<JsonObject> reply) {
				assertThat(reply.body().getBoolean("exists"), is(true));
				assertThat(reply.body().getObject("contents").getString("path"),
						is("src/foo/bar/MyClass.java"));
			}
		});
	}

	@Test
	public void testHasResourceFalse() {
		final Resource resourceIdent = new Resource();
		resourceIdent.setProjectName("foo");
		resourceIdent.setPath("src/foo/bar/Missing.java");
		execute(createFooProject, new TestHandler(Ids.GET_RESOURCE, resourceIdent) {
			@Override
			void expect(Message<JsonObject> reply) {
				assertThat(reply, instanceOf(ReplyFailureMessage.class));
			}
		}, new TestHandler(Ids.HAS_RESOURCE, resourceIdent) {
			@Override
			void expect(Message<JsonObject> reply) {
				assertThat(reply.body().getBoolean("exists"), is(false));
			}
		});
	}

	@Test
	public void testHasResourceNoProjectFail() {
		final Resource resourceIdent = new Resource();
		resourceIdent.setProjectName("my.project");
		resourceIdent.setPath("src/foo/bar/Missing.java");
		execute(createFooProject, new TestHandler(Ids.GET_RESOURCE, resourceIdent) {
			@Override
			void expect(Message<JsonObject> reply) {
				assertThat(reply, instanceOf(ReplyFailureMessage.class));
				assertThat(reply.body(), instanceOf(ReplyException.class));
			}
		}, new TestHandler(Ids.HAS_RESOURCE, resourceIdent) {
			@Override
			void expect(Message<JsonObject> reply) {
				assertThat(reply.body().getBoolean("exists"), is(false));
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
