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

package org.eclipse.flight.server.editor;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;

/**
 * @author Miles Parker
 * 
 */
public class ServerVerticle extends Verticle {
	@Override
	public void start() {
		HttpServer fileServer = vertx.createHttpServer();
		fileServer.requestHandler(new Handler<HttpServerRequest>() {
		    public void handle(HttpServerRequest req) {
		        String file = "";
		        if (req.path().equals("/")) {
		          file = "index.html";
		        } else if (!req.path().contains("..")) {
		          file = req.path();
		        }
		        System.err.println("request " + file);
		        req.response().sendFile("web-client/" + file);      		    }
		});
		fileServer.listen(3000, "localhost");
		
//		HttpServer bridgeServer = vertx.createHttpServer();
//		JsonArray permitted = new JsonArray();
//		permitted.add(new JsonObject());
//		JsonObject config = new JsonObject().putString("prefix", "/eventbus");
//		vertx.createSockJSServer(bridgeServer).bridge(config, permitted, permitted);
//		bridgeServer.listen(6270, "localhost");
	}
}
