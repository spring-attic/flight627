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

package org.eclipse.flight.vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.flight.Configuration;
import org.eclipse.flight.messages.NotificationMessage;
import org.eclipse.flight.messages.RequestMessage;
import org.eclipse.flight.objects.FlightObject;
import org.eclipse.flight.objects.JsonWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 * 
 */
public class VertxManager {

	Logger logger = LoggerFactory.getLogger(VertxManager.class);

	private static VertxManager INSTANCE;

	class MultiHandler implements Handler<Message<JsonObject>> {
		String address;

		public MultiHandler(String address) {
			this.address = address;
		}

		Map<String, FlightHandler> handlerForAction = new HashMap<String, FlightHandler>();

		@Override
		public void handle(Message<JsonObject> message) {
			JsonObject body = message.body();
			Long senderId = body.getLong("senderId");
			if (senderId.equals(id)) {
				return;// Don't send back to ourselves
			}
			String action = message.body().getString("action");
			FlightHandler handler = handlerForAction.get(action);
			if (handler != null) {
				logger.debug("Handling " + handler);
				handler.handle(message);
			} else {
				logger.warn("No handler for @" + address + " " + action);
			}
		}
	}

	private Map<String, MultiHandler> multiHandlers = new ConcurrentHashMap<String, MultiHandler>();

	Vertx vertx;

	Long id;

	public VertxManager(Vertx vertx, Long id) {
		this.vertx = vertx;
		this.id = id;
	}

	public VertxManager(Vertx vertx) {
		this.vertx = vertx;
		this.id = (new Random()).nextLong();
	}

	private VertxManager() {
		id = (new Random()).nextLong();
		System.setProperty("vertx.clusterManagerFactory",
				"org.vertx.java.spi.cluster.impl.hazelcast.HazelcastClusterManagerFactory");
		VertxFactory.newVertx(Configuration.getEventBusPort(), Configuration.getHost(),
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
		logger.info("Starting vertx bridge server...");

		HttpServer bridgeServer = vertx.createHttpServer();
		JsonArray permitted = new JsonArray();
		permitted.add(new JsonObject());

		JsonObject config = new JsonObject().putString("prefix", "/eventbus");
		vertx.createSockJSServer(bridgeServer).bridge(config, permitted, permitted);

		bridgeServer.listen(Configuration.getEventBusBridgePort(),
				Configuration.getHost());
		logger.info("Vertx bridge server started");
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
		logger.debug("Registered Handler: " + handler);
	}

	public void send(String address, String action, FlightObject object) {
		logger.debug("Sending: " + action + "@" + address + "\n\t\t" + object);
		vertx.eventBus().send(address,
				new RequestMessage(id, action, object).toJson(true));
	}

	public void publish(String address, String action, FlightObject object) {
		logger.debug("Publishing: " + action + "@" + address + "\n\t\t" + object);
		vertx.eventBus().publish(address,
				new NotificationMessage(id, action, object).toJson());
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
						JsonObject contents = reply.body().getObject("contents");
						logger.debug("Accepting: " + action + "@" + address + "\n\t\t"
								+ contents);
						FlightObject flightObject = FlightObject.createFromJson(contents);
						requester.accept(flightObject);
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
