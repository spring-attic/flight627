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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.flight.core.internal.vertx.EclipseVertx;
import org.eclipse.flight.messages.Messages;
import org.eclipse.flight.resources.MessageObject;
import org.eclipse.flight.resources.Project;
import org.eclipse.flight.resources.Request;
import org.eclipse.flight.resources.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 * 
 */
public class EclipseRepositoryTest {

	IStatus done;
	private IProject project;
	private EclipseRepository repository;

	abstract class TestHandler {
		private String address;
		private MessageObject message;

		TestHandler next;

		TestHandler(String address, MessageObject message) {
			this.address = address;
			this.message = message;
		}

		/**
		 * Something has happened, so handle it.
		 */
		abstract void expect(Message<JsonObject> reply);

		void execute() throws InterruptedException {
			done = null;
			EclipseVertx
					.get()
					.eventBus()
					.send(Messages.RESOURCE_PROVIDER,
							new Request(address, message).toJson(),
							new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> reply) {
									try {
										expect(reply);
									} catch (AssertionError e) {
										done = new Status(IStatus.ERROR,
												"org.eclipse.flight.core.tests", e
														.getMessage(), e);
										return;
									}
									done = Status.OK_STATUS;
								}
							});
			waitForDone();
		}

		/**
		 * @param handler
		 *            the next to set
		 */
		public void setNext(TestHandler handler) {
			this.next = handler;
		}
	}

	public void execute(TestHandler... handlers) throws InterruptedException {
		int index = 0;
		for (TestHandler testHandler : handlers) {
			if (index < handlers.length - 1) {
				testHandler.setNext(handlers[index + 1]);
			}
			testHandler.execute();
			index++;
		}
	}

	@Before
	public void setup() throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject("test.project");
		project.create(null);
		project.open(null);
		IFolder folder = project.getFolder("src");
		folder.create(true, true, null);
		IFolder folder2 = project.getFolder("src/org");
		folder2.create(true, true, null);
		IFolder folder3 = project.getFolder("src/org/blah");
		folder3.create(true, true, null);
		project.getFile("src/org/blah/Foo.java").create(
				new ByteArrayInputStream("class Foo".getBytes()), true, null);
		project.getFile("src/org/blah/Bar.java").create(
				new ByteArrayInputStream("class Bar".getBytes()), true, null);
		repository = Activator.getDefault().getRepository();
	}

	@After
	public void tearDown() throws CoreException {
		System.err.println("Delete");
		System.err.println(project.exists());
		project.delete(true, null);
		System.err.println(project.exists());
		repository.removeProject(project);
	}

	@Test
	public void testGetProjectsNone() throws InterruptedException {
		execute(new TestHandler(Messages.GET_ALL_PROJECTS, null) {
			@Override
			public void expect(Message<JsonObject> reply) {
				JsonArray array = reply.body().getObject("contents").getArray("projects");
				assertThat(array.size(), is(0));
				System.err.println(array);
			}
		});
	}

	@Test
	public void testGetProjectsOne() throws InterruptedException {
		repository.addProject(project);
		execute(new TestHandler(Messages.GET_ALL_PROJECTS, null) {
			@Override
			public void expect(Message<JsonObject> reply) {
				JsonArray array = reply.body().getObject("contents").getArray("projects");
				assertThat(array.size(), is(1));
			}
		});
	}

	private void waitForDone() throws InterruptedException {
		while (done == null) {
			Thread.sleep(10);
		}
		if (!done.isOK()) {
			done.getException().printStackTrace();
			fail(done.getException().getMessage());
		}
		done = null;
	}

	@Test
	public void testGetProject() throws InterruptedException, CoreException {
		repository.addProject(project);
		FlightProject flightProject = (FlightProject) repository.getProject(project);
		flightProject.updateResources();
		Project searchProject = new Project();
		searchProject.setName("test.project");
		searchProject.setUserName("defaultUser");
		execute(new TestHandler(Messages.GET_PROJECT, searchProject) {
			public void expect(Message<JsonObject> reply) {
				JsonArray array = reply.body().getObject("contents")
						.getArray("resources");
				assertThat(array, notNullValue());
				Set<String> res = new HashSet<String>();
				for (Object object : array) {
					res.add(((JsonObject) object).getString("path"));
				}
				assertThat(res.contains("src/org/blah/Foo.java"), is(true));
				assertThat(res.contains("src/org/blah/Bar.java"), is(true));
				assertThat(res.contains(".project"), is(true));
				assertThat(res.contains("src/org"), is(true));
			}
		});
	}

	@Test
	public void testGetResource() throws InterruptedException, CoreException {
		repository.addProject(project);
		FlightProject flightProject = (FlightProject) repository.getProject(project);
		flightProject.updateResources();
		Project searchProject = new Project();
		searchProject.setName("test.project");
		searchProject.setUserName("defaultUser");
		Resource searchResource = new Resource();
		searchResource.setPath("src/org/blah/Foo.java");
		searchResource.setProject(searchProject);
		execute(new TestHandler(Messages.GET_RESOURCE, searchResource) {
			public void expect(Message<JsonObject> reply) {
				JsonObject contents = reply.body().getObject("contents");
				assertThat(contents.getString("path"), is("src/org/blah/Foo.java"));
				assertThat(contents.getString("data"), is("class Foo"));
			}
		});
	}
}
