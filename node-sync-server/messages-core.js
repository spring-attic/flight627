/*******************************************************************************
 * @license
 *  Copyright (c) 2013 GoPivotal, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      Martin Lippert (GoPivotal, Inc.) - initial API and implementation
 *******************************************************************************/

MessageCore = function() {};
exports.MessageCore = MessageCore;

MessageCore.prototype.initialize = function(socket, sockets) {
	console.log('client connected for update notifications');
	
	this.configureBroadcast(socket, 'startedediting');
	this.configureBroadcast(socket, 'modelchanged');
	this.configureBroadcast(socket, 'livemetadata');
	this.configureRequest(socket, 'contentassistrequest');
	this.configureResponse(socket, sockets, 'contentassistresponse');

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
