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

var MessagesRepository = function(socket, repository) {
	this.repository = repository;
	this.socket = socket;
	
	socket.on('getProjectsRequest', function(data) {this.getProjects()});
	socket.on('getProjectRequest', function(data) {this.getProjects()});
	socket.on('getResourceRequest', function(data) {this.getProjects()});
};

exports.MessagesRepository = MessagesRepository;

MessagesRepository.prototype.getProjects = function(data) {
    this.repository.getProjects(function(error, result) {
		if (error == null) {
			socket.emit('getProjectsResponse', {
				'callback_id' : data.callback_id,
				'requestSenderID' : data.requestSenderID,
				'projects' : result});
		}
    });
}

MessagesRepository.prototype.getProject = function(data) {
    this.repository.getProject(data.project, function(error, result) {
		if (error == null) {
			socket.emit('getProjectResponse', {
				'callback_id' : data.callback_id,
				'requestSenderID' : data.requestSenderID,
				'project' : data.project,
				'content' : result});
		}
    });
}

MessagesRepository.prototype.getResource = function(data) {
	this.repository.getResource(data.project, data.resource, function(error, result) {
		if (error == null) {
			socket.emit('getResourceResponse', {
				'callback_id' : data.callback_id,
				'requestSenderID' : data.requestSenderID,
				'project' : data.project,
				'resource' : data.resource,
				'content' : result});
		}
	});
}
