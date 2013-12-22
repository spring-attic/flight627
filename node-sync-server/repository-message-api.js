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

var MessagesRepository = function(repository) {
	that = this;
	this.repository = repository;
};

exports.MessagesRepository = MessagesRepository;

MessagesRepository.prototype.setSocket = function(socket) {
	that.socket = socket;
	
	socket.on('getProjectsRequest', that.getProjects);
	socket.on('getProjectRequest', that.getProject);
	socket.on('getResourceRequest', that.getResource);
	
	socket.on('getProjectResponse', that.getProjectResponse);
	socket.on('getResourceResponse', that.getResourceResponse);
	
	socket.on('projectConnected', that.projectConnected);
	socket.on('projectDisconnected', that.projectDisconnected);
	
	socket.on('resourceChanged', that.resourceChanged);
	socket.on('resourceCreated', that.resourceCreated);
	socket.on('resourceDeleted', that.resourceDeleted);
	
}

MessagesRepository.prototype.getProjects = function(data) {
    that.repository.getProjects(data.username, function(error, result) {
		if (error == null) {
			that.socket.emit('getProjectsResponse', {
				'callback_id' : data.callback_id,
				'requestSenderID' : data.requestSenderID,
				'username' : data.username,
				'projects' : result});
		}
    });
}

MessagesRepository.prototype.getProject = function(data) {
    that.repository.getProject(data.username, data.project, data.includeDeleted, function(error, resources, deleted) {
		if (error == null) {
			if (data.includeDeleted) {
				that.socket.emit('getProjectResponse', {
					'callback_id' : data.callback_id,
					'requestSenderID' : data.requestSenderID,
					'username' : data.username,
					'project' : data.project,
					'files' : resources,
					'deleted' : deleted});
			}
			else {
				that.socket.emit('getProjectResponse', {
					'callback_id' : data.callback_id,
					'requestSenderID' : data.requestSenderID,
					'username' : data.username,
					'project' : data.project,
					'files' : resources});
			}
		}
    });
}

MessagesRepository.prototype.getResource = function(data) {
	that.repository.getResource(data.username, data.project, data.resource, data.timestamp, data.hash, function(error, content, timestamp, hash) {
		if (error == null) {
			that.socket.emit('getResourceResponse', {
				'callback_id' : data.callback_id,
				'requestSenderID' : data.requestSenderID,
				'username' : data.username,
				'project' : data.project,
				'resource' : data.resource,
				'timestamp' : timestamp,
				'hash' : hash,
				'content' : content});
		}
	});
}

MessagesRepository.prototype.projectConnected = function(data) {
	var projectName = data.project;
	var username = data.username;
	if (!that.repository.hasProject(username, projectName)) {
		that.repository.createProject(username, projectName, function(error, result) {});
	}
	
	that.socket.emit('getProjectRequest', {
		'callback_id' : 0,
		'username' : username,
		'project' : projectName,
		'includeDeleted' : true
	});
}

MessagesRepository.prototype.projectDisconnected = function(data) {	
}

MessagesRepository.prototype.getProjectResponse = function(data) {
	var projectName = data.project;
	var username = data.username;
	var files = data.files;
	var deleted = data.deleted;
	
	if (that.repository.hasProject(username, projectName)) {
		for (i = 0; i < files.length; i += 1) {
			var resource = files[i].path;
			var type = files[i].type;
			var timestamp = files[i].timestamp;
			var hash = files[i].hash;
			
			var newResource = !that.repository.hasResource(username, projectName, resource, type) && !that.repository.gotDeleted(username, projectName, resource, timestamp);
			var updatedResource = that.repository.needsUpdate(username, projectName, resource, type, timestamp, hash);

			if (newResource || updatedResource) {
				that.socket.emit('getResourceRequest', {
					'callback_id' : 0,
					'username' : username,
					'project' : projectName,
					'resource' : resource,
					'timestamp' : timestamp,
					'hash' : hash
				});
			}
		}
		
		if (deleted !== undefined) {
			for (i = 0; i < deleted.length; i += 1) {
				var resource = deleted[i].path;
				var deletedTimestamp = deleted[i].timestamp;
			
				that.repository.deleteResource(username, projectName, resource, deletedTimestamp, function(error, result) {
					if (error !== null) {
						console.log('did not delete resource: ' + projectName + "/" + resource + " - deleted at: " + deletedTimestamp);
					}	
				});
			}
		}
	}
}

MessagesRepository.prototype.getResourceResponse = function(data) {
	var username = data.username;
	var projectName = data.project;
	var resource = data.resource;
	var type = data.type;
	var timestamp = data.timestamp;
	var hash = data.hash;
	var content = data.content;
	
	if (!that.repository.hasResource(username, projectName, resource, type)) {
		that.repository.createResource(username, projectName, resource, content, hash, timestamp, type, function(error, result) {
			if (error !== null) {
				console.log('Error creating repository resource: ' + projectName + "/" + resource + " - " + data.timestamp);
			}
		});
	}
	else {
		that.repository.updateResource(username, projectName, resource, content, hash, timestamp, function(error, result) {
			if (error !== null) {
				console.log('Error updating repository resource: ' + projectName + "/" + resource + " - " + timestamp);
			}
		});
	}
}

MessagesRepository.prototype.resourceChanged = function(data) {
	var username = data.username;
	var projectName = data.project;
	var resource = data.resource;
	var timestamp = data.timestamp;
	var hash = data.hash;
	var type = "file";
	
	if (!that.repository.hasResource(username, projectName, resource, type) || that.repository.needsUpdate(projectName, resource, type, timestamp, hash)) {
		that.socket.emit('getResourceRequest', {
			'callback_id' : 0,
			'username' : username,
			'project' : projectName,
			'resource' : resource,
			'timestamp' : timestamp,
			'hash' : hash
		});
	}
	
}

MessagesRepository.prototype.resourceCreated = function(data) {
	var username = data.username;
	var projectName = data.project;
	var resource = data.resource;
	var timestamp = data.timestamp;
	var hash = data.hash;
	var type = data.type;
	
	if (!that.repository.hasResource(username, projectName, resource, type)) {
		that.socket.emit('getResourceRequest', {
			'callback_id' : 0,
			'username' : username,
			'project' : projectName,
			'resource' : resource,
			'timestamp' : timestamp,
			'hash' : hash
		});
	}
	
}

MessagesRepository.prototype.resourceDeleted = function(data) {
	var username = data.username;
	var projectName = data.project;
	var resource = data.resource;
	var timestamp = data.timestamp;
	
	if (that.repository.hasResource(username, projectName, resource)) {
		that.repository.deleteResource(username, projectName, resource, timestamp, function(error, result) {
			if (error !== null) {
				console.log('Error deleting repository resource: ' + projectName + "/" + resource + " - " + timestamp);
			}
		})
	}
}
