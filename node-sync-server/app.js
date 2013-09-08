
var express = require('express')
  , app = express()
  , server = app.listen(3000)
  , io = require('socket.io').listen(server);

var ProjectProvider = require('./project-provider').ProjectProvider;
var projectProvider = new ProjectProvider();

var getProjects = function(req, res) {
    projectProvider.getProjects(function(error, result) {
        res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
    });
}

var getProject = function(req, res) {
    projectProvider.getProject(req.params.project, function(error, result) {
		if (error == null) {
        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
		}
		else {
			res.send(error);
		}
    });
}

var createProject = function(req, res) {
    projectProvider.createProject(req.params.project, function(error, result) {
		if (error == null) {
        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
		}
		else {
			res.send(error);
		}
    });
}

var postResource = function(req, res) {
	var body = '';
	req.on('data', function(buffer) {
		console.log("Chunk:", buffer.length );
		body += buffer;
	});
	
	req.on('end', function() {
	    projectProvider.createResource(req.params.project, req.params.resource, body, req.headers['resource-type'], function(error, result) {
			if (error == null) {
	        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
			}
			else {
				res.send(error);
			}
	    });
	});
}

var putResource = function(req, res) {
	var body = '';
	req.on('data', function(buffer) {
		console.log("Chunk:", buffer.length );
		body += buffer;
	});
	
	req.on('end', function() {
		if (req.param('meta') !== undefined) {
			var metadata = JSON.parse(body);
			var type = req.param('meta');
		    projectProvider.updateMetadata(req.params.project, req.params.resource, metadata, type, function(error, result) {
				if (error == null) {
		        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);

					var metadataMessage = {
						'project' : req.params.project,
						'resource' : req.params.resource,
						'type' : type,
						'metadata' : metadata
					};
					io.sockets.emit('metadataupdate', metadataMessage);
				}
				else {
					res.send(error);
				}
		    });
		}
		else {
		    projectProvider.updateResource(req.params.project, req.params.resource, body, function(error, result) {
				if (error == null) {
		        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);

					io.sockets.emit('resourceupdate', { 'project' : req.params.project,
														'resource' : req.params.resource,
														'newversion' : result.newversion,
														'fingerprint' : result.fingerprint});
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

var getResource = function(req, res) {
    projectProvider.getResource(req.params.project, req.params.resource, function(error, result) {
		if (error == null) {
        	res.send(result, 200);
		}
		else {
			res.send(error);
		}
    });
}

// app.use(express.methodOverride());

app.get('/api', getProjects);

app.get('/api/:project', getProject);
app.post('/api/:project', createProject);

app.get('/api/:project/:resource(*)', getResource);
app.put('/api/:project/:resource(*)', putResource);
app.post('/api/:project/:resource(*)', postResource);

app.use("/client", express.static(__dirname + '/web-client'));

// server.listen(3000);
console.log('Express server started on port 3000');

io.set('transports', ['websocket']);

io.sockets.on('connection', function (socket) {
	console.log('client connected for update notifications');
	
	socket.on('startedediting', function(data) {
		console.log('startedediting' + data);
		socket.broadcast.emit('startedediting', data);
	});

	socket.on('modelchanged', function(data) {
		console.log('modelchanged' + data);
		socket.broadcast.emit('modelchanged', data);
	});

	socket.on('livemetadata', function(data) {
		console.log('livemetadata' + data);
		socket.broadcast.emit('livemetadata', data);
	});

	socket.on('contentassistrequest', function(data) {
		console.log('contentassistrequest' + data);
		socket.broadcast.emit('contentassistrequest', data);
	});

	socket.on('contentassistresponse', function(data) {
		console.log('contentassistresponse' + data);
		socket.broadcast.emit('contentassistresponse', data);
	});

	socket.on('disconnect', function () {
		console.log('client disconnected from update notifications');
	});
});
