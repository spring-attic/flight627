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

MessageCore = function() {};
exports.MessageCore = MessageCore;

MessageCore.prototype.initialize = function(socket, sockets) {
	console.log('client connected for update notifications');
	
	this.configureBroadcast(socket, 'projectConnected');
	this.configureBroadcast(socket, 'projectDisconnected');

	this.configureBroadcast(socket, 'resourceCreated');
	this.configureBroadcast(socket, 'resourceChanged');
	this.configureBroadcast(socket, 'resourceDeleted');
	
	this.configureBroadcast(socket, 'metadataChanged');
	
	this.configureRequest(socket, 'getProjectsRequest');
	this.configureRequest(socket, 'getProjectRequest');
	this.configureRequest(socket, 'getResourceRequest');
	this.configureRequest(socket, 'getMetadataRequest');
	
	this.configureResponse(socket, sockets, 'getProjectsResponse');
	this.configureResponse(socket, sockets, 'getProjectResponse');
	this.configureResponse(socket, sockets, 'getResourceResponse');
	this.configureResponse(socket, sockets, 'getMetadataResponse');

	this.configureBroadcast(socket, 'startedediting');
	this.configureBroadcast(socket, 'modelchanged');
	this.configureBroadcast(socket, 'livemetadata');
	
	this.configureRequest(socket, 'contentassistrequest');
	this.configureResponse(socket, sockets, 'contentassistresponse');
	
	this.configureRequest(socket, 'navigationrequest');
	this.configureResponse(socket, sockets, 'navigationresponse');

	this.configureRequest(socket, 'renameinfilerequest');
	this.configureResponse(socket, sockets, 'renameinfileresponse');

	socket.on('disconnect', function () {
		console.log('client disconnected from update notifications');
	});
}

MessageCore.prototype.configureBroadcast = function(socket, messageName) {
	socket.on(messageName, function(data) {
		console.log(messageName + data);
		socket.broadcast.emit(messageName, data);
	});
}

MessageCore.prototype.configureRequest = function(socket, messageName) {
	socket.on(messageName, function(data) {
		console.log(messageName + data);
		data.requestSenderID = socket.id;
		socket.broadcast.emit(messageName, data);
	});
}

MessageCore.prototype.configureResponse = function(socket, sockets, messageName) {
	socket.on(messageName, function(data) {
		console.log(messageName + data);
		
		var requester = sockets.sockets[data.requestSenderID];
		if (requester !== undefined) {
			requester.emit(messageName, data);
		}
		else {
			sockets.clients().forEach(function(client) {
				if (client.id === data.requestSenderID) {
					client.emit(messageName, data);
				}
			});
		}
	});
}
