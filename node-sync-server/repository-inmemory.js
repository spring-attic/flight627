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

var sys = require('sys')
var crypto = require('crypto');

InMemoryRepository = function(notificationSender) {
	this.notificationSender = notificationSender;
};

InMemoryRepository.prototype.projectsStorage = {};
exports.Repository = InMemoryRepository;

InMemoryRepository.prototype.getProjects = function(callback) {
	var projects = [];
	var i = 0;
	for (projectName in this.projectsStorage) {
		if (typeof this.projectsStorage[projectName] !== 'function') {
			project = {};
			project[projectName] = '/api/' + projectName;
			projects[i++] = project;
		}
	}

    callback(null, projects);
};

InMemoryRepository.prototype.getProject = function(projectName, callback) {
	var project = this.projectsStorage[projectName];
	if (project !== undefined) {
		var result = {};
		result.name = project.name;
		result.files = [];

		var i = 0;
		for (resourcePath in project.resources) {
			if (typeof project.resources[resourcePath] !== 'function') {
				var resourceDescription = {};
				resourceDescription.path = resourcePath;
				resourceDescription.version = project.resources[resourcePath].version;
				resourceDescription.type = project.resources[resourcePath].type;
				resourceDescription.uri = '/api/' + projectName + '/' + resourcePath;

				if (resourceDescription.type == 'file') {
					resourceDescription.edit = '/client/html/editor.html#' + projectName + '/' + resourcePath;
				}

				result.files[i++] = resourceDescription;
			}
		}

	    callback(null, result);
	}
	else {
	    callback(404);
	}
};

InMemoryRepository.prototype.createProject = function(projectName, callback) {
	if (this.projectsStorage[projectName] === undefined) {
		this.projectsStorage[projectName] = {'name' : projectName, 'resources' : {}};
	    callback(null, {'project': projectName});
	
		this.notificationSender.emit('projectCreated', { 'project' : projectName});
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.createResource = function(projectName, resourcePath, data, type, callback) {
	if (this.projectsStorage[projectName] !== undefined) {
		console.log('putResource ' + resourcePath);
		var project = this.projectsStorage[projectName];
		project.resources[resourcePath] = {'data' : data, 'type' : type, 'version' : 0, 'metadata' : {}};

	    callback(null, {'project': projectName});

		this.notificationSender.emit('resourceCreated', { 'project' : projectName,
														'resource' : resourcePath});
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.updateResource = function(projectName, resourcePath, data, callback) {
	if (this.projectsStorage[projectName] !== undefined) {
		console.log('updateResource ' + resourcePath);
		var project = this.projectsStorage[projectName];
		var resource = project.resources[resourcePath];

		if (resource !== undefined) {
			resource.data = data;
			resource.version = resource.version + 1;

			var fingerprint = crypto.createHash('md5').update(data).digest("hex");

		    callback(null, {'project' : projectName,
							'newversion' : resource.version,
							'fingerprint' : fingerprint
							});
							
			this.notificationSender.emit('resourceChanged', { 'project' : projectName,
												'resource' : resourcePath,
												'newversion' : resource.version,
												'fingerprint' : fingerprint});
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.updateMetadata = function(projectName, resourcePath, metadata, type, callback) {
	if (this.projectsStorage[projectName] !== undefined) {
		console.log('updateMetadata ' + resourcePath);
		var project = this.projectsStorage[projectName];
		var resource = project.resources[resourcePath];

		if (resource !== undefined) {
			resource.metadata[type] = metadata;

		    callback(null, {'project' : projectName
							});
							
			var metadataMessage = {
				'project' : projectName,
				'resource' : resourcePath,
				'type' : type,
				'metadata' : metadata
			};
			this.notificationSender.emit('metadataChanged', metadataMessage);
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.getResource = function(projectName, resourcePath, callback) {
	if (this.projectsStorage[projectName] !== undefined) {
		console.log('getResource ' + resourcePath);
		var project = this.projectsStorage[projectName];
		var resource = project.resources[resourcePath];

		if (resource !== undefined) {
			callback(null, resource.data);
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.deleteResource = function(projectName, resourcePath, callback) {
	if (this.projectsStorage[projectName] !== undefined) {
		console.log('deleteResource ' + resourcePath);
		var project = this.projectsStorage[projectName];
		var resource = project.resources[resourcePath];

		if (resource !== undefined) {
			project.resources[resourcePath] = undefined;
			callback(null, {});
			
			this.notificationSender.emit('resourceDeleted', { 'project' : projectName,
															'resource' : resourcePath});
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};
