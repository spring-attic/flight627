/*******************************************************************************
 * @license
 * Copyright (c) 2013 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
/*global require console exports process __dirname*/

// create and configure express
var express = require('express');
var app = express();

app.use("/client", express.static(__dirname + '/web-client'));

var host = process.env.VCAP_APP_HOST || 'localhost';
var port = process.env.VCAP_APP_PORT || '3000';

var server = app.listen(port, host);
console.log('Express server started on port ' + port);

// create and configure socket.io
var io = require('socket.io').listen(server);
io.set('transports', ['websocket']);

// create and configure services
var MessageCore = require('./messages-core.js').MessageCore;
var messageSync = new MessageCore();

io.sockets.on('connection', function (socket) {
	messageSync.initialize(socket, io.sockets);
});

var client_io = require('socket.io-client');
var client_socket = client_io.connect('localhost', {
	port : 3000
});

var Repository = require('./repository-inmemory.js').Repository;
var repository = new Repository();

var RestRepository = require('./repository-rest-api.js').RestRepository;
var restrepository = new RestRepository(app, repository);

var MessagesRepository = require('./repository-message-api.js').MessagesRepository;
var messagesrepository = new MessagesRepository(repository);

client_socket.on('connect', function() {
	console.log('client socket connected');
	
	repository.setNotificationSender.call(repository, client_socket);
	messagesrepository.setSocket.call(messagesrepository, client_socket);
});
