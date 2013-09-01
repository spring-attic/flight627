var sys = require('sys')
var crypto = require('crypto');

ProjectProvider = function() {};
ProjectProvider.prototype.projectsStorage = {};
exports.ProjectProvider = ProjectProvider;

ProjectProvider.prototype.getProjects = function(callback) {
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

ProjectProvider.prototype.getProject = function(projectName, callback) {
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

ProjectProvider.prototype.createProject = function(projectName, callback) {
	if (this.projectsStorage[projectName] === undefined) {
		this.projectsStorage[projectName] = {'name' : projectName, 'resources' : {}};
	    callback(null, {'project': projectName});
	}
	else {
		callback(404);
	}
};

ProjectProvider.prototype.createResource = function(projectName, resourcePath, data, type, callback) {
	if (this.projectsStorage[projectName] !== undefined) {
		console.log('putResource ' + resourcePath);
		var project = this.projectsStorage[projectName];
		project.resources[resourcePath] = {'data' : data, 'type' : type, 'version' : 0, 'metadata' : {}};
		
	    callback(null, {'project': projectName});
	}
	else {
		callback(404);
	}
};

ProjectProvider.prototype.updateResource = function(projectName, resourcePath, data, callback) {
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
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};

ProjectProvider.prototype.updateMetadata = function(projectName, resourcePath, metadata, type, callback) {
	if (this.projectsStorage[projectName] !== undefined) {
		console.log('updateMetadata ' + resourcePath);
		var project = this.projectsStorage[projectName];
		var resource = project.resources[resourcePath];
		
		if (resource !== undefined) {
			resource.metadata[type] = metadata;
			
		    callback(null, {'project' : projectName
							});
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};

ProjectProvider.prototype.getResource = function(projectName, resourcePath, callback) {
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

ProjectProvider.prototype.deleteResource = function(projectName, resourcePath, callback) {
	if (this.projectsStorage[projectName] !== undefined) {
		console.log('deleteResource ' + resourcePath);
		var project = this.projectsStorage[projectName];
		var resource = project.resources[resourcePath];
		
		if (resource !== undefined) {
			project.resources[resourcePath] = undefined;
			callback(null, {});
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};
