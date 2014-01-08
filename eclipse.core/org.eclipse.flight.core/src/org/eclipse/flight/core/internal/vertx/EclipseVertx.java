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

package org.eclipse.flight.core.internal.vertx;

import org.eclipse.flight.Constants;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 * 
 */
public class EclipseVertx {
	private static Vertx INSTANCE;

	public static Vertx get() {
		if (INSTANCE == null) {

			VertxFactory.newVertx(Constants.PORT, Constants.HOST,
					new Handler<AsyncResult<Vertx>>() {
						@Override
						public void handle(AsyncResult<Vertx> event) {
							INSTANCE = event.result();
						}
					});
			while (INSTANCE == null) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {

				}
			}
			HttpServer httpServer = INSTANCE.createHttpServer();
			JsonArray permitted = new JsonArray();
			permitted.add(new JsonObject());

			JsonObject config = new JsonObject().putString("prefix", "/eventbus");
			INSTANCE.createSockJSServer(httpServer).bridge(config, permitted, permitted);

			httpServer.listen(3001, "localhost");
		}
		return INSTANCE;
	}
}
