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

package org.eclipse.flight.resources.vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.eclipse.flight.Constants;
import org.eclipse.flight.resources.FlightObject;
import org.eclipse.flight.resources.JsonWrapper;
import org.eclipse.flight.resources.NotificationMessage;
import org.eclipse.flight.resources.RequestMessage;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.spi.cluster.impl.hazelcast.HazelcastClusterManagerFactory;

/**
 * @author Miles Parker
 * 
 */
public class VertxManager {

	Logger logger = Logger.getLogger(VertxManager.class);
	
	private static VertxManager INSTANCE;

	class MultiHandler implements Handler<Message<JsonObject>> {
		String address;
		
		public MultiHandler(String address) {
			this.address = address;
		}
		
		Map<String, FlightHandler> handlerForAction = new HashMap<String, FlightHandler>();

		@Override
		public void handle(Message<JsonObject> event) {
			String action = event.body().getString("action");
			FlightHandler handler = handlerForAction.get(action);
			if (handler != null) {
				logger.debug("Handling " + handler);
				handler.handle(event);
			} else {
				logger.warn("No handler for @" + address + " " + action);
			}
		}
	}

	private Map<String, MultiHandler> multiHandlers = new ConcurrentHashMap<String, MultiHandler>();

	Vertx vertx;

	long id;

	public VertxManager(Vertx vertx, long id) {
		this.vertx = vertx;
		this.id = id;
	}

	public VertxManager(Vertx vertx) {
		this.vertx = vertx;
		this.id = (new Random()).nextLong();
	}

	private VertxManager() {
		id = (new Random()).nextLong();
		System.setProperty("vertx.clusterManagerFactory", HazelcastClusterManagerFactory.class.getCanonicalName());
		VertxFactory.newVertx(Constants.PORT, Constants.HOST,
				new Handler<AsyncResult<Vertx>>() {
					@Override
					public void handle(AsyncResult<Vertx> event) {
						vertx = event.result();
					}
				});
		while (vertx == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {

			}
		}
	}

	/**
	 * Blocks!!!
	 * 
	 * @return
	 */
	public static VertxManager get() {
		if (INSTANCE == null) {
			INSTANCE = new VertxManager();
			INSTANCE.start();
		}
		return INSTANCE;
	}

	public void start() {
		logger.info("Starting vertx manager...");

		HttpServer httpServer = vertx.createHttpServer();
		JsonArray permitted = new JsonArray();
		permitted.add(new JsonObject());

		JsonObject config = new JsonObject().putString("prefix", "/eventbus");
		vertx.createSockJSServer(httpServer).bridge(config, permitted, permitted);

		httpServer.listen(3001, "localhost");
		logger.info("Vertx manager started");
	}

	public void register(FlightHandler handler) {
		MultiHandler multiHandler = multiHandlers.get(handler.getAddress());
		if (multiHandler == null) {
			multiHandler = new MultiHandler(handler.getAddress());
			vertx.eventBus().registerHandler(handler.getAddress(), multiHandler);
			multiHandlers.put(handler.getAddress(), multiHandler);
		}
		handler.setId(id);
		multiHandler.handlerForAction.put(handler.getAction(), handler);
	}

	public void send(String address, String action, FlightObject object) {
		vertx.eventBus().send(address,
				new RequestMessage(id, action, object).toJson(true));
	}

	public void publish(String address, String action, FlightObject object) {
		logger.debug("Publishing @" + address + " " + action + "\n\t\t" + object);
		vertx.eventBus().publish(
				Constants.EDIT_PARTICIPANT,
				new NotificationMessage(id, Constants.LIVE_RESOURCE_CHANGED, object)
						.toJson());
	}

	public void publish(String address, String action, JsonObject json) {
		publish(address, action, new JsonWrapper(json));
	}

	public void request(final String address, final String action, FlightObject object,
			final Requester requester) {
		logger.debug("Requesting @" + address + " " + action + "\n\t\t" + object);
		vertx.eventBus().send(address,
				new RequestMessage(id, action, object).toJson(true),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> reply) {
						logger.debug("Accepting @" + address + " " + action + "\n\t\t" + reply.body());
						requester.accept(reply.body().getObject("contents"));
					}
				});
	}

	/**
	 * @return the vertx
	 */
	public Vertx getVertx() {
		return vertx;
	}
}
