var sys = require('sys')
var crypto = require('crypto');

ProjectProvider = function() {};
ProjectProvider.prototype.stored = {};
exports.ProjectProvider = ProjectProvider;

ProjectProvider.prototype.getProjects = function(callback) {
    callback(null, this.stored);
};

ProjectProvider.prototype.getProject = function(projectName, callback) {
	var project = this.stored[projectName];
	if (project !== undefined) {
	    callback(null, {'project': project.name});
	}
	else {
	    callback(404);
	}
};

ProjectProvider.prototype.createProject = function(projectName, callback) {
	if (this.stored[projectName] === undefined) {
		this.stored[projectName] = {'name' : projectName, 'resources' : {}};
	    callback(null, {'project': projectName});
	}
	else {
		callback(404);
	}
};

ProjectProvider.prototype.putResource = function(projectName, resourcePath, data, callback) {
	if (this.stored[projectName] !== undefined) {
		console.log('putResource ' + resourcePath);
		var project = this.stored[projectName];
		project.resources[resourcePath] = {'data' : data, 'version' : 0};
		
	    callback(null, {'project': projectName});
	}
	else {
		callback(404);
	}
};

ProjectProvider.prototype.updateResource = function(projectName, resourcePath, data, callback) {
	if (this.stored[projectName] !== undefined) {
		console.log('updateResource ' + resourcePath);
		var project = this.stored[projectName];
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
	if (this.stored[projectName] !== undefined) {
		console.log('updateMetadata ' + resourcePath);
		var project = this.stored[projectName];
		var resource = project.resources[resourcePath];
		
		if (resource !== undefined) {
			resource.metadata = metadata;
			
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
	if (this.stored[projectName] !== undefined) {
		console.log('getResource ' + resourcePath);
		var project = this.stored[projectName];
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
	if (this.stored[projectName] !== undefined) {
		console.log('deleteResource ' + resourcePath);
		var project = this.stored[projectName];
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
