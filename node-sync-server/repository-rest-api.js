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

var RestRepository = function(expressapp, repository) {
	that = this;

	this.repository = repository;
	
	expressapp.get('/api', this.getProjects);

	expressapp.get('/api/:project', this.getProject);
	expressapp.post('/api/:project', this.createProject);

	expressapp.get('/api/:project/:resource(*)', this.getResource);
	expressapp.put('/api/:project/:resource(*)', this.putResource);
	expressapp.post('/api/:project/:resource(*)', this.postResource);
};

RestRepository.prototype.projectsStorage = {};
exports.RestRepository = RestRepository;

RestRepository.prototype.getProjects = function(req, res) {
    that.repository.getProjects(function(error, result) {
        res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
    });
}

RestRepository.prototype.getProject = function(req, res) {
	var includeDeleted = req.query.includeDeleted;
	
    that.repository.getProject(req.params.project, includeDeleted, function(error, content, deleted) {
		if (error == null) {
			if (includeDeleted) {
	        	res.send(JSON.stringify({
	        		'content' : content,
					'deleted' : deleted
				}), { 'Content-Type': 'application/json' }, 200);
			}
			else {
	        	res.send(JSON.stringify({
	        		'content' : content
				}), { 'Content-Type': 'application/json' }, 200);
			}
		}
		else {
			res.send(error);
		}
    });
}

RestRepository.prototype.createProject = function(req, res) {
    that.repository.createProject(req.params.project, function(error, result) {
		if (error == null) {
        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
		}
		else {
			res.send(error);
		}
    });
}

RestRepository.prototype.postResource = function(req, res) {
	var body = '';
	req.on('data', function(buffer) {
		console.log("Chunk:", buffer.length );
		body += buffer;
	});

	req.on('end', function() {
	    that.repository.createResource(req.params.project, req.params.resource, body, req.headers['resource-sha1'],
				req.headers['resource-timestamp'], req.headers['resource-type'], function(error, result) {

			if (error == null) {
	        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
			}
			else {
				res.send(error);
			}
	    });
	});
}

RestRepository.prototype.putResource = function(req, res) {
	var body = '';
	req.on('data', function(buffer) {
		console.log("Chunk:", buffer.length );
		body += buffer;
	});

	req.on('end', function() {
		if (req.param('meta') !== undefined) {
			var metadata = JSON.parse(body);
			var type = req.param('meta');
		    that.repository.updateMetadata(req.params.project, req.params.resource, metadata, type, function(error, result) {
				if (error == null) {
		        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
				}
				else {
					res.send(error);
				}
		    });
		}
		else {
		    that.repository.updateResource(req.params.project, req.params.resource, body, req.headers['resource-sha1'],
					req.headers['resource-timestamp'], function(error, result) {
				if (error == null) {
		        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
				}
				else {
					res.send(error);
				}
		    });
		}

	});

	req.on('error', function(error) {
		console.log('Error: ' + error);
	});

}

RestRepository.prototype.getResource = function(req, res) {
    that.repository.getResource(req.params.project, req.params.resource, undefined, undefined, function(error, result) {
		if (error == null) {
        	res.send(result, 200);
		}
		else {
			res.send(error);
		}
    });
}
